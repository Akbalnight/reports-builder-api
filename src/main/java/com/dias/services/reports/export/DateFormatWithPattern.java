package com.dias.services.reports.export;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

/**
 * Обертка над паттерном даты и форматтерами.
 * Необходима главным образом по причине использования в JFreeChart классического форматтера
 */
public class DateFormatWithPattern {
    private final String pattern;
    private DateTimeFormatter format;
    private SimpleDateFormat classicFormat;

    DateFormatWithPattern(String pattern) {
        this.pattern = pattern;
    }

    public DateFormat toClassicFormat() {
        if (classicFormat == null) {
            classicFormat = new SimpleDateFormat(pattern);
        }
        return classicFormat;
    }

    public DateTimeFormatter toFormat() {
        if (format == null) {
            format = DateTimeFormatter.ofPattern(pattern);
        }
        return format;
    }
}
