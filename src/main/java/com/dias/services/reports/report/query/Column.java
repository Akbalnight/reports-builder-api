package com.dias.services.reports.report.query;

import lombok.*;

/**
 * Объект SQL запроса с колонкой
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Column {
    private String column;
    private String title;

    /**
     * Представление для SQL запроса
     *
     * @return Строка для вставки в SQL запрос
     */
    public String toSQL() {
        return column;
    }

    /**
     * Пользовательское представление колонки
     *
     * @return Строка для отображения пользователю
     */
    public String toUser() {
        return title != null ? title : column;
    }
}
