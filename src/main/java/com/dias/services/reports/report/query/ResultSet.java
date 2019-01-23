package com.dias.services.reports.report.query;

import com.dias.services.reports.subsystem.ColumnWithType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Данные, возвращаемые запросом
 */
@Getter
@Setter
public class ResultSet {
    private List<List<Object>> rows;
    private List<ColumnWithType> headers;
}
