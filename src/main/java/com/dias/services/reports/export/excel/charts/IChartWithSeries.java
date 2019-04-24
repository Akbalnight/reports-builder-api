package com.dias.services.reports.export.excel.charts;

import com.dias.services.reports.report.chart.ChartDescriptor;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTUnsignedInt;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTValAx;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;

/**
 * Интерфейс диаграммы с сериями. Служит для представления абстракции диаграммы
 * для Apache POI, в которой отсутствует общий интерфейс для диаграмм не смотря на обилие
 * одинаковой логики. Введение данного интерфейса позволяет избавится от многочисленного
 * дублирования кода. Внутри реализации интерфейса происходит делегирование к Apache POI диаграмме
 */
public interface IChartWithSeries {
    ISeries addNewSeries(ChartDescriptor.Series s);
    CTUnsignedInt addNewAxId();
    CTShapeProperties addNewShapeProperties(int seriesIndex);
    IAxisX addAxisX(CTPlotArea plotArea, boolean isCategoryAxisNumeric);
    CTValAx addNewValAx();
}
