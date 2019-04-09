package com.dias.services.reports.report.query;

import com.dias.services.reports.query.TotalValue;
import com.dias.services.reports.service.ReportBuilderService;
import com.dias.services.reports.subsystem.ColumnWithType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

/**
 * Данные возвращаемые запросом плюс строка с итогами
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResultSetWithTotal {

    private List<List<Object>> rows;
    private List<ColumnWithType> headers;
    private List<TotalValue> total;
    @JsonIgnore
    private List<Integer> groupRowsIndexes;

    /**
     *
     * Генерируем итоговую строку, для подстановки в таблицу
     * Таким образом, количество колонок итоговой строки равно количеству колонок в резалтсете
     *
     * @return строка, дополняющая основной резалтсет итогами
     */
    public List<Object> generateTableReadyTotalRow() {
        List<Object> totalRow = new ArrayList<>();
        if (headers != null) {
            for (ColumnWithType header : headers) {
                boolean aggregateColumn = false;
                if (total != null) {
                    for (TotalValue totalValue : total) {
                        if (totalValue.getColumn().equals(header.getColumn())) {
                            totalRow.add(totalValue.getValue());
                            aggregateColumn = true;
                            break;
                        }
                    }
                    if (!aggregateColumn) {
                        totalRow.add("");
                    }
                }
            }
        }
        return totalRow;
    }

    /**
     *
     * Содержит ли резалтсет итог
     *
     * @return true если резалтсет содержит итог
     */
    public boolean containsTotal() {
        return total != null && !total.isEmpty();
    }

    /**
     * Функция возвращает сопоставление имя или заголовок колонки к индексу в резалтсете
     *
     * @return map column->index в резалтсете
     */
    @JsonIgnore
    public Map<String, Integer> getColumnsMap() {
        Map<String, Integer> result = new HashMap<>();
        List<ColumnWithType> headers = getHeaders();
        for (int i = 0; i < headers.size(); i++) {
            ColumnWithType column = headers.get(i);

            Column column1 = new Column(column.getColumn());
            String columnName = column1.getColumnName();
            result.put(columnName, i);
            result.put(column.getTitle(), i);
            result.put(column1.toSQL(), i);
        }
        return result;
    }

    @JsonIgnore
    public ResultSetWithTotal convertToGroupped(Column[] groups, OrderBy[] orderBy) {

        //нечего конвертировать, в случае, если группировки не заданы
        if (groups == null || groups.length == 0) {
            return this;
        }

        Map<String, Integer> columnsMap = getColumnsMap();
        Integer[] groupIndexes = new Integer[groups.length];
        for (int i = 0; i < groups.length; i++) {
            Column group = groups[i];
            groupIndexes[i] = columnsMap.get(group.getTitle());
        }
        Integer[] orderByIndexes = new Integer[orderBy != null ? orderBy.length : 0];
        for (int i = 0; i < orderByIndexes.length; i++) {
            Column orderbByCol = orderBy[i];
            orderByIndexes[i] = columnsMap.get(orderbByCol.getTitle());
        }


        //отсортируем данные по группам и по полю сортировки, если оно задано
        rows.sort((row1, row2) -> {
            for (Integer groupValueIndex : groupIndexes) {
                int compare = Objects.toString(row1.get(groupValueIndex)).compareTo(Objects.toString(row2.get(groupValueIndex)));
                if (compare != 0) {
                    return compare;
                }
            }
            for (Integer orderByIndex : orderByIndexes) {
                int compare = Objects.toString(row1.get(orderByIndex)).compareTo(Objects.toString(row2.get(orderByIndex)));
                if (compare != 0) {
                    return compare;
                }
            }
            return 0;
        });

        int columns = headers.size();
        ResultSetWithTotal newRs = new ResultSetWithTotal();
        newRs.groupRowsIndexes = new ArrayList<>();
        newRs.headers = headers;
        newRs.total = total;
        List<List<Object>> newRows = new ArrayList<>();
        String[] groupValues = new String[groups.length];
        int index = 0;
        for (List<Object> row : rows) {
            for (int j = 0; j < groupValues.length; j++) {
                String groupValue = groupValues[j];
                String groupValueInRow = Objects.toString(row.get(groupIndexes[j]));
                if (groupValue == null || !groupValue.equals(groupValueInRow)) {
                    newRows.add(createGroupRow(groupValueInRow, groups[j], groupIndexes[j], columns));
                    newRs.groupRowsIndexes.add(index);
                    index++;
                    groupValues[j] = groupValueInRow;
                }
            }
            newRows.add(row);
            index++;
        }
        newRs.rows = newRows;
        return newRs;
    }

    private static List<Object> createGroupRow(String groupValue, Column group, int groupIndex, int columns) {
        List<Object> row = new ArrayList<>();
        for (int i = 0; i < columns; i++) {
            if (i == 0) {
                row.add(group.getTitle() + ": " + ("null".equals(groupValue) ? "" : groupValue));
            } else {
                row.add("");
            }
        }
        return row;
    }

    @JsonIgnore
    public List<Integer> getDateColumnsIndexes() {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            if (ReportBuilderService.JAVA_TYPE_DATE.equals(headers.get(i).getType())) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    @JsonIgnore
    public List<Integer> getNumericColumnsIndexes() {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            if (ReportBuilderService.JAVA_TYPE_NUMERIC.equals(headers.get(i).getType())) {
               indexes.add(i);
            }
        }
        return indexes;
    }

}
