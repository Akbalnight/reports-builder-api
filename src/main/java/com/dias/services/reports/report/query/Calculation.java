package com.dias.services.reports.report.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Аггрегационная функция
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Calculation extends Column {
    private String function;
}
