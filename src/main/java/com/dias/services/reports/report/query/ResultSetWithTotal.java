package com.dias.services.reports.report.query;

import com.dias.services.reports.query.TotalValue;
import com.dias.services.reports.subsystem.ColumnWithType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public Map<String, Integer> getColumnsMap() {
        Map<String, Integer> result = new HashMap<>();
        List<ColumnWithType> headers = getHeaders();
        for (int i = 0; i < headers.size(); i++) {
            ColumnWithType column = headers.get(i);
            String columnName = column.getColumn();
            result.put(columnName, i);
            result.put(column.getTitle(), i);
            //продублируем колонку без имени таблицы
            if (columnName.contains(".")) {
                result.put(columnName.substring(columnName.indexOf(".") + 1), i);
            }
        }
        return result;
    }
}
