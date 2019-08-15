package com.dias.services.reports.query;

import com.dias.services.reports.export.ExportChartsHelper;
import com.dias.services.reports.report.query.Calculation;
import com.dias.services.reports.report.query.Column;
import com.dias.services.reports.report.query.Condition;
import com.dias.services.reports.report.query.QueryDescriptor;
import com.dias.services.reports.service.ReportBuilderService;
import com.dias.services.reports.subsystem.ColumnWithType;
import com.dias.services.reports.subsystem.TablesService;
import com.dias.services.reports.translation.Translator;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * Построитель запроса.
 * Построитель не использует группировки.
 * Основные возможности: поддержка select, where, order by, limit
 *
 */
public class NoGroupByQueryBuilder {

    private static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd";
    private static final SimpleDateFormat isoFormatter = new SimpleDateFormat(ISO_8601_DATE_FORMAT);
    private static Logger LOG = Logger.getLogger(NoGroupByQueryBuilder.class.getName());

    private static final String CLAUSE_SELECT   = "SELECT ";
    private static final String CLAUSE_FROM     = " FROM ";
    private static final String CLAUSE_WHERE    = " WHERE ";
    private static final String CLAUSE_ORDER_BY = " ORDER BY ";
    private static final String CLAUSE_LIMIT    = " LIMIT ";
    private static final String CLAUSE_OFFSET   = " OFFSET ";
    private static final String CLAUSE_AS       = " as ";
    private static final String CLAUSE_AND      = " and ";

    private static final String COMMA_SEPARATOR = ",";
    private static final String OPEN_BRACKET = "(";
    private static final String CLOSE_BRACKET = ")";
    private static final String DOUBLE_QUOTES = "\"";
    private static final String QUOTE = "'";
    private static final String SPACE = " ";
    private static final String ASTERISC = "*";

    private final QueryDescriptor descriptor;
    private final Set<TableName> tableNames;
    private String tablesJoin;

    private Long limit;
    private Long offset;
    private Map<String, Map<String, ColumnWithType>> columnWithTypes;

    public NoGroupByQueryBuilder(QueryDescriptor descriptor, TablesService tablesService) {
        this(descriptor, tablesService, false);
    }

    public NoGroupByQueryBuilder(QueryDescriptor descriptor, TablesService tablesService, boolean resolveGroupByByCalculations) {
        this.descriptor = tablesService.createNewWithTableNames(descriptor);
        this.tableNames = tablesService.extractTableNames(this.descriptor);
        if (tableNames.size() > 1) {
            tablesJoin = tablesService.getTablesJoin(tableNames);
        }

    }
    public NoGroupByQueryBuilder withRowLimit(Long limit) {
        this.limit = limit;
        return this;
    }

    public NoGroupByQueryBuilder withOffset(Long offset) {
        this.offset = offset;
        return this;
    }

    public NoGroupByQueryBuilder withColumns(Map<String, Map<String, ColumnWithType>> columnWithTypes) {
        this.columnWithTypes = columnWithTypes;
        return this;
    }

    /**
     *
     * Генерация запроса по SELECT полям. Поля из агрегаций игнорируются.
     * Запрос по агрегацим можно получить, вызвав <code>buildSummaryQuery</code>
     * В этом случае получается  по сути строка итогов
     *
     * @return Запрос по SELECT без агрегаций
     */
    public String buildSelectQuery() {
        StringBuilder bld = new StringBuilder();


        Column[] select = descriptor.getSelect();
        Column[] orderBy = descriptor.getOrderBy();
        Condition[] where = descriptor.getWhere();

        bld.append(CLAUSE_SELECT);
        if (!descriptorColumnsEmpty(select)) {
            bld.append(getColumnsForQuerySeparatedByComma(select));
        } else {
            bld.append(ASTERISC);
        }

        bld.append(CLAUSE_FROM);
        bld.append(buildFromClause());

        if (!descriptorColumnsEmpty(where) || (tablesJoin != null && !tablesJoin.isEmpty())) {
            bld.append(CLAUSE_WHERE);
            bld.append(buildWhereClause(where));
        }

        if (!descriptorColumnsEmpty(orderBy)) {
            bld.append(CLAUSE_ORDER_BY);
            bld.append(getColumnsForQuerySeparatedByComma(orderBy));
        }

        if (limit != null && limit > 0) {
            bld.append(CLAUSE_LIMIT).append(limit);
            if (offset != null && offset > 0) {
                bld.append(CLAUSE_OFFSET).append(offset);
            }
        }

        return bld.toString();
    }

