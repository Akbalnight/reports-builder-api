package com.dias.services.reports.report.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Объект SQL запроса с колонкой
 */
@Getter
@Setter
@NoArgsConstructor
public class Column {

    // содержит в себе имя таблицы и имя колонки
    // в общем случае также содержит имя схемы базы данных,
    // если таблица использует схему
    private String column;
    private String title;

    @JsonIgnore
    private String schemeName;
    @JsonIgnore
    private String tableName;
    @JsonIgnore
    private String columnName;

    /**
     *
     * @param column - имя колонки
     * @param parseName - флаг, необходимо ли пытаться распарсить имя колонки
     */
    public Column(String column, boolean parseName) {
        if (parseName) {
            setColumn(column);
        } else {
            this.column = column;
            columnName = column;
        }
    }

    public void setColumn(String column) {
        this.column = column;
        if (column != null) {
            String[] parts = column.split("\\.");
            if (parts.length > 1) {

                StringBuilder st = new StringBuilder();
                for (int x = 2; x < parts.length; x++) {
                    st.append(parts[x]);
                    if(x!=parts.length-1){
                        st.append(".");
                    }
                }
                columnName = st.toString();
//                columnName = parts[parts.length - 1];
                tableName = parts[1];
                if (parts.length > 1) {
                    schemeName = parts[0];
                }
            } else {
                columnName = column;
            }
        }
    }

    /**
     * Представление для SQL запроса. tableName в данном случае alias полного имени таблицы
     *
     * @return Строка для вставки в SQL запрос
     */
    public String toSQL() {
        return (tableName != null ? tableName + "." : "") + columnName;
    }

    /**
     * Пользовательское представление колонки
     *
     * @return Строка для отображения пользователю
     */
    public String toUser() {
        return title != null ? title : columnName;
    }


    /**
     * Полное имя таблицы со схемой, если она задана
     * @return Полное имя таблицы со схемой, если задана
     */
    @JsonIgnore
    public String getFullTableName() {
        if (tableName == null) {
            return null;
        }
        return (schemeName != null ? schemeName + "." : "") + tableName;
    }
}
