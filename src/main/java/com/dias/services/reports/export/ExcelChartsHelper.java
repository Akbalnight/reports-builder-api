package com.dias.services.reports.export;

import com.dias.services.reports.dto.reports.ReportDTO;
import com.dias.services.reports.report.chart.ChartDescriptor;
import com.dias.services.reports.report.query.ResultSetWithTotal;
import lombok.experimental.Delegate;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph;

import java.util.List;
import java.util.Map;

/**
 * Хелпер для построения диаграмм в Excel.
 */
class ExcelChartsHelper {

    private static final int CHART_WIDTH = 15;
    private static final int CHART_HEIGHT = 20;
    private static final int LABEL_POSITION_OUTSIDE_TOP = 7;
    private static final int LABEL_POSITION_TOP = 9;
    private static final int AXIS_Y_ID = 2;
    private static final int AXIS_X_ID = 1;

    /**
     * Интерфейс диаграммы с сериями. Служит для представления абстракции диаграммы
     * для Apache POI, в которой отсутствует общий интерфейс для диаграмм не смотря на обилие
     * одинаковой логики. Введение данного интерфейса позволяет избавится от многочисленного
     * дублирования кода. Внутри реализации интерфейса происходит делегирование к Apache POI диаграмме
     */
    interface IChartWithSeries {
        CTBoolean addNewVaryColors();
        ISeries addNewSeries();
        CTUnsignedInt addNewAxId();
        CTShapeProperties addNewShapeProperties(int seriesIndex);
        IAxisX addAxisX(CTPlotArea plotArea, boolean isCategoryAxisNumeric);
    }

    /**
     * Интерфейс серии. Как и <code>IChartWithSeries</code> служит абстракцией для серий из
     * Apache POI. Внутри реализации серии происходит делегирование к Apache POI серии
     */
    interface ISeries {

        CTUnsignedInt addNewIdx();
        CTNumDataSource addNewVal();
        void setFforX(String formula);
        CTSerTx addNewTx();
        CTDLbls addNewDLbls();
    }

    /**
     * Серия линейной диаграммы
     */
    static class LineSer implements ISeries {
        @Delegate
        private final CTLineSer series;
        LineSer(CTLineSer ctLineSer) {
            this.series = ctLineSer;
        }
        @Override
        public void setFforX(String formula) {
            series.addNewCat().addNewStrRef().setF(formula);
        }
    }

    /**
     * Серия графика с числовой осью X
     */
    static class ScatterSer implements ISeries {
        @Delegate
        private final CTScatterSer series;
        ScatterSer(CTScatterSer ctScatterSer) {
            this.series = ctScatterSer;
        }

        @Override
        public CTNumDataSource addNewVal() {
            return series.addNewYVal();
        }

        @Override
        public void setFforX(String formula) {
            series.addNewXVal().addNewNumRef().setF(formula);
        }
    }

    interface IAxisX {
        CTUnsignedInt addNewAxId();
        CTBoolean addNewDelete();
        CTAxPos addNewAxPos();
        CTUnsignedInt addNewCrossAx();
        CTTickLblPos addNewTickLblPos();
        CTScaling addNewScaling();
        CTDouble addNewCrossesAt();
        boolean supportsMinMax();
        CTTitle addTitle();
    }

    static class CategoryAxis implements IAxisX {
        @Delegate
        private final CTCatAx ctCatAx;
        CategoryAxis(CTCatAx ctCatAx) {
            this.ctCatAx = ctCatAx;
        }
        @Override
        public boolean supportsMinMax() {
            return false;
        }
        @Override
        public CTTitle addTitle() {
            return ctCatAx.addNewTitle();
        }
    }

    static class NumericAxis implements IAxisX {
        @Delegate
        private final CTValAx ctValAx;
        NumericAxis(CTValAx ctValAx) {
            this.ctValAx = ctValAx;
        }
        @Override
        public boolean supportsMinMax() {
            return true;
        }
        @Override
        public CTTitle addTitle() {
            return ctValAx.addNewTitle();
        }
    }

