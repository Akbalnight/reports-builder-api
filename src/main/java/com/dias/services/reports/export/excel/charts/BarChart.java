package com.dias.services.reports.export.excel.charts;

import com.dias.services.reports.report.chart.ChartDescriptor;
import com.dias.services.reports.report.query.ResultSetWithTotal;
import lombok.experimental.Delegate;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;

/**
 * Гистограмма
 */
public class BarChart extends BaseChart {
    @Delegate
    private final CTBarChart barChart;
    private final CTPlotArea plot;

    public BarChart(ResultSetWithTotal rs, CTChart ctChart, ChartDescriptor chartDescriptor, STBarDir.Enum barChartType) {
        super(rs, ctChart, chartDescriptor);
        this.plot = ctChart.getPlotArea();
        this.barChart = plot.addNewBarChart();
        barChart.addNewBarDir().setVal(barChartType);
        barChart.addNewVaryColors().setVal(false);
    }

    @Override
    public ISeries addNewSeries(ChartDescriptor.Series s) {
        return new BarSer(barChart.addNewSer());
    }

    @Override
    public CTShapeProperties addNewShapeProperties(int seriesIndex) {
        return plot.getBarChartArray(0).getSerArray(seriesIndex).addNewSpPr();
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

