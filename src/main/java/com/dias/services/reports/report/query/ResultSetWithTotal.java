package com.dias.services.reports.report.query;

import com.dias.services.reports.query.TotalValue;
import com.dias.services.reports.subsystem.ColumnWithType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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
     * @return
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
     * @return
     */
    public boolean containsTotal() {
        return total != null && !total.isEmpty();
    }
}