    /**
     *
     * Генерация запроса по aggregation полям. Поля из SELECT игнорируются.
     * По сути, получаем итоговую строку
     *
     * @return Запрос для получения итогов
     */
    public String buildSummaryQuery() {

        StringBuilder bld = new StringBuilder();
        bld.append(CLAUSE_SELECT);

        if (!descriptorColumnsEmpty(descriptor.getAggregations())) {
            bld.append(buildCalculations(descriptor.getAggregations()));
        }
        bld.append(CLAUSE_FROM);
        bld.append(buildFromClause());

        Condition[] where = descriptor.getWhere();

        if (!descriptorColumnsEmpty(where) || (tablesJoin != null && !tablesJoin.isEmpty())) {
            bld.append(CLAUSE_WHERE);
            bld.append(buildWhereClause(where));
        }

        return bld.toString();
    }



    private String buildWhereClause(Condition[] where) {
        StringBuilder bld = new StringBuilder();
        String whereByDesriptor = buildWhereStatement(where, columnWithTypes, false, null);
        bld.append(whereByDesriptor);
        if (tablesJoin != null && !tablesJoin.isEmpty()) {
            if (!whereByDesriptor.isEmpty()) {
                bld.append(CLAUSE_AND);
            }
            bld.append(tablesJoin);
        }
        return bld.toString();
    }

    private String buildFromClause() {
        StringBuilder bld = new StringBuilder();
        TableName[] tableNamesArray = tableNames.toArray(new TableName[0]);
        for (int i = 0; i < tableNamesArray.length; i++) {
            TableName tableName = tableNamesArray[i];
            bld.append(tableName.getTable()).append(SPACE).append(tableName.getTableName());
            if (i < tableNames.size() - 1) {
                bld.append(COMMA_SEPARATOR);
            }
        }
        return bld.toString();
    }

    private String buildCalculations(Calculation[] aggregations) {
        List<String> aggregationsInStrings = Arrays.stream(aggregations).map(aggr -> {
            StringBuilder functionBuilder = new StringBuilder();
            functionBuilder.append(aggr.getFunction()).append(OPEN_BRACKET).append(aggr.getColumn()).append(CLOSE_BRACKET);
            String header = aggr.getTitle() != null ? aggr.getTitle() : generateTitleForAggregateFunction(aggr);
            functionBuilder.append(CLAUSE_AS).append(DOUBLE_QUOTES).append(header).append(DOUBLE_QUOTES);
            return functionBuilder.toString();
        }).collect(Collectors.toList());
        return StringUtils.join(aggregationsInStrings, COMMA_SEPARATOR);
    }

    public static String buildWhereStatement(Condition[] where, Map<String, Map<String, ColumnWithType>> columnWithTypes, boolean beautify, Translator translator) {
        if (where == null || where.length == 0) {
            return "";
        }
        StringBuilder bld = new StringBuilder();
        for (int i = 0; i < where.length; i++) {
            bld.append(buildWherePart(where[i], columnWithTypes, 0, beautify));
            if (i < where.length - 1) {
                //AND is used by default
                bld.append(CLAUSE_AND);
            }
        }
        if (beautify) {
            String result = new BasicFormatterImpl().format(bld.toString());
            if (translator != null) {
                String[] tokens = result.split(SPACE);
                for (int i = 0; i < tokens.length; i++) {
                    tokens[i] = translator.translate(tokens[i]);
                }
                result = StringUtils.join(tokens, SPACE);
            }
            return result;
        }
        return bld.toString();
    }

