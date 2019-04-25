package com.dias.services.reports.export.excel.charts;

import com.dias.services.reports.export.excel.ReportExcelWriter;
import com.dias.services.reports.report.chart.ChartDescriptor;
import com.dias.services.reports.report.query.Column;
import com.dias.services.reports.report.query.ResultSetWithTotal;
import com.dias.services.reports.service.ReportBuilderService;
import com.dias.services.reports.subsystem.ColumnWithType;
import lombok.experimental.Delegate;
import org.apache.poi.ss.util.CellReference;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * График c числами по оси X
 */
public class CascadeChart extends BaseChart {

    public static final String START_VALUE_TITLE = "Начальное значение";
    public static final String END_VALUE_TITLE = "Итог";
    private final CTPlotArea plot;

    @Delegate
    private final CTBarChart totalBarChart;
    private final ReportExcelWriter reportExcelWriter;
    private final ResultSetWithTotal rs;
    private final ChartDescriptor.Series diagramSeries;
    private final int fromRowIndex;
    private final int toRowIndex;
    private final int from;
    private final int to;
    private final String xColumnName;

    private final static int START_VALUE_INDEX = 0;
    private final static int CURRENT_VALUE_INDEX = 1;
    private final static int PREVIOUS_VALUE_INDEX = 2;
    private final static int DELTA_VALUE_INDEX = 3;
    private final static int END_VALUE_INDEX = 4;

    public CascadeChart(Map<String, Integer> excelColumnsMap, int firstDataRow, ReportExcelWriter reportExcelWriter, ResultSetWithTotal rs, CTChart ctChart, ChartDescriptor chartDescriptor) {
        super(rs, ctChart, chartDescriptor);
        this.plot = ctChart.getPlotArea();

        CTBarChart barChart = plot.addNewBarChart();
        barChart.addNewBarDir().setVal(STBarDir.COL);
        barChart.addNewGrouping().setVal(STBarGrouping.CLUSTERED);
        barChart.addNewVaryColors().setVal(false);
        barChart.addNewOverlap().setVal((byte) 100);
        barChart.addNewGapWidth().setVal(0);

        this.totalBarChart = barChart;
        this.reportExcelWriter = reportExcelWriter;
        this.rs = rs;

        diagramSeries = chartDescriptor.getSeries().get(0);
        fromRowIndex = (diagramSeries.getStartRow() != null && diagramSeries.getStartRow() > 0) ? diagramSeries.getStartRow() - 1 : 0;
        int rowsNumber = rs.getRows().size();
        toRowIndex = (diagramSeries.getEndRow() != null && diagramSeries.getEndRow() < rowsNumber) ? diagramSeries.getEndRow(): rowsNumber;
        from = firstDataRow + fromRowIndex + 1;
        to = firstDataRow + toRowIndex;
        int xColumn = excelColumnsMap.get(new Column(chartDescriptor.getAxisXColumn()).getColumnName());
        xColumnName = CellReference.convertNumToColString(xColumn);

    }

    @Override
    public void addSeries(int firstDataRow, Map<String, Integer> excelColumnsMap, String dataSheetName) {

        updateDataSheet();
        Integer lastColumn = excelColumnsMap.values().stream().max(Integer::compareTo).get();

        addTotalBar(dataSheetName, 0, lastColumn + START_VALUE_INDEX + 1, 1, diagramSeries.toAwtColor(diagramSeries.getColorInitial()), from, to, START_VALUE_TITLE); // начальное значение
        addTotalBar(dataSheetName, 1, lastColumn + END_VALUE_INDEX + 1, 2, diagramSeries.toAwtColor(diagramSeries.getColorTotal()), from, to + 1, END_VALUE_TITLE); // конечное значение

        CTLineChart lineChart = plot.addNewLineChart();

        addLineSeries(lineChart, dataSheetName, 2, lastColumn + CURRENT_VALUE_INDEX + 1, 0, ""); // текущие значения
        addLineSeries(lineChart, dataSheetName, 3, lastColumn + PREVIOUS_VALUE_INDEX + 1, 3, ""); // предыдущие значения

        CTUpDownBars updowns = lineChart.addNewUpDownBars();
        updowns.addNewGapWidth().setVal(0);
        CTUpDownBar upBars = updowns.addNewUpBars();


        if (diagramSeries.getColorPositive() != null) {
            CTShapeProperties sp = upBars.addNewSpPr();
            Color color = diagramSeries.toAwtColor(diagramSeries.getColorPositive());
            sp.addNewSolidFill().addNewSrgbClr().setVal(new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()});
            sp.addNewLn().addNewNoFill();
        }

