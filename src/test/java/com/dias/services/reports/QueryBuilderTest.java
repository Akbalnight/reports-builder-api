package com.dias.services.reports;

import com.dias.services.reports.query.NoGroupByQueryBuilder;
import com.dias.services.reports.report.query.QueryDescriptor;
import com.dias.services.reports.service.ReportBuilderService;
import com.dias.services.reports.subsystem.ColumnWithType;
import com.dias.services.reports.subsystem.TablesService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class QueryBuilderTest {

    private static final Long LIMIT = 10L;
    private static final String QUERY = "SELECT author as \"Автор\",filename as \"Имя файла\" FROM databasechangelog databasechangelog";
    private static final String QUERY_LIMIT = "SELECT author as \"Автор\",filename as \"Имя файла\" FROM databasechangelog databasechangelog LIMIT " + LIMIT;
    private static final String QUERY_ORDER_BY = "SELECT author as \"Автор\",filename as \"Имя файла\" FROM databasechangelog databasechangelog ORDER BY author ASC";
    private static final String QUERY_SELECT_ALL = "SELECT * FROM databasechangelog databasechangelog";
    private static final String QUERY_WITH_WHERE = "SELECT * FROM databasechangelog databasechangelog WHERE filename = 'f_name_0' and (filename = 'f_name_1' or filename = 'f_name_2')";
    private static final String QUERY_WITH_SIMPLE_WHERE = "SELECT * FROM databasechangelog databasechangelog WHERE filename = 'f_name_0' and filename = 'f_name_0'";


    private TablesService tablesService;

    @Before
    public void setup() {
        tablesService = new TablesService(new ObjectMapper());
        tablesService.init();
    }


    @Test
    public void buildSqlByDescriptor() {
        compareWithExpectedQueryResult(null, "QueryBuilder/queryDescriptor.json", QUERY, null);
    }

    @Test
    public void buildSqlByDescriptorWithLimit() {
        compareWithExpectedQueryResult(null, "QueryBuilder/queryDescriptor.json", QUERY_LIMIT, LIMIT);
    }

    @Test
    public void buildSqlByDescriptorWithGroupBy() {
        compareWithExpectedQueryResult(null, "QueryBuilder/queryDescriptorWithOrderBy.json", QUERY_ORDER_BY, null);
    }

    @Test
    public void buildSqlWithAllColumns() {
        compareWithExpectedQueryResult(null, "QueryBuilder/noSelectDescriptor.json", QUERY_SELECT_ALL, null);
    }

    @Test
    public void buildSqlWithWhere() {
        compareWithExpectedQueryResult(null, "QueryBuilder/queryDescriptorWithCondition.json", QUERY_WITH_WHERE, null);
    }

    @Test
    public void buildSqlWithSimpleWhere() {
        compareWithExpectedQueryResult(null, "QueryBuilder/queryDescriptorWithSimpleCondition.json", QUERY_WITH_SIMPLE_WHERE, null);
    }

    @Test
    public void formatSqlTest() {
        QueryDescriptor descriptor = Util.readObjectFromJSON("QueryBuilder/queryDescriptorWithCondition2.json", QueryDescriptor.class);
        String formattedWhere = NoGroupByQueryBuilder.buildWhereStatement(descriptor.getWhere(), null, true, null);
        Assert.assertEquals("\n" +
                "    (\n" +
                "        filename = 'f_name_1' \n" +
                "        or (\n" +
                "            filename = 'f_name_1' \n" +
                "            or filename = 'f_name_2'\n" +
                "        )\n" +
                "    ) \n" +
                "    and (\n" +
                "        filename = 'f_name_1' \n" +
                "        or filename = 'f_name_2'\n" +
                "    )", formattedWhere);
    }


    @Test
    public void buildSqlWithWhereContains() {
        compareQueryResults(null, "QueryBuilder/queryDescriptorWithWhereContains.json", "QueryBuilder/queryDescriptorWithWhereContains_Expected.sql");
    }

    @Test
    public void buildSqlWithWhereLike() {
        compareQueryResults(null, "QueryBuilder/queryDescriptorWithWhereLike.json", "QueryBuilder/queryDescriptorWithWhereLike_Expected.sql");
    }

    @Test
    public void buildSqlWithWhereNotIn() {
        compareQueryResults(null, "QueryBuilder/queryDescriptorWithWhereNotIn.json", "QueryBuilder/queryDescriptorWithWhereNotIn_Expected.sql");
    }

    @Test
    public void buildSqlWithNullValueWhere() {

        //определим типы колонок для теста
        Map<String, Map<String, ColumnWithType>> columnsWithTypes = new HashMap<>();
        Map<String, ColumnWithType> columnsMap = new HashMap<>();
        columnsMap.put("dateexecuted", ColumnWithType.builder().column("dateexecuted").type(ReportBuilderService.JAVA_TYPE_DATE).build());
        columnsMap.put("filename", ColumnWithType.builder().column("filename").type(ReportBuilderService.JAVA_TYPE_STRING).build());
        columnsMap.put("int_val", ColumnWithType.builder().column("int_val").type(ReportBuilderService.JAVA_TYPE_NUMERIC).build());
        columnsWithTypes.put("databasechangelog", columnsMap);

        compareQueryResults(columnsWithTypes, "QueryBuilder/queryDescriptorWithNullValue1.json", "QueryBuilder/queryDescriptorWithNullValue1_Expected.sql");
        compareQueryResults(columnsWithTypes, "QueryBuilder/queryDescriptorWithNullValue2.json", "QueryBuilder/queryDescriptorWithNullValue2_Expected.sql");
        compareQueryResults(columnsWithTypes, "QueryBuilder/queryDescriptorWithNullValue3.json", "QueryBuilder/queryDescriptorWithNullValue3_Expected.sql");

    }

    private void compareQueryResults(Map<String, Map<String, ColumnWithType>> columnsWithTypes, String queryDescriptorResource, String expectedQueryResultResource) {
        compareWithExpectedQueryResult(columnsWithTypes, queryDescriptorResource, Util.readResource(expectedQueryResultResource), null);
    }

    private void compareWithExpectedQueryResult(Map<String, Map<String, ColumnWithType>> columnsWithTypes, String queryDescriptorResource, String expectedQueryResult, Long limit) {
        QueryDescriptor descriptor = Util.readObjectFromJSON(queryDescriptorResource, QueryDescriptor.class);
        String query = new NoGroupByQueryBuilder(descriptor, tablesService)
                .withColumns(columnsWithTypes)
                .withRowLimit(limit)
                .buildSelectQuery();
        Assert.assertEquals(expectedQueryResult, query);
    }

}
