package com.dias.services.reports.export.charts;

import com.dias.services.reports.report.chart.ChartDescriptor;
import com.dias.services.reports.report.query.ResultSetWithTotal;
import lombok.experimental.Delegate;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.openxmlformats.schemas.drawingml.x2006.main.CTLineProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.STSchemeColorVal;

import java.awt.*;

/**
 * График c числами по оси X
 */
public class ScatterMarkerChart extends BaseChart {
    @Delegate
    private final CTScatterChart ctScatterChart;
    private final CTPlotArea plot;

    public ScatterMarkerChart(ResultSetWithTotal rs, CTChart ctChart, ChartDescriptor chartDescriptor) {
        super(rs, ctChart, chartDescriptor);
        this.plot = ctChart.getPlotArea();
        this.ctScatterChart = plot.addNewScatterChart();
        this.ctScatterChart.addNewScatterStyle().setVal(STScatterStyle.LINE_MARKER);
        this.ctScatterChart.addNewVaryColors().setVal(false);
    }

    @Override
    public ISeries addNewSeries(ChartDescriptor.Series s) {
        CTScatterSer ctScatterSer = ctScatterChart.addNewSer();
        CTMarker marker = ctScatterSer.addNewMarker();
        marker.addNewSymbol().setVal(STMarkerStyle.CIRCLE);
        marker.addNewSize().setVal((short) 5);
        CTShapeProperties spPr = marker.addNewSpPr();
        Color color = s.getAwtColor();
        if (color != null) {
            spPr.addNewSolidFill().addNewSrgbClr().setVal(new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()});
        } else {
            spPr.addNewSolidFill().addNewSchemeClr().setVal(STSchemeColorVal.ACCENT_1);
            CTLineProperties ln = spPr.addNewLn();
            ln.setW(9525);
            ln.addNewSolidFill().addNewSchemeClr().setVal(STSchemeColorVal.ACCENT_1);
        }


        return new ScatterMarkerSer(ctScatterSer);
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


