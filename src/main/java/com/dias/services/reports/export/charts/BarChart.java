package com.dias.services.reports.export.charts;

import lombok.experimental.Delegate;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTValAx;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;

/**
 * Гистограмма
 */
public class BarChart implements IChartWithSeries {
    @Delegate
    private final CTBarChart barChart;
    private final CTPlotArea plot;
    public BarChart(CTBarChart ctBarChart, CTPlotArea plot) {
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

    @Override
    public CTValAx addNewValAx() {
        return plot.addNewValAx();
    }
}

