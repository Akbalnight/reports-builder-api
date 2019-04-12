package com.dias.services.reports.export.charts;

import lombok.experimental.Delegate;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTitle;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTValAx;

public class NumericAxis implements IAxisX {
    @Delegate
    private final CTValAx ctValAx;
    NumericAxis(CTValAx ctValAx) {
        this.ctValAx = ctValAx;
    }
    @Override
    public boolean supportsMinMax() {
        return true;
    }
    @Override
    public CTTitle addTitle() {
        return ctValAx.addNewTitle();
    }
}
