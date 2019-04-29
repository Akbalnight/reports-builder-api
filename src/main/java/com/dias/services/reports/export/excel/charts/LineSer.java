package com.dias.services.reports.export.excel.charts;

import lombok.experimental.Delegate;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTMarker;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;

import java.awt.*;

public class LineSer implements ISeries {
    @Delegate
    private final CTLineSer series;
    LineSer(CTLineSer ctLineSer) {
        this.series = ctLineSer;
    }
    @Override
    public void setFforX(String formula) {
        series.addNewCat().addNewStrRef().setF(formula);
    }

    @Override
    public void colorize(Color color) {
        if (color != null) {
            CTShapeProperties sp = series.addNewSpPr();
            sp.addNewLn().addNewSolidFill().addNewSrgbClr().setVal(new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()});
            CTMarker ctMarker = series.addNewMarker();
            CTShapeProperties markerSp = ctMarker.addNewSpPr();
            markerSp.addNewSolidFill().addNewSrgbClr().setVal(new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()});
            markerSp.addNewLn().addNewNoFill();
        }
    }
}
