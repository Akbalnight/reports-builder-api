package com.dias.services.reports.export.charts;

import lombok.experimental.Delegate;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDPt;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPieSer;
import org.openxmlformats.schemas.drawingml.x2006.main.*;

import java.awt.*;

/**
 * Серия для круговой диаграммы
 */
public class PieSer implements ISeries {
    @Delegate
    private final CTPieSer series;
    PieSer(CTPieSer ctPieSer) {
        this.series = ctPieSer;
    }

    @Override
    public void setFforX(String formula) {
    }

    @Override
    public void doWithRange(int from, int to) {
        int colorIndex = 1;
        int pointIndex = 0;
        for (int i = from; i <= to; i++) {
            CTDPt ctdPt = series.addNewDPt();
            ctdPt.addNewIdx().setVal(pointIndex++);
            ctdPt.addNewBubble3D().setVal(false);
            CTShapeProperties ctShapeProperties = ctdPt.addNewSpPr();
            CTSolidColorFillProperties solidFill = ctShapeProperties.addNewSolidFill();
            CTSchemeColor color = solidFill.addNewSchemeClr();
            // для раскраски используется accent цветовая
            // схема. значения типа Enum. Диапазон ограничен от 1 до 6.
            // отсюда такая логика
            colorIndex = colorIndex > 6 ? 1 : colorIndex;
            color.setVal(STSchemeColorVal.Enum.forString("accent" + Integer.toString(colorIndex++)));
            CTLineProperties lineProperties = ctShapeProperties.addNewLn();
            lineProperties.setW(19050);
            lineProperties.addNewSolidFill().addNewSchemeClr().setVal(STSchemeColorVal.Enum.forString("lt1"));
        }
    }

    @Override
    public void colorize(Color color) {
        if (color != null) {
            CTShapeProperties seriesShapeProperties = series.addNewSpPr();
            seriesShapeProperties.addNewSolidFill().addNewSrgbClr().setVal(new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()});
        }
    }


}