    /**
     * Серия для гистограммы
     */
    static class BarSer implements ISeries {
        @Delegate
        private final CTBarSer series;
        BarSer(CTBarSer ctBarSer) {
            this.series = ctBarSer;
        }

        @Override
        public void setFforX(String formula) {
            series.addNewCat().addNewStrRef().setF(formula);
        }
    }

    /**
     * График с категориями по оси X
     */
    static class LineChart implements IChartWithSeries {
        @Delegate
        private final CTLineChart lineChart;
        private final CTPlotArea plot;
        LineChart(CTLineChart ctLineChart, CTPlotArea plot) {
            this.lineChart = ctLineChart;
            this.plot = plot;
        }

        @Override
        public ISeries addNewSeries() {
            return new LineSer(lineChart.addNewSer());
        }

        @Override
        public CTShapeProperties addNewShapeProperties(int seriesIndex) {
            return plot.getLineChartList().get(0).getSerArray(seriesIndex).addNewSpPr();
        }

        @Override
        public IAxisX addAxisX(CTPlotArea plotArea, boolean isCategoryAxisNumeric) {
            return new CategoryAxis(plotArea.addNewCatAx());
        }
    }

    /**
     * График c числами по оси X
     */
    static class ScatterChart implements IChartWithSeries {
        @Delegate
        private final CTScatterChart ctScatterChart;
        private final CTPlotArea plot;
        ScatterChart(CTScatterChart ctScatterChart, CTPlotArea plot) {
            this.ctScatterChart = ctScatterChart;
            this.plot = plot;
        }

        @Override
        public ISeries addNewSeries() {
            return new ScatterSer(ctScatterChart.addNewSer());
        }

        @Override
        public CTShapeProperties addNewShapeProperties(int seriesIndex) {
            return plot.getScatterChartList().get(0).getSerArray(seriesIndex).addNewSpPr();
        }

        @Override
        public IAxisX addAxisX(CTPlotArea plotArea, boolean isCategoryAxisNumeric) {
            return new NumericAxis(plotArea.addNewValAx());
        }
    }

    /**
     * Гистограмма
     */
    static class BarChart implements IChartWithSeries {
        @Delegate
        private final CTBarChart barChart;
        private final CTPlotArea plot;
        BarChart(CTBarChart ctBarChart, CTPlotArea plot) {
            this.barChart = ctBarChart;
            this.plot = plot;
        }

        @Override
        public ISeries addNewSeries() {
            return new BarSer(barChart.addNewSer());
        }

        @Override
        public CTShapeProperties addNewShapeProperties(int seriesIndex) {
            return plot.getBarChartArray(0).getSerArray(seriesIndex).addNewSpPr();
        }

        @Override
        public IAxisX addAxisX(CTPlotArea plotArea, boolean isCategoryAxisNumeric) {
            /*if (isCategoryAxisNumeric) {
                return new NumericAxis(plotArea.addNewValAx());
            }*/
            return new CategoryAxis(plotArea.addNewCatAx());
        }
    }

