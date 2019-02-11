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
 * Хелпер для построения диаграмм в Excel. В виду сложной и запутанной логики код перенесен в отдельный класс
 */
class ExcelChartsHelper {

    private static final int CHART_WIDTH = 15;
    private static final int CHART_HEIGHT = 20;

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

    private static BarChart addBarChart(CTPlotArea plot, STBarDir.Enum barChartType) {
        CTBarChart ctBarChart = plot.addNewBarChart();
        ctBarChart.addNewBarDir().setVal(barChartType);
        return new BarChart(ctBarChart, plot);

    }

    interface IChart {
        CTBoolean addNewVaryColors();
        ISeries addNewSeries();
        CTUnsignedInt addNewAxId();
        CTShapeProperties addNewShapeProperties(int seriesIndex);
    }

    interface ISeries {

        CTUnsignedInt addNewIdx();
        CTAxDataSource addNewCat();
        CTNumDataSource addNewVal();
        CTSerTx addNewTx();
        CTDLbls addNewDLbls();
    }

    static class LineSer implements ISeries {
        @Delegate
        private final CTLineSer series;
        LineSer(CTLineSer ctLineSer) {
            this.series = ctLineSer;
        }
    }

    static class BarSer implements ISeries {
        @Delegate
        private final CTBarSer series;
        BarSer(CTBarSer ctBarSer) {
            this.series = ctBarSer;
        }
    }

    static class LineChart implements IChart {
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
    }

    private static class BarChart implements IChart {
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
    }

    private static void fillChart(
            XSSFSheet sheet,
            Map<String, Integer> columnMap,
            IChart chart,
            XSSFChart xssfChart,
            ChartDescriptor chartDescriptor,
            Integer firstDataRow,
            Integer rowsNumber,
            Integer dataLabelPos) {

        xssfChart.setTitleText(chartDescriptor.getTitle());
        chart.addNewVaryColors().setVal(false);
        int xColumn = columnMap.get(chartDescriptor.getAxisXColumn());
        String xColumnName = CellReference.convertNumToColString(xColumn);
        List<ChartDescriptor.Series> series = chartDescriptor.getSeries();
        for (int i = 0; i < series.size(); i++) {
            ChartDescriptor.Series s = series.get(i);
            ISeries ctBarSer = chart.addNewSeries();
            ctBarSer.addNewIdx().setVal(i);
            CTAxDataSource cttAxDataSource = ctBarSer.addNewCat();
            CTStrRef ctStrRef = cttAxDataSource.addNewStrRef();
            int from = s.getStartRow() != null && s.getStartRow() > 0 ? firstDataRow + s.getStartRow() : firstDataRow + 1;
            int to = s.getEndRow() != null && s.getEndRow() > 0 ? firstDataRow + s.getEndRow() : firstDataRow + rowsNumber;
            ctStrRef.setF(sheet.getSheetName() + "!$" + xColumnName + "$" + from + ":$" + xColumnName + "$" + to);
            CTNumDataSource ctNumDataSource = ctBarSer.addNewVal();
            CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();
            int valueColumnIndex = columnMap.get(s.getValueColumn());

            String valueColumnName = CellReference.convertNumToColString(valueColumnIndex);
            if (chartDescriptor.getShowLegend()) {
                //если необходимо показывать легенду, указываем ячейку с наименованием колонки
                ctBarSer.addNewTx().addNewStrRef().setF(sheet.getSheetName() + "!$" + valueColumnName + "$" + firstDataRow);
            }

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

            ctNumRef.setF(sheet.getSheetName() + "!$" + valueColumnName + "$" + from + ":$" + valueColumnName + "$" + to);
        }

        //telling the BarChart that it has axes and giving them Ids
        chart.addNewAxId().setVal(1);
        chart.addNewAxId().setVal(2);

        CTPlotArea plotArea = xssfChart.getCTChart().getPlotArea();
        //cat axis
        CTCatAx ctCatAx = plotArea.addNewCatAx();
        ctCatAx.addNewAxId().setVal(1); //id of the cat axis
        CTScaling ctScaling = ctCatAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctCatAx.addNewDelete().setVal(false);
        ctCatAx.addNewAxPos().setVal(STAxPos.B);
        ctCatAx.addNewCrossAx().setVal(2); //id of the val axis
        ctCatAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);


        //val axis
        CTValAx ctValAx = plotArea.addNewValAx();
        ctValAx.addNewAxId().setVal(2); //id of the val axis
        ctScaling = ctValAx.addNewScaling();
        ctScaling.addNewOrientation().setVal(STOrientation.MIN_MAX);
        ctValAx.addNewDelete().setVal(false);
        ctValAx.addNewAxPos().setVal(STAxPos.L);
        ctValAx.addNewCrossAx().setVal(1); //id of the cat axis
        ctValAx.addNewTickLblPos().setVal(STTickLblPos.NEXT_TO);

        //legend
        if (chartDescriptor.getShowLegend()) {
            CTLegend ctLegend = xssfChart.getCTChart().addNewLegend();
            ctLegend.addNewLegendPos().setVal(STLegendPos.R);
            ctLegend.addNewOverlay().setVal(false);
        }

        nameAxis(chartDescriptor, xssfChart);

        // line style of the series
        for (int i = 0; i < series.size(); i++) {
            java.awt.Color color = series.get(i).getAwtColor();
            if (color != null) {
                CTShapeProperties seriesShapeProperties = chart.addNewShapeProperties(i);
                seriesShapeProperties.addNewSolidFill().addNewSrgbClr().setVal(new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()});
            }
        }
    }



    public static void buildChart(XSSFWorkbook workbook, ChartDescriptor chartDescriptor, ReportDTO report, ReportType repType, int firstRowWithData, ResultSetWithTotal rs, XSSFSheet sheet, Map<String, Integer> columnMap) {
        XSSFChart xssfChart = xssfChart(workbook, chartDescriptor, report);
        IChart chart;
        Integer dataLabelPos = 7;
        if (ReportType.hbar == repType) {
            chart = addBarChart(xssfChart.getCTChart().getPlotArea(), STBarDir.BAR);
        } else if (ReportType.bar == repType) {
            chart = addBarChart(xssfChart.getCTChart().getPlotArea(), STBarDir.COL);
        } else {
            chart = addLineChart(xssfChart.getCTChart().getPlotArea());
            dataLabelPos = 9;
        }
        fillChart(sheet, columnMap, chart, xssfChart, chartDescriptor, firstRowWithData, rs.getRows().size(), dataLabelPos);
    }

    private static void nameAxis(ChartDescriptor chartDescriptor, XSSFChart chart) {
        if (!StringUtils.isEmpty(chartDescriptor.getAxisYTitle())) {
            setValuesAxisTitle(chart, chartDescriptor.getAxisYTitle());
        }

        if (!StringUtils.isEmpty(chartDescriptor.getAxisXTitle())) {
            setCatAxisTitle(chart, chartDescriptor.getAxisXTitle());
        }
    }

    private static void setValuesAxisTitle(XSSFChart chart, String title) {

        CTPlotArea plotArea = chart.getCTChart().getPlotArea();
        CTValAx valAx = plotArea.getValAxArray(0);
        CTTitle ctTitle = valAx.addNewTitle();
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

    private static void setCatAxisTitle(XSSFChart chart, String title) {
        CTCatAx valAx = chart.getCTChart().getPlotArea().getCatAxArray(0);
        CTTitle ctTitle = valAx.addNewTitle();
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
