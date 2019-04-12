package com.dias.services.reports.export.charts;

import lombok.experimental.Delegate;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumDataSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScatterSer;

/**
 * Серия графика с числовой осью X
 */
public class ScatterSer implements ISeries {

    @Delegate
    private final CTScatterSer series;
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
}
