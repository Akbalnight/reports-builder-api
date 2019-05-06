package com.dias.services.reports.export.excel.charts;

import com.dias.services.reports.report.chart.ChartDescriptor;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDLblPos;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTScatterSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.STDLblPos;
import org.openxmlformats.schemas.drawingml.x2006.main.CTLineProperties;

import java.awt.*;

public class ScatterMarkerSer extends ScatterSer {
    ScatterMarkerSer(CTScatterSer ctScatterSer) {
        super(ctScatterSer);
    }

    @Override
    public void colorize(Color color) {
        CTLineProperties ln = series.addNewSpPr().addNewLn();
        ln.setW(31750);
        ln.addNewNoFill();
    }

    @Override
    public void addDotValues(ChartDescriptor chartDescriptor, STDLblPos.Enum dataLabelPos) {
        if (chartDescriptor.isShowDotValues()) {
            //добавляем метки к столбцам
            CTDLbls dLbls = addNewDLbls();
            //укажем положение - OUT_END (соответствует 7)
            CTDLblPos ctdLblPos = dLbls.addNewDLblPos();
            ctdLblPos.setVal(STDLblPos.T);
            dLbls.addNewShowVal().setVal(true);
            //отключим отображение всего лишнего
            dLbls.addNewShowSerName().setVal(false);
            dLbls.addNewShowCatName().setVal(false);
            dLbls.addNewShowBubbleSize().setVal(false);
            dLbls.addNewShowLeaderLines().setVal(false);
            dLbls.addNewShowLegendKey().setVal(false);
        }
    }
}