        CTUpDownBar downBars = updowns.addNewDownBars();
        if (diagramSeries.getColorNegative() != null) {
            CTShapeProperties sp = downBars.addNewSpPr();
            Color color = diagramSeries.toAwtColor(diagramSeries.getColorNegative());
            sp.addNewSolidFill().addNewSrgbClr().setVal(new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()});
            sp.addNewLn().addNewNoFill();
        }
    }

    private void addLineSeries(CTLineChart lineChart, String dataSheetName, int seriesIndex, Integer valueColumnIndex, long order, String title) {

        lineChart.addNewAxId().setVal(AXIS_X_ID);
        lineChart.addNewAxId().setVal(AXIS_Y_ID);

        CTLineSer lineSeries = lineChart.addNewSer();

        // спрячем маркеры и линии, поскольку нам нужны только up/down бары
        lineSeries.addNewMarker().addNewSymbol().setVal(STMarkerStyle.NONE);
        CTShapeProperties sp = lineSeries.addNewSpPr();
        sp.addNewLn().addNewNoFill();

        lineSeries.addNewOrder().setVal(order);
        lineSeries.addNewIdx().setVal(seriesIndex);


        lineSeries.addNewCat().addNewStrRef().setF(dataSheetName + "!$" + xColumnName + "$" + from + ":$" + xColumnName + "$" + to);
        CTNumDataSource ctNumDataSource = lineSeries.addNewVal();
        CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();
        String valueColumnName = CellReference.convertNumToColString(valueColumnIndex);
        ctNumRef.setF(dataSheetName + "!$" + valueColumnName + "$" + from + ":$" + valueColumnName + "$" + to);

        lineSeries.addNewTx().setV(title);

        if (chartDescriptor.isShowDotValues()) {
            /*CTDLbls dLbls = lineSeries.addNewDLbls();
            //CTNumFmt ctNumFmt = dLbls.addNewNumFmt();
            //ctNumFmt.setFormatCode("#.##\"\"");
            //ctNumFmt.setSourceLinked(false);
            dLbls.addNewDLblPos().setVal(STDLblPos.T);
            dLbls.addNewShowVal().setVal(true);
            //отключим отображение всего лишнего
            dLbls.addNewShowSerName().setVal(false);
            dLbls.addNewShowCatName().setVal(false);
            dLbls.addNewShowBubbleSize().setVal(false);
            dLbls.addNewShowLeaderLines().setVal(false);
            dLbls.addNewShowLegendKey().setVal(false);
            */
            // TODO необходимо использовать расширения
            // что-то можно взять здесь: https://stackoverflow.com/questions/40382369/embed-files-into-xssf-sheets-in-excel-using-apache-poi
            /*
                     <c:extLst>
                        <c:ext uri="{02D57815-91ED-43cb-92C2-25804820EDAC}" xmlns:c15="http://schemas.microsoft.com/office/drawing/2012/chart">
                            <c15:datalabelsRange>
                                <c15:f>Данные!$K$10:$K$16</c15:f>
                            </c15:datalabelsRange>
                        </c:ext>
                        <c:ext uri="{C3380CC4-5D6E-409C-BE32-E72D297353CC}" xmlns:c16="http://schemas.microsoft.com/office/drawing/2014/chart">
                            <c16:uniqueId val="{00000003-3920-49F9-91D2-BDCC3873B7F5}" />
                        </c:ext>
                    </c:extLst>
            */
            /*CTDLbls dLbls = lineSeries.addNewDLbls();
            CTExtension ext = dLbls.addNewExtLst().addNewExt();
            ext.setUri("{02D57815-91ED-43cb-92C2-25804820EDAC}");
            XmlCursor cur = ext.newCursor();
            cur.toEndToken();
            cur.beginElement(new QName("http://schemas.microsoft.com/office/drawing/2012/chart", "showDataLabelsRange", "c15"));
            cur.insertAttributeWithValue("val", "1");

            ext = lineSeries.addNewExtLst().addNewExt();
            ext.setUri("{02D57815-91ED-43cb-92C2-25804820EDAC}");
            cur = ext.newCursor();
            cur.toEndToken();
            cur.beginElement(new QName("http://schemas.microsoft.com/office/drawing/2012/chart", "showDataLabelsRange", "c15"));
            */


        }

    }

    private void addTotalBar(String dataSheetName, int seriesIndex, int columnIndex, long order, Color color, int from, int to, String title) {
        //ISeries chartSeries = addNewSeries(diagramSeries);
        CTBarSer chartSeries = totalBarChart.addNewSer();
        chartSeries.addNewOrder().setVal(order);
        chartSeries.addNewIdx().setVal(seriesIndex);

        if (color != null) {
            CTShapeProperties sp = chartSeries.addNewSpPr();
            sp.addNewSolidFill().addNewSrgbClr().setVal(new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()});
        }

        chartSeries.addNewCat().addNewStrRef().setF(dataSheetName + "!$" + xColumnName + "$" + from + ":$" + xColumnName + "$" + to);
        CTNumDataSource ctNumDataSource = chartSeries.addNewVal();
        CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();
        String valueColumnName = CellReference.convertNumToColString(columnIndex);
        ctNumRef.setF(dataSheetName + "!$" + valueColumnName + "$" + from + ":$" + valueColumnName + "$" + to);

        //если необходимо показывать легенду, указываем ячейку с наименованием колонки
        chartSeries.addNewTx().setV(title);

        if (chartDescriptor.isShowDotValues()) {
            /*CTDLbls dLbls = chartSeries.addNewDLbls();
            CTNumFmt ctNumFmt = dLbls.addNewNumFmt();
            ctNumFmt.setFormatCode("#.##\"\"");
            ctNumFmt.setSourceLinked(false);
            dLbls.addNewDLblPos().setVal(STDLblPos.OUT_END);
            dLbls.addNewShowVal().setVal(true);
            //отключим отображение всего лишнего
            dLbls.addNewShowSerName().setVal(false);
            dLbls.addNewShowCatName().setVal(false);
            dLbls.addNewShowBubbleSize().setVal(false);
            dLbls.addNewShowLeaderLines().setVal(false);
            dLbls.addNewShowLegendKey().setVal(false);*/
        }

    }

    private void updateDataSheet() {
        List<ChartDescriptor.Series> series = chartDescriptor.getSeries();
        Map<String, Integer> rsColumnsMap = rs.getColumnsMap();
        ChartDescriptor.Series s = series.get(0);
        Integer valueColumnIndex = rsColumnsMap.get(s.getValueColumn());
        ResultSetWithTotal calculatedData = new ResultSetWithTotal();
        List<List<Object>> newRows = new ArrayList<>();
        List<ColumnWithType> newHeaders = new ArrayList<>();
        newHeaders.add(ColumnWithType.builder().title(START_VALUE_TITLE).type(ReportBuilderService.JAVA_TYPE_NUMERIC).build());
        newHeaders.add(ColumnWithType.builder().title("Текущее значение").type(ReportBuilderService.JAVA_TYPE_NUMERIC).build());
        newHeaders.add(ColumnWithType.builder().title("Предыдущее значение").type(ReportBuilderService.JAVA_TYPE_NUMERIC).build());
        newHeaders.add(ColumnWithType.builder().title("Разница").type(ReportBuilderService.JAVA_TYPE_NUMERIC).build());
        newHeaders.add(ColumnWithType.builder().title(END_VALUE_TITLE).type(ReportBuilderService.JAVA_TYPE_NUMERIC).build());
        calculatedData.setHeaders(newHeaders);
        calculatedData.setRows(newRows);
        Double previous = 0D;
        for (int i = fromRowIndex; i < toRowIndex; i++) {
            Double[] values = new Double[5];
            List<Object> row = rs.getRows().get(i);
            Double value = (Double) row.get(valueColumnIndex);
            value = value == null ? 0D : value;
            if (i == fromRowIndex) {
                values[START_VALUE_INDEX] = value;
                values[CURRENT_VALUE_INDEX] = null;
                values[PREVIOUS_VALUE_INDEX] = null;
                values[DELTA_VALUE_INDEX] = null;
                values[END_VALUE_INDEX] = null;
            } else {
                values[START_VALUE_INDEX] = null;
                values[CURRENT_VALUE_INDEX] = value;
                values[PREVIOUS_VALUE_INDEX] = previous;
                values[DELTA_VALUE_INDEX] = value - previous;
                values[END_VALUE_INDEX] = null;
            }
            previous = value;
            if (value > yMinMax[1]) {
                yMinMax[1] = value;
            }
            if (value < yMinMax[0]) {
                yMinMax[0] = value;
            }
            newRows.add(Arrays.asList(values));
        }

        // добавим итоговую строку
        Double[] values = new Double[5];
        values[START_VALUE_INDEX] = null;
        values[CURRENT_VALUE_INDEX] = null;
        values[PREVIOUS_VALUE_INDEX] = null;
        values[DELTA_VALUE_INDEX] = null;
        values[END_VALUE_INDEX] = previous;
        newRows.add(Arrays.asList(values));


        reportExcelWriter.joinTable(fromRowIndex, rs, calculatedData);
    }


    @Override
    public ISeries addNewSeries(ChartDescriptor.Series s) {
        return new BarSer(totalBarChart.addNewSer());
    }

    @Override
    public CTShapeProperties addNewShapeProperties(int seriesIndex) {
        return plot.getBarChartList().get(0).getSerArray(seriesIndex).addNewSpPr();
    }

    @Override
    public IAxisX addAxisX(CTPlotArea plotArea, boolean isCategoryAxisNumeric) {
        return new CategoryAxis(plotArea.addNewCatAx());
    }

    @Override
    public CTValAx addNewValAx() {
        return plot.addNewValAx();
    }

}


