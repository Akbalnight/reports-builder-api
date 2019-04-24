package com.dias.services.reports.export.excel.charts;

import org.openxmlformats.schemas.drawingml.x2006.chart.*;

public interface IAxisX {
    CTUnsignedInt addNewAxId();
    CTBoolean addNewDelete();
    CTAxPos addNewAxPos();
    CTUnsignedInt addNewCrossAx();
    CTTickLblPos addNewTickLblPos();
    CTScaling addNewScaling();
    CTDouble addNewCrossesAt();
    boolean supportsMinMax();
    CTTitle addTitle();
}
