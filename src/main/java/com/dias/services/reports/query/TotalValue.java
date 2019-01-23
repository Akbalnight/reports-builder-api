package com.dias.services.reports.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 *
 * Итоговое значение
 *
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TotalValue {
    private String column;
    private Object value;
}
