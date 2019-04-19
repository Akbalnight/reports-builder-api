package com.dias.services.reports.export.charts;

import lombok.experimental.Delegate;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumDataSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScatterSer;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;

import java.awt.*;

/**
 * Серия графика с числовой осью X
 */
public class ScatterSer implements ISeries {

    @Delegate
    protected final CTScatterSer series;
    ScatterSer(CTScatterSer ctScatterSer) {
        this.series = ctScatterSer;
    }

    @Override
    public CTNumDataSource addNewVal() {
        return series.addNewYVal();
    }

    @Override
    public void setFforX(String formula) {
        series.addNewXVal().addNewNumRef().setF(formula);
    }

    @Override
    public void colorize(Color color) {
        if (color != null) {
            CTShapeProperties seriesShapeProperties = series.addNewSpPr();
            seriesShapeProperties.addNewSolidFill().addNewSrgbClr().setVal(new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()});
        }
    }
}