    /**
     * Добавление диаграммы в рабочую книгу excel
     *  @param workbook рабочая книга excel
     * @param chartDescriptor описание диаграммы
     * @param report отчет
     * @param repType тип отчета
     * @param firstRowWithData номер первой строки, содержащей данные
     * @param rs набор данных
     * @param sheet страница для добавления диагрммы
     * @param excelColumnsMap соответствие имен колонок индексам на странице excel
     */
    static void addChartToWorkbook(XSSFWorkbook workbook, ChartDescriptor chartDescriptor, ReportDTO report, ReportType repType, int firstRowWithData, ResultSetWithTotal rs, XSSFSheet sheet, Map<String, Integer> excelColumnsMap) {
        XSSFChart xssfChart = xssfChart(workbook, chartDescriptor, report);
        IChartWithSeries chart;
        Integer dataLabelPos = LABEL_POSITION_OUTSIDE_TOP;
        if (ReportType.hbar == repType) {
            chart = addBarChart(xssfChart.getCTChart().getPlotArea(), STBarDir.BAR);
        } else if (ReportType.bar == repType) {
            chart = addBarChart(xssfChart.getCTChart().getPlotArea(), STBarDir.COL);
        } else {
            Integer categoryRsColumnIndex = rs.getColumnsMap().get(chartDescriptor.getAxisXColumn());
            if (rs.getNumericColumnsIndexes().contains(categoryRsColumnIndex) || rs.getDateColumnsIndexes().contains(categoryRsColumnIndex)) {
                chart = addScatterChart(xssfChart.getCTChart().getPlotArea());
            } else {
                chart = addLineChart(xssfChart.getCTChart().getPlotArea());
            }
            dataLabelPos = LABEL_POSITION_TOP;
        }
        fillChart(sheet, excelColumnsMap, chart, xssfChart, chartDescriptor, firstRowWithData, rs, dataLabelPos);
    }

