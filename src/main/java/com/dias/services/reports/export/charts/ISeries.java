package com.dias.services.reports.export.charts;

import org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTNumDataSource;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTSerTx;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTUnsignedInt;

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
}

