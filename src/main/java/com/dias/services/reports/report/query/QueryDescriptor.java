package com.dias.services.reports.report.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;

/**
 * Описание SQL запроса
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QueryDescriptor {
    private String table;
    private Select[] select;
    private Column[] groupBy;
    private OrderBy[] orderBy;
    private Condition[] where;
    private Calculation[] aggregations;

    public QueryDescriptor copy() {
        QueryDescriptor copy = new QueryDescriptor();
        copy.setTable(this.table);
        if (this.select != null) {
            copy.setSelect(Arrays.copyOf(this.select, this.select.length));
        }
        if (this.groupBy != null) {
            copy.setGroupBy(Arrays.copyOf(this.groupBy, this.groupBy.length));
        }
        if (this.orderBy != null) {
            copy.setOrderBy(Arrays.copyOf(this.orderBy, this.orderBy.length));
        }
        if (this.where != null) {
            copy.setWhere(Arrays.copyOf(this.where, this.where.length));
        }
        if (this.aggregations != null) {
            copy.setAggregations(Arrays.copyOf(this.aggregations, this.aggregations.length));
        }
        return copy;
    }
}
