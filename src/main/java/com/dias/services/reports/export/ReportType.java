package com.dias.services.reports.export;

import org.apache.commons.lang3.StringUtils;

import java.util.logging.Logger;

/**
 * Тип отчета
 */
public enum ReportType {

    hbar, //гистограмма
    linear, //линейный график
    bar, //барчарт
    table, //просто таблица
    pie, // круговая
    scatter,
    combo, // комбинированная диаграмма
    cascade; // каскад (водопад)

    private static Logger LOG = Logger.getLogger(ReportType.class.getName());

    public static ReportType byNameOrDefaultForUnknown(String strType) {
        try {
            return StringUtils.isEmpty(strType) ? ReportType.table : ReportType.valueOf(strType);
        } catch (Exception e) {
            LOG.severe("ReportType is unknown: " + strType);
            return ReportType.table;
        }
    }
}
