package com.dias.services.reports.export.charts;

import com.dias.services.reports.report.chart.ChartDescriptor;
import com.dias.services.reports.report.query.ResultSetWithTotal;
import lombok.experimental.Delegate;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;

/**
 * Круговая диаграмма
 */
public class PieChart extends BaseChart {

    @Delegate
    private final CTPieChart pieChart;
    private final CTPlotArea plot;

    public PieChart(ResultSetWithTotal rs, CTChart ctChart, ChartDescriptor chartDescriptor) {
        super(rs, ctChart, chartDescriptor);
        this.plot = ctChart.getPlotArea();
        this.pieChart = ctChart.getPlotArea().addNewPieChart();
        this.pieChart.addNewVaryColors().setVal(true);
    }

    @Override
    public ISeries addNewSeries(ChartDescriptor.Series s) {
        return new PieSer(pieChart.addNewSer());
    }

    @Override
    public CTUnsignedInt addNewAxId() {
        return null;
    }

    @Override
    public CTShapeProperties addNewShapeProperties(int seriesIndex) {
        return plot.getPieChartArray(0).getSerArray(seriesIndex).addNewSpPr();
    }

    @Override
    public IAxisX addAxisX(CTPlotArea plotArea, boolean isCategoryAxisNumeric) {
        return null;
    }

    @Override
    public CTValAx addNewValAx() {
        return null;
    }

}
