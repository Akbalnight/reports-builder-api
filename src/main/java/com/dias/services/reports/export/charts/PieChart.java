package com.dias.services.reports.export.charts;

import lombok.experimental.Delegate;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPieChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTUnsignedInt;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTValAx;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;

/**
 * Круговая диаграмма
 */
public class PieChart implements IChartWithSeries {

    @Delegate
    private final CTPieChart pieChart;
    private final CTPlotArea plot;
    public PieChart(CTPieChart ctPieChart, CTPlotArea plot) {
        this.pieChart = ctPieChart;
        this.plot = plot;
    }

    @Override
    public ISeries addNewSeries() {
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