    private static XSSFChart xssfChart(XSSFWorkbook workbook,
                                       ChartDescriptor chartDescriptor,
                                       ReportDTO report) {
        String title = chartDescriptor.getTitle();
        XSSFSheet dataSheet = workbook.createSheet(title != null ? title : report.getTitle());
        XSSFDrawing drawing = dataSheet.createDrawingPatriarch();
        ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 0, CHART_WIDTH, CHART_HEIGHT);
        return drawing.createChart(anchor);
    }

    private static LineChart addLineChart(CTPlotArea plot) {
        return new LineChart(plot.addNewLineChart(), plot);
    }

    private static IChartWithSeries addScatterChart(CTPlotArea plot) {
        CTScatterChart ctScatterChart = plot.addNewScatterChart();
        ctScatterChart.addNewScatterStyle().setVal(STScatterStyle.Enum.forString("smoothMarker"));
        return new ScatterChart(ctScatterChart, plot);
    }

    private static BarChart addBarChart(CTPlotArea plot, STBarDir.Enum barChartType) {
        CTBarChart ctBarChart = plot.addNewBarChart();
        ctBarChart.addNewBarDir().setVal(barChartType);
        return new BarChart(ctBarChart, plot);

    }


    private static void fillChart(
            XSSFSheet sheet,
            Map<String, Integer> excelColumnsMap,
            IChartWithSeries chart,
            XSSFChart xssfChart,
            ChartDescriptor chartDescriptor,
            Integer firstDataRow,
            ResultSetWithTotal rs,
            Integer dataLabelPos) {

        Integer rowsNumber = rs.getRows().size();
        xssfChart.setTitleText(chartDescriptor.getTitle());
        chart.addNewVaryColors().setVal(false);
        int xColumn = excelColumnsMap.get(chartDescriptor.getAxisXColumn());
        String xColumnName = CellReference.convertNumToColString(xColumn);
        List<ChartDescriptor.Series> series = chartDescriptor.getSeries();

        double[] xMinMax = new double[]{Double.MIN_NORMAL, Double.MIN_NORMAL};
        double[] yMinMax = new double[]{Double.MIN_NORMAL, Double.MIN_NORMAL};
        int[] xFromTo = new int[]{-1,0};
        Map<String, Integer> rsColumnsMap = rs.getColumnsMap();

        for (int i = 0; i < series.size(); i++) {
            ChartDescriptor.Series s = series.get(i);
            ISeries ctBarSer = chart.addNewSeries();
            ctBarSer.addNewIdx().setVal(i);
            int fromRowIndex = (s.getStartRow() != null && s.getStartRow() > 0) ? s.getStartRow() - 1 : 0;
            int toRowIndex = (s.getEndRow() != null && s.getEndRow() < rowsNumber) ? s.getEndRow(): rowsNumber;
            int from = firstDataRow + fromRowIndex + 1;
            int to = firstDataRow + toRowIndex;
            ctBarSer.setFforX(sheet.getSheetName() + "!$" + xColumnName + "$" + from + ":$" + xColumnName + "$" + to);
            CTNumDataSource ctNumDataSource = ctBarSer.addNewVal();
            CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();
            int valueColumnIndex = excelColumnsMap.get(s.getValueColumn());
            if (chartDescriptor.isCalculatedXRange()) {
                if (xFromTo[0] > fromRowIndex || xFromTo[0] == -1){
                    xFromTo[0] = fromRowIndex;
                }
                if (xFromTo[1] < toRowIndex) {
                    xFromTo[1] = toRowIndex;
                }
            }
            if (chartDescriptor.isCalculatedYRange()) {
                defineMinMax(yMinMax, rsColumnsMap.get(s.getValueColumn()), rs, fromRowIndex, toRowIndex);
            }

            String valueColumnName = CellReference.convertNumToColString(valueColumnIndex);
            if (chartDescriptor.getShowLegend()) {
                //если необходимо показывать легенду, указываем ячейку с наименованием колонки
                ctBarSer.addNewTx().addNewStrRef().setF(sheet.getSheetName() + "!$" + valueColumnName + "$" + firstDataRow);
            }

            if (chartDescriptor.isShowDotValues()) {
                //добавляем метки к столбцам
                CTDLbls dLbls = ctBarSer.addNewDLbls();
                //укажем положение - OUT_END (соответствует 7)
                CTDLblPos ctdLblPos = dLbls.addNewDLblPos();
                ctdLblPos.setVal(org.openxmlformats.schemas.drawingml.x2006.chart.STDLblPos.Enum.forInt(dataLabelPos));
                dLbls.addNewShowVal().setVal(true);
                //отключим отображение всего лишнего
                dLbls.addNewShowSerName().setVal(false);
                dLbls.addNewShowCatName().setVal(false);
                dLbls.addNewShowBubbleSize().setVal(false);
                dLbls.addNewShowLeaderLines().setVal(false);
                dLbls.addNewShowLegendKey().setVal(false);
            }

            ctNumRef.setF(sheet.getSheetName() + "!$" + valueColumnName + "$" + from + ":$" + valueColumnName + "$" + to);
        }

        chart.addNewAxId().setVal(AXIS_X_ID);
        chart.addNewAxId().setVal(AXIS_Y_ID);

        CTPlotArea plotArea = xssfChart.getCTChart().getPlotArea();

        Integer categoryRsColumnIndex = rsColumnsMap.get(chartDescriptor.getAxisXColumn());
        boolean isCategoryAxisNumeric = rs.getNumericColumnsIndexes().contains(categoryRsColumnIndex);
        boolean isCategoryAxisDate = rs.getDateColumnsIndexes().contains(categoryRsColumnIndex);

        //val axis
        CTValAx ctValAx = plotArea.addNewValAx();
        ctValAx.addNewAxId().setVal(AXIS_Y_ID); //id of the val axis
        ctValAx.addNewDelete().setVal(false);
        ctValAx.addNewAxPos().setVal(STAxPos.L);
        ctValAx.addNewCrossAx().setVal(AXIS_X_ID); //id of the cat axis
        ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);
        if (!StringUtils.isEmpty(chartDescriptor.getAxisYTitle())) {
            setAxisTitle(ctValAx.addNewTitle(), chartDescriptor.getAxisYTitle());
        }

        //cat axis
        IAxisX ctCatAx = chart.addAxisX(plotArea, isCategoryAxisNumeric || isCategoryAxisDate);
        ctCatAx.addNewAxId().setVal(AXIS_X_ID); //id of the cat axis
        ctCatAx.addNewDelete().setVal(false);
        ctCatAx.addNewAxPos().setVal(STAxPos.B);
        ctCatAx.addNewCrossAx().setVal(AXIS_Y_ID); //id of the val axis
        ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);
        if (!StringUtils.isEmpty(chartDescriptor.getAxisXTitle())) {
            setAxisTitle(ctCatAx.addTitle(), chartDescriptor.getAxisXTitle());
        }

        CTScaling xctScaling = ctCatAx.addNewScaling();
        xctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);


        if (ctCatAx.supportsMinMax()) {
            // гистограмма и колонки не поддерживают минимум/максимум
            if (chartDescriptor.isCalculatedXRange() && isCategoryAxisNumeric) {
                defineMinMax(xMinMax, categoryRsColumnIndex, rs, xFromTo[0], xFromTo[1]);
                fixRange(xMinMax);

                // блок определения минимума/максимума оси
                xctScaling.addNewMin().setVal(xMinMax[0]);
                xctScaling.addNewMax().setVal(xMinMax[1]);
                // укажем, что ось значений должна пересечь ось категорий в минимуме
                ctValAx.addNewCrossesAt().setVal(xMinMax[0]);
            } else if (isCategoryAxisNumeric) {
                xctScaling.addNewMin().setVal(0);
                ctValAx.addNewCrossesAt().setVal(0);
            }
        }

        CTScaling yctScaling = ctValAx.addNewScaling();
        yctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        if (chartDescriptor.isCalculatedYRange()) {

            fixRange(yMinMax);

            // блок определения минимума/максимума оси
            yctScaling.addNewMin().setVal(yMinMax[0]);
            yctScaling.addNewMax().setVal(yMinMax[1]);
            // укажем, что ось значений должна пересечь ось значений в минимуме
            ctCatAx.addNewCrossesAt().setVal(yMinMax[0]);
        }


        //legend
        if (chartDescriptor.getShowLegend()) {
            CTLegend ctLegend = xssfChart.getCTChart().addNewLegend();
            ctLegend.addNewLegendPos().setVal(STLegendPos.R);
            ctLegend.addNewOverlay().setVal(false);
        }

        // line style of the series
        for (int i = 0; i < series.size(); i++) {
            java.awt.Color color = series.get(i).getAwtColor();
            if (color != null) {
                CTShapeProperties seriesShapeProperties = chart.addNewShapeProperties(i);
                seriesShapeProperties.addNewSolidFill().addNewSrgbClr().setVal(new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()});
            }
        }
    }

    private static void defineMinMax(double[] minmax, int valueColumnIndex, ResultSetWithTotal rs, int from, int to) {
        List<List<Object>> rows = rs.getRows();
        for (int i = from; i < to; i++) {
            List<Object> row = rows.get(i);
            Object value = row.get(valueColumnIndex);
            if (value != null) {
                try {
                    Double doubleValue = Double.valueOf(value.toString());
                    if (minmax[0] > doubleValue || minmax[0] == Double.MIN_NORMAL) {
                        minmax[0] = doubleValue;
                    }
                    if (minmax[1] < doubleValue || minmax[1] == Double.MIN_NORMAL) {
                        minmax[1] = doubleValue;
                    }
                } catch (Exception ignore) {
                }
            }
        }
    }

    private static void fixRange(double[] minmax) {
        // подгон минимума и максимума значений - алогоритм аналогичен конструктору отчета (UI)
        if (minmax[0] > Double.MIN_NORMAL) {
            double margin = Math.abs((minmax[1] - minmax[0]) / 15);
            double lower = Math.floor((minmax[0] - margin) * 100) / 100;
            double upper = Math.floor((minmax[1] + margin) * 100) / 100;
            minmax[0] = lower;
            minmax[1] = upper;
        }
    }

    private static void setAxisTitle(CTTitle ctTitle, String title) {
        ctTitle.addNewLayout();
        ctTitle.addNewOverlay().setVal(false);
        CTTextBody rich = ctTitle.addNewTx().addNewRich();
        rich.addNewBodyPr();
        rich.addNewLstStyle();
        CTTextParagraph p = rich.addNewP();
        p.addNewPPr().addNewDefRPr();
        p.addNewR().setT(title);
        p.addNewEndParaRPr();
    }

}
