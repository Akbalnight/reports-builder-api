package com.dias.services.reports.export.charts;

import com.dias.services.reports.export.ReportExcelWriter;
import com.dias.services.reports.report.chart.ChartDescriptor;
import com.dias.services.reports.report.query.Column;
import com.dias.services.reports.report.query.ResultSetWithTotal;
import com.dias.services.reports.subsystem.ColumnWithType;
import lombok.experimental.Delegate;
import org.apache.poi.ss.util.CellReference;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;

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

    public CascadeChart(ReportExcelWriter reportExcelWriter, ResultSetWithTotal rs, CTChart ctChart, ChartDescriptor chartDescriptor) {
        super(rs, ctChart, chartDescriptor);
        this.plot = ctChart.getPlotArea();
        CTBarChart barChart = plot.addNewBarChart();
        barChart.addNewBarDir().setVal(STBarDir.COL);
        barChart.addNewGrouping().setVal(STBarGrouping.CLUSTERED);
        barChart.addNewVaryColors().setVal(false);
        this.totalBarChart = barChart;
        this.reportExcelWriter = reportExcelWriter;
        this.rs = rs;
    }

    @Override
    public void addSeries(int firstDataRow, Map<String, Integer> excelColumnsMap, String dataSheetName) {
        updateDataSheet();

        ChartDescriptor.Series s = chartDescriptor.getSeries().get(0);
        ISeries chartSeries = addNewSeries(s);

        int xColumn = excelColumnsMap.get(new Column(chartDescriptor.getAxisXColumn()).getColumnName());
        String xColumnName = CellReference.convertNumToColString(xColumn);
        chartSeries.addNewIdx().setVal(0);

        int fromRowIndex = (s.getStartRow() != null && s.getStartRow() > 0) ? s.getStartRow() - 1 : 0;
        int rowsNumber = rs.getRows().size();
        int toRowIndex = (s.getEndRow() != null && s.getEndRow() < rowsNumber) ? s.getEndRow(): rowsNumber;
        int from = firstDataRow + fromRowIndex + 1;
        int to = firstDataRow + toRowIndex;

        chartSeries.setFforX(dataSheetName + "!$" + xColumnName + "$" + from + ":$" + xColumnName + "$" + to);
        CTNumDataSource ctNumDataSource = chartSeries.addNewVal();
        CTNumRef ctNumRef = ctNumDataSource.addNewNumRef();
        int valueColumnIndex = excelColumnsMap.get(new Column(s.getValueColumn()).getColumnName());

        String valueColumnName = CellReference.convertNumToColString(valueColumnIndex);
        ctNumRef.setF(dataSheetName + "!$" + valueColumnName + "$" + from + ":$" + valueColumnName + "$" + from);
    }

    private void updateDataSheet() {
        List<ChartDescriptor.Series> series = chartDescriptor.getSeries();
        Map<String, Integer> rsColumnsMap = rs.getColumnsMap();
        ChartDescriptor.Series s = series.get(0);
        Integer valueColumnIndex = rsColumnsMap.get(s.getValueColumn());
        ResultSetWithTotal calculatedData = new ResultSetWithTotal();
        List<List<Object>> newRows = new ArrayList<>();
        List<ColumnWithType> newHeaders = new ArrayList<>();
        newHeaders.add(ColumnWithType.builder().title("Предыдущее значение").build());
        newHeaders.add(ColumnWithType.builder().title("Разница").build());
        calculatedData.setHeaders(newHeaders);
        calculatedData.setRows(newRows);
        Double previous = 0D;
        for (int i = 0; i < rs.getRows().size(); i++) {
            List<Object> row = rs.getRows().get(i);
            Double value = (Double) row.get(valueColumnIndex);
            value = value == null ? 0D : value;
            List<Object> newRow = new ArrayList<>();
            if (i == 0) {
                previous = value;
            }
            newRow.add(previous);
            newRow.add(value - previous);

            previous = value;
            newRows.add(newRow);
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