    private static String buildWherePart(Condition part, Map<String, Map<String, ColumnWithType>> columnWithTypes, int deep, boolean beautify) {
        StringBuilder bld = new StringBuilder();

        if (part.getColumn() != null) {
            Object value = part.getValue();
            bld.append(beautify ? part.toUser() : part.toSQL());
            bld.append(SPACE);
            String operator = part.getOperator();
            boolean requiresQuoting = value != null && requiresQuoting(part.getColumnName(), part.getFullTableName(), value, columnWithTypes);

            //специальная обработка для value = null значений
            //для = сравнения оставляем null как есть, а = меняем на is
            //для всех других сравнений меняем null на соответствующее типу минимальное значение
            if (value == null) {
                value = "null";
                if ("=".equals(operator)) {
                    operator = " is ";
                } else {
                    ColumnWithType columnWithType = defineColumnWithType(part.getColumnName(), part.getFullTableName(), columnWithTypes);
                    if (columnWithType != null) {
                        if (ReportBuilderService.JAVA_TYPE_NUMERIC.equals(columnWithType.getType())) {
                            value = "0";
                        } else if (ReportBuilderService.JAVA_TYPE_DATE.equals(columnWithType.getType())) {
                            value = "to_timestamp(0)";
                        }
                    }
                }
            } else {
                value = transformToISOifDate(value);
            }


            if ("[contains],[not contains]".contains("[" + operator.toLowerCase() + "]")) {
                operator = operator.toLowerCase().replace("contains", "like");
                value = "%" + value + "%";
            } else if ("[is null],[is not null]".contains("[" + operator.toLowerCase() + "]")) {
                value = "";
                requiresQuoting = false;
            } else if ("[in],[not in]".contains("[" + operator.toLowerCase() + "]")) {
                if (requiresQuoting) {
                    String[] values = value.toString().split(",");
                    value = OPEN_BRACKET + QUOTE + StringUtils.join(values, QUOTE + "," + QUOTE) + QUOTE + CLOSE_BRACKET;
                } else {
                    value = OPEN_BRACKET + value + CLOSE_BRACKET;
                }
                requiresQuoting = false;
            }

            bld.append(operator);

            bld.append(SPACE);

            if (requiresQuoting) {
                bld.append(QUOTE);
            }
            bld.append(value);
            if (requiresQuoting) {
                bld.append(QUOTE);
            }
        } else if (part.getLeft() != null) {
            boolean putIntoBrackets = deep > 0;
            deep++;
            if (putIntoBrackets) {
                bld.append(OPEN_BRACKET);
            }
            bld.append(buildWherePart(part.getLeft(), columnWithTypes, deep, beautify));
            bld.append(SPACE);
            bld.append(part.getOperand());
            bld.append(SPACE);
            bld.append(buildWherePart(part.getRight(), columnWithTypes, deep, beautify));
            if (putIntoBrackets) {
                bld.append(CLOSE_BRACKET);
            }
        }
        return bld.toString();
    }

    private static Object transformToISOifDate(Object value) {
        // дата может прийти в произвольном формате со стороны клиента
        // сначала попытаемся ее распарсить, затем трансформировать в ISO 8601
        String strValue = value.toString();
        String format = ExportChartsHelper.calculateDateFormatPattern(strValue);
        if (format != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            try {
                Date date = dateFormat.parse(strValue);
                return isoFormatter.format(date);
            } catch (ParseException e) {
                LOG.severe("Значение не удалось форматировать в ISO 8601 формат даты: " + strValue);
                return value;
            }
        }
        return value;
    }

    private String generateTitleForAggregateFunction(Calculation aggr) {
        return aggr.getFunction().toUpperCase() + "_" + aggr.getColumn().toUpperCase();
    }

    private String getColumnsForQuerySeparatedByComma(Column[] columns) {
        List<String> columnNames = Arrays.stream(columns).map(Column::toSQL).collect(Collectors.toList());
        return StringUtils.join(columnNames, COMMA_SEPARATOR);
    }

    private boolean descriptorColumnsEmpty(Column[] columns) {
        return columns == null || columns.length == 0;
    }

    private static boolean requiresQuoting(String columnName, String fullTableName, Object value, Map<String, Map<String, ColumnWithType>> columnWithTypes) {

        boolean requires = value != null && !Number.class.isAssignableFrom(value.getClass());
        //Если значение явно передано как число, тогда сразу возвращаем ответ
        //в противном случае пытаемся определить по типам
        if (requires && columnWithTypes != null) {
            ColumnWithType columnWithType = defineColumnWithType(columnName, fullTableName, columnWithTypes);
            requires = columnWithType == null || columnWithType.isRequiresQuoting();
        }
        return requires;
    }

    private static ColumnWithType defineColumnWithType(String column, String table, Map<String, Map<String, ColumnWithType>> columnWithTypes) {

        if (columnWithTypes != null) {
            Map<String, ColumnWithType> columnsToSearchIn;
            if (table != null) {
                columnsToSearchIn = columnWithTypes.get(table);
            } else {
                columnsToSearchIn = new HashMap<>();
                Map<String, ColumnWithType> finalColumnsToSearchIn = columnsToSearchIn;
                columnWithTypes.values().forEach(finalColumnsToSearchIn::putAll);
            }

            return columnsToSearchIn.get(column);
        }
        return null;
    }

}
