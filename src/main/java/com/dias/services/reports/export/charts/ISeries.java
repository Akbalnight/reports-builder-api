package com.dias.services.reports.export.charts;

import com.dias.services.reports.report.chart.ChartDescriptor;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;

import java.awt.*;

/**
 * Интерфейс серии. Как и <code>IChartWithSeries</code> служит абстракцией для серий из
 * Apache POI. Внутри реализации серии происходит делегирование к Apache POI серии
 */
public interface ISeries {

    CTUnsignedInt addNewIdx();
    CTNumDataSource addNewVal();
    void setFforX(String formula);
    CTSerTx addNewTx();
    CTDLbls addNewDLbls();

    /**
     * Дополнительная обработка специфичная для каждого типа графика
     *
     * @param from Начальная строка
     * @param to Конечная строка
     */
    default void doWithRange(int from, int to) {
    }

    default void addDotValues(ChartDescriptor chartDescriptor, Integer dataLabelPos) {
        if (chartDescriptor.isShowDotValues()) {
            //добавляем метки к столбцам
            CTDLbls dLbls = addNewDLbls();
            //укажем положение - OUT_END (соответствует 7)
            CTDLblPos ctdLblPos = dLbls.addNewDLblPos();
            ctdLblPos.setVal(STDLblPos.OUT_END);
            dLbls.addNewShowVal().setVal(true);
            //отключим отображение всего лишнего
            dLbls.addNewShowSerName().setVal(false);
            dLbls.addNewShowCatName().setVal(false);
            dLbls.addNewShowBubbleSize().setVal(false);
            dLbls.addNewShowLeaderLines().setVal(false);
            dLbls.addNewShowLegendKey().setVal(false);
        }
    }

    void colorize(Color color);
}

