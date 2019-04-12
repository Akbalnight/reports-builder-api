package com.dias.services.reports.export;

import com.dias.services.reports.dto.reports.ReportDTO;
import com.dias.services.reports.export.charts.*;
import com.dias.services.reports.report.chart.ChartDescriptor;
import com.dias.services.reports.report.query.Column;
import com.dias.services.reports.report.query.ResultSetWithTotal;
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
        } else if (ReportType.Wpie == repType) {
            chart = addPieChart(xssfChart.getCTChart().getPlotArea());
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
        CTScatterChart chart = plot.addNewScatterChart();
        chart.addNewScatterStyle().setVal(STScatterStyle.Enum.forString("smoothMarker"));
        chart.addNewVaryColors().setVal(false);
        return new ScatterChart(chart, plot);
    }

    private static BarChart addBarChart(CTPlotArea plot, STBarDir.Enum barChartType) {
        CTBarChart chart = plot.addNewBarChart();
        chart.addNewBarDir().setVal(barChartType);
        chart.addNewVaryColors().setVal(false);
        return new BarChart(chart, plot);

    }

    private static PieChart addPieChart(CTPlotArea plot) {
        CTPieChart chart = plot.addNewPieChart();
        chart.addNewVaryColors().setVal(true);
        return new PieChart(chart, plot);

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
        int xColumn = excelColumnsMap.get(new Column(chartDescriptor.getAxisXColumn()).getColumnName());
        String xColumnName = CellReference.convertNumToColString(xColumn);
        List<ChartDescriptor.Series> series = chartDescriptor.getSeries();

        double[] xMinMax = new double[]{Double.MIN_NORMAL, Double.MIN_NORMAL};
        double[] yMinMax = new double[]{Double.MIN_NORMAL, Double.MIN_NORMAL};
        int[] xFromTo = new int[]{-1,0};
        Map<String, Integer> rsColumnsMap = rs.getColumnsMap();

        for (int i = 0; i < series.size(); i++) {
            ChartDescriptor.Series s = series.get(i);
            ISeries chartSeries = chart.addNewSeries();
            chartSeries.addNewIdx().setVal(i);
            int fromRowIndex = (s.getStartRow() != null && s.getStartRow() > 0) ? s.getStartRow() - 1 : 0;
            int toRowIndex = (s.getEndRow() != null && s.getEndRow() < rowsNumber) ? s.getEndRow(): rowsNumber;
            int from = firstDataRow + fromRowIndex + 1;
            int to = firstDataRow + toRowIndex;
            chartSeries.doWithRange(from, to);
            chartSeries.setFforX(sheet.getSheetName() + "!$" + xColumnName + "$" + from + ":$" + xColumnName + "$" + to);
            CTNumDataSource ctNumDataSource = chartSeries.addNewVal();
            CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();
            int valueColumnIndex = excelColumnsMap.get(new Column(s.getValueColumn()).getColumnName());
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
                chartSeries.addNewTx().addNewStrRef().setF(sheet.getSheetName() + "!$" + valueColumnName + "$" + firstDataRow);
            }

            if (chartDescriptor.isShowDotValues()) {
                //добавляем метки к столбцам
                CTDLbls dLbls = chartSeries.addNewDLbls();
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


        CTUnsignedInt axId = chart.addNewAxId();
        if (axId != null) {
            axId.setVal(AXIS_X_ID);
        }
        axId = chart.addNewAxId();
        if (axId != null) {
            axId.setVal(AXIS_Y_ID);
        }

        CTPlotArea plotArea = xssfChart.getCTChart().getPlotArea();

        Integer categoryRsColumnIndex = rsColumnsMap.get(chartDescriptor.getAxisXColumn());
        boolean isCategoryAxisNumeric = rs.getNumericColumnsIndexes().contains(categoryRsColumnIndex);
        boolean isCategoryAxisDate = rs.getDateColumnsIndexes().contains(categoryRsColumnIndex);

        //val axis
        CTValAx ctValAx = chart.addNewValAx();
        if (ctValAx != null) {
            // не все диаграммы поддерживают ось Y
            ctValAx.addNewAxId().setVal(AXIS_Y_ID); //id of the val axis
            ctValAx.addNewDelete().setVal(false);
            ctValAx.addNewAxPos().setVal(STAxPos.L);
            ctValAx.addNewCrossAx().setVal(AXIS_X_ID); //id of the cat axis
            ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);
            if (!StringUtils.isEmpty(chartDescriptor.getAxisYTitle())) {
                setAxisTitle(ctValAx.addNewTitle(), chartDescriptor.getAxisYTitle());
            }
        }

        //cat axis
        IAxisX axisX = chart.addAxisX(plotArea, isCategoryAxisNumeric || isCategoryAxisDate);
        if (axisX != null) {
            // не все диаграммы поддерживают ось X
            axisX.addNewAxId().setVal(AXIS_X_ID); //id of the cat axis
            axisX.addNewDelete().setVal(false);
            axisX.addNewAxPos().setVal(STAxPos.B);
            axisX.addNewCrossAx().setVal(AXIS_Y_ID); //id of the val axis
            axisX.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);
            if (!StringUtils.isEmpty(chartDescriptor.getAxisXTitle())) {
                setAxisTitle(axisX.addTitle(), chartDescriptor.getAxisXTitle());
            }

            CTScaling xctScaling = axisX.addNewScaling();
            xctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);


            if (axisX.supportsMinMax()) {
                // гистограмма и колонки не поддерживают минимум/максимум
                if (chartDescriptor.isCalculatedXRange() && isCategoryAxisNumeric) {
                    defineMinMax(xMinMax, categoryRsColumnIndex, rs, xFromTo[0], xFromTo[1]);
                    fixRange(xMinMax);

                    // блок определения минимума/максимума оси
                    xctScaling.addNewMin().setVal(xMinMax[0]);
                    xctScaling.addNewMax().setVal(xMinMax[1]);

                    if (ctValAx != null) {
                        // укажем, что ось значений должна пересечь ось категорий в минимуме
                        ctValAx.addNewCrossesAt().setVal(xMinMax[0]);
                    }
                } else if (isCategoryAxisNumeric) {
                    xctScaling.addNewMin().setVal(0);
                    if (ctValAx != null) {
                        ctValAx.addNewCrossesAt().setVal(0);
                    }
                }
            }
        }


        if (ctValAx != null) {
            CTScaling yctScaling = ctValAx.addNewScaling();
            yctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
            if (chartDescriptor.isCalculatedYRange()) {

                fixRange(yMinMax);

                // блок определения минимума/максимума оси
                yctScaling.addNewMin().setVal(yMinMax[0]);
                yctScaling.addNewMax().setVal(yMinMax[1]);
                // укажем, что ось значений должна пересечь ось значений в минимуме
                axisX.addNewCrossesAt().setVal(yMinMax[0]);
            }
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
