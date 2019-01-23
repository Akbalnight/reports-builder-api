package com.dias.services.reports.report.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Select extends Column {
    private static final String CLAUSE_AS = " as ";
    private static final String EMPTY_STRING = "";
    private static final String DOUBLE_QUOTES = "\"";

    private boolean sortable;
    private boolean filterable;

    @Override
    public String toSQL() {
        return getColumn()
                + (getTitle() != null ? CLAUSE_AS + DOUBLE_QUOTES + getTitle() + DOUBLE_QUOTES : EMPTY_STRING);
    }
}
