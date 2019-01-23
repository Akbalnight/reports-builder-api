package com.dias.services.reports.report.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderBy extends Column {
    /*
    Порядок сортировки
     */
    private String order;

    @Override
    public String toSQL() {
        //Возвращаем имя колонки с порядком сортировки, если таковой присутствует
        return getColumn() + (order != null ? " " + order : "");
    }
}
