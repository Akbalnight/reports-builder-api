package com.dias.services.reports.export.charts;

import com.dias.services.reports.report.chart.ChartDescriptor;
import lombok.experimental.Delegate;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTValAx;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;

/**
 * График с категориями по оси X
 */
public class LineChart implements IChartWithSeries {
    @Delegate
    private final CTLineChart lineChart;
    private final CTPlotArea plot;
    public LineChart(CTLineChart ctLineChart, CTPlotArea plot) {
        this.lineChart = ctLineChart;
        this.plot = plot;
    }

    @Override
    public ISeries addNewSeries(ChartDescriptor.Series s) {
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

    @Override
    public CTValAx addNewValAx() {
        return plot.addNewValAx();
    }
}

