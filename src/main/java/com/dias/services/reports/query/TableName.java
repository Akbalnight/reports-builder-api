package com.dias.services.reports.query;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class TableName {
    private String scheme;
    private String tableName;
    private String table;

    public TableName(String table) {
        setTable(table);
    }

    public TableName(String scheme, String tableName) {
        this.scheme = scheme;
        this.tableName = tableName;
        this.table = (scheme != null ? scheme + "." : "") + tableName;
    }

    public void setTable(String value) {
        if (value != null) {
            this.table = value;
            if (value.contains(".")) {
                String[] parts = value.split("\\.");
                scheme = parts[0];
                tableName = parts[1];
            } else {
                tableName = value;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableName tableName1 = (TableName) o;
        return Objects.equals(scheme, tableName1.scheme) &&
                Objects.equals(tableName, tableName1.tableName) &&
                Objects.equals(table, tableName1.table);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheme, tableName, table);
    }
}
