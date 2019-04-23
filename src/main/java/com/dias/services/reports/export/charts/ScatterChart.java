package com.dias.services.reports.export.charts;

import com.dias.services.reports.report.chart.ChartDescriptor;
import com.dias.services.reports.report.query.ResultSetWithTotal;
import lombok.experimental.Delegate;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;

/**
 * График c числами по оси X
 */
public class ScatterChart extends BaseChart {
    @Delegate
    private final CTScatterChart ctScatterChart;
    private final CTPlotArea plot;

    public ScatterChart(ResultSetWithTotal rs, CTChart ctChart, ChartDescriptor chartDescriptor, STScatterStyle.Enum style) {
        super(rs, ctChart, chartDescriptor);
        this.plot = ctChart.getPlotArea();
        this.ctScatterChart = plot.addNewScatterChart();
        this.ctScatterChart.addNewScatterStyle().setVal(style);
        this.ctScatterChart.addNewVaryColors().setVal(false);
    }

    @Override
    public ISeries addNewSeries(ChartDescriptor.Series s) {
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

    @Override
    protected int getValueLabelsLocation() {
        return LABEL_POSITION_TOP;
    }
}


