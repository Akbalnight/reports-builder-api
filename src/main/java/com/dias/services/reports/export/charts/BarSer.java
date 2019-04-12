package com.dias.services.reports.export.charts;

import lombok.experimental.Delegate;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarSer;

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
}
