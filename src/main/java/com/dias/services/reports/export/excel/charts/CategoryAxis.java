package com.dias.services.reports.export.excel.charts;

import lombok.experimental.Delegate;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTCatAx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTitle;

public class CategoryAxis implements IAxisX {
    @Delegate
    private final CTCatAx ctCatAx;
    CategoryAxis(CTCatAx ctCatAx) {
        this.ctCatAx = ctCatAx;
    }
    @Override
    public boolean supportsMinMax() {
        return false;
    }
    @Override
    public CTTitle addTitle() {
        return ctCatAx.addNewTitle();
    }
}
