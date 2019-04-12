package com.dias.services.reports.export.charts;

import lombok.experimental.Delegate;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScatterChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTValAx;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;

/**
 * График c числами по оси X
 */
public class ScatterChart implements IChartWithSeries {
    @Delegate
    private final CTScatterChart ctScatterChart;
    private final CTPlotArea plot;
    public ScatterChart(CTScatterChart ctScatterChart, CTPlotArea plot) {
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

    @Override
    public CTValAx addNewValAx() {
        return plot.addNewValAx();
    }
}


