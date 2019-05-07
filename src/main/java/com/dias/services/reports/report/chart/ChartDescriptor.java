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

    public static final String SERIES_TYPE_LINEAR = "linear";
    public static final String SERIES_TYPE_BAR = "bar";
    public static final String SERIES_TYPE_AREA = "area";
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
        private String type; // для комбинированной диаграммы. каждая серия имеет свой тип
        private String valueColumn;
        private String dataKey;
        private String color;
        private String colorPositive;
        private String colorNegative;
        private String colorInitial;
        private String colorTotal;
        private String title;
        private Integer startRow;
        private Integer endRow;

        public Color getAwtColor() {
            return toAwtColor(color);
        }

        public Color toAwtColor(String colorString) {
            if (colorString != null && colorString.startsWith("#")) {
                return new java.awt.Color(Integer.parseInt(colorString.substring(1), 16));
            }
            return null;
        }
    }

}
