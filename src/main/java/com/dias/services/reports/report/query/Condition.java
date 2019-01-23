package com.dias.services.reports.report.query;

import lombok.Getter;
import lombok.Setter;

/**
 * Условие
 */
@Getter
@Setter
public class Condition extends Column {
    private String operator;
    private Object value;
    private String operand;
    Condition left;
    Condition right;

}
