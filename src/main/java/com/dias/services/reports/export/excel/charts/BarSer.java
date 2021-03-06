package com.dias.services.reports.export.excel.charts;

import lombok.experimental.Delegate;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarSer;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;

import java.awt.*;

/**
 * Серия для гистограммы
 */
public class BarSer implements ISeries {
    @Delegate
    private final CTBarSer series;
    BarSer(CTBarSer ctBarSer) {
        this.series = ctBarSer;
    }

    @Override
    public void setFforX(String formula) {
        series.addNewCat().addNewStrRef().setF(formula);
    }

    @Override
    public void colorize(Color color) {
        if (color != null) {
            CTShapeProperties seriesShapeProperties = series.addNewSpPr();
            seriesShapeProperties.addNewSolidFill().addNewSrgbClr().setVal(new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()});
        }
    }
}
