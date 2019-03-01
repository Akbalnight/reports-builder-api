package com.dias.services.reports.report.chart;

import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.List;

/**
 * Описание диаграммы
 */
@Getter
@Setter
public class ChartDescriptor {

    private String title;
    private String axisXTitle;
    private String axisYTitle;
    private String axisXColumn;
    private Boolean showLegend;
    private List<Series> series;
    private boolean calculatedXRange;
    private boolean calculatedYRange;
    private boolean showDotValues;

    @Getter
    @Setter
    public static class Series {
        private String valueColumn;
        private String color;
        private String title;
        private Integer startRow;
        private Integer endRow;
        public Color getAwtColor() {
            if (color != null && color.startsWith("#")) {
                return new java.awt.Color(Integer.parseInt(color.substring(1), 16));
            }
            return null;
        }
    }

}
