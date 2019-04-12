package com.dias.services.reports.export.charts;

import lombok.experimental.Delegate;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTLineSer;

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
}
