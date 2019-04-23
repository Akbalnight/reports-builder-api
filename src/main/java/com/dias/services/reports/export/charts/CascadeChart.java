package com.dias.services.reports.export.charts;

import com.dias.services.reports.export.ReportExcelWriter;
import com.dias.services.reports.report.chart.ChartDescriptor;
import com.dias.services.reports.report.query.Column;
import com.dias.services.reports.report.query.ResultSetWithTotal;
import com.dias.services.reports.service.ReportBuilderService;
import com.dias.services.reports.subsystem.ColumnWithType;
import lombok.experimental.Delegate;
import org.apache.poi.ss.util.CellReference;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.openxmlformats.schemas.drawingml.x2006.main.CTLineProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSchemeColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.STSchemeColorVal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * График c числами по оси X
 */
public class CascadeChart extends BaseChart {

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
    private CTLineChart lineChartWithCurrentValues;
    private CTLineChart lineChartWithPreviousValues;

    public CascadeChart(Map<String, Integer> excelColumnsMap, int firstDataRow, ReportExcelWriter reportExcelWriter, ResultSetWithTotal rs, CTChart ctChart, ChartDescriptor chartDescriptor) {
        super(rs, ctChart, chartDescriptor);
        this.plot = ctChart.getPlotArea();
        CTBarChart barChart = plot.addNewBarChart();
        barChart.addNewBarDir().setVal(STBarDir.COL);
        barChart.addNewGrouping().setVal(STBarGrouping.CLUSTERED);
        barChart.addNewVaryColors().setVal(false);
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
        int startColumnIndex = lastColumn + 1; // берем данные из первой добавочной колонки, содержащей начальное значение
        int endColumnIndex = lastColumn + 4; // берем данные из последней добавочной колонки, содержащей конечное значение
        addTotalBar(dataSheetName, 0, startColumnIndex, 1);
        addTotalBar(dataSheetName, 1, endColumnIndex, 2);

        CTLineChart lineChart = plot.addNewLineChart();

        addLineSeries(lineChart, dataSheetName, 2, excelColumnsMap.get(diagramSeries.getValueColumn()), 0);
        addLineSeries(lineChart, dataSheetName, 3, lastColumn + 2, 3); // предыдущие значения

        CTUpDownBars updowns = lineChart.addNewUpDownBars();
        updowns.addNewGapWidth().setVal(150);
        CTUpDownBar upBars = updowns.addNewUpBars();

        CTShapeProperties sp = upBars.addNewSpPr();
        sp.addNewSolidFill().addNewSchemeClr().setVal(STSchemeColorVal.ACCENT_1);
        CTLineProperties ln = sp.addNewLn();
        ln.setW(9525);
        CTSchemeColor scheme = ln.addNewSolidFill().addNewSchemeClr();
        scheme.setVal(STSchemeColorVal.TX_1);
        scheme.addNewLumMod().setVal(15000);
        scheme.addNewLumOff().setVal(85000);


        CTUpDownBar downBars = updowns.addNewDownBars();
        sp = downBars.addNewSpPr();
        sp.addNewSolidFill().addNewSchemeClr().setVal(STSchemeColorVal.ACCENT_1);
        ln = sp.addNewLn();
        ln.setW(9525);
        scheme.setVal(STSchemeColorVal.TX_1);
        scheme.addNewLumMod().setVal(15000);
        scheme.addNewLumOff().setVal(85000);

    }

    private void addLineSeries(CTLineChart lineChart, String dataSheetName, int seriesIndex, Integer valueColumnIndex, long order) {

        lineChart.addNewAxId().setVal(AXIS_X_ID);
        lineChart.addNewAxId().setVal(AXIS_Y_ID);

        CTLineSer lineSeries = lineChart.addNewSer();
        lineSeries.addNewOrder().setVal(order);
        lineSeries.addNewIdx().setVal(seriesIndex);


        lineSeries.addNewCat().addNewStrRef().setF(dataSheetName + "!$" + xColumnName + "$" + from + ":$" + xColumnName + "$" + to);
        CTNumDataSource ctNumDataSource = lineSeries.addNewVal();
        CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();
        String valueColumnName = CellReference.convertNumToColString(valueColumnIndex);
        ctNumRef.setF(dataSheetName + "!$" + valueColumnName + "$" + from + ":$" + valueColumnName + "$" + to);


    }

    private void addTotalBar(String dataSheetName, int seriesIndex, int columnIndex, long order) {
        //ISeries chartSeries = addNewSeries(diagramSeries);
        CTBarSer chartSeries = totalBarChart.addNewSer();
        chartSeries.addNewOrder().setVal(order);
        chartSeries.addNewIdx().setVal(seriesIndex);

        chartSeries.addNewCat().addNewStrRef().setF(dataSheetName + "!$" + xColumnName + "$" + from + ":$" + xColumnName + "$" + to);
        CTNumDataSource ctNumDataSource = chartSeries.addNewVal();
        CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();
        String valueColumnName = CellReference.convertNumToColString(columnIndex);
        ctNumRef.setF(dataSheetName + "!$" + valueColumnName + "$" + from + ":$" + valueColumnName + "$" + to);
    }

    private void updateDataSheet() {
        List<ChartDescriptor.Series> series = chartDescriptor.getSeries();
        Map<String, Integer> rsColumnsMap = rs.getColumnsMap();
        ChartDescriptor.Series s = series.get(0);
        Integer valueColumnIndex = rsColumnsMap.get(s.getValueColumn());
        ResultSetWithTotal calculatedData = new ResultSetWithTotal();
        List<List<Object>> newRows = new ArrayList<>();
        List<ColumnWithType> newHeaders = new ArrayList<>();
        newHeaders.add(ColumnWithType.builder().title("Начальное значение").type(ReportBuilderService.JAVA_TYPE_NUMERIC).build());
        newHeaders.add(ColumnWithType.builder().title("Предыдущее значение").type(ReportBuilderService.JAVA_TYPE_NUMERIC).build());
        newHeaders.add(ColumnWithType.builder().title("Разница").type(ReportBuilderService.JAVA_TYPE_NUMERIC).build());
        newHeaders.add(ColumnWithType.builder().title("Конечное значение").type(ReportBuilderService.JAVA_TYPE_NUMERIC).build());
        calculatedData.setHeaders(newHeaders);
        calculatedData.setRows(newRows);
        Double previous = 0D;
        for (int i = fromRowIndex; i < toRowIndex; i++) {
            List<Object> row = rs.getRows().get(i);
            Double value = (Double) row.get(valueColumnIndex);
            value = value == null ? 0D : value;
            List<Object> newRow = new ArrayList<>();
            if (i == fromRowIndex) {
                previous = value;
                newRow.add(value);
            } else {
                newRow.add(null);
            }
            newRow.add(previous);
            newRow.add(value - previous);

            if (i == toRowIndex - 1) {
                newRow.add(value);
            } else {
                newRow.add(null);
            }

            previous = value;
            newRows.add(newRow);
            if (value > yMinMax[1]) {
                yMinMax[1] = value;
            }
            if (value < yMinMax[0]) {
                yMinMax[0] = value;
            }
        }

        reportExcelWriter.joinTable(rs, calculatedData);
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


