package com.dias.services.reports;

import com.dias.services.reports.query.NoGroupByQueryBuilder;
import com.dias.services.reports.report.query.QueryDescriptor;
import com.dias.services.reports.subsystem.TablesService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
        QueryDescriptor descriptor = Util.readObjectFromJSON("QueryBuilder/queryDescriptor.json", QueryDescriptor.class);
        String query = new NoGroupByQueryBuilder(descriptor, tablesService).buildSelectQuery();
        Assert.assertEquals(QUERY, query);
    }

    @Test
    public void buildSqlByDescriptorWithLimit() {
        QueryDescriptor descriptor = Util.readObjectFromJSON("QueryBuilder/queryDescriptor.json", QueryDescriptor.class);
        String query = new NoGroupByQueryBuilder(descriptor, tablesService).withRowLimit(LIMIT).buildSelectQuery();
        Assert.assertEquals(QUERY_LIMIT, query);
    }

    @Test
    public void buildSqlByDescriptorWithGroupBy() {
        QueryDescriptor descriptor = Util.readObjectFromJSON("QueryBuilder/queryDescriptorWithOrderBy.json", QueryDescriptor.class);
        String query = new NoGroupByQueryBuilder(descriptor, tablesService).buildSelectQuery();
        Assert.assertEquals(QUERY_ORDER_BY, query);
    }

    @Test
    public void buildSqlWithAllColumns() {
        QueryDescriptor descriptor = Util.readObjectFromJSON("QueryBuilder/noSelectDescriptor.json", QueryDescriptor.class);
        String query = new NoGroupByQueryBuilder(descriptor, tablesService).buildSelectQuery();
        Assert.assertEquals(QUERY_SELECT_ALL, query);
    }

    @Test
    public void buildSqlWithWhere() {
        QueryDescriptor descriptor = Util.readObjectFromJSON("QueryBuilder/queryDescriptorWithCondition.json", QueryDescriptor.class);
        String query = new NoGroupByQueryBuilder(descriptor, tablesService).buildSelectQuery();
        Assert.assertEquals(QUERY_WITH_WHERE, query);
    }

    @Test
    public void buildSqlWithSimpleWhere() {
        QueryDescriptor descriptor = Util.readObjectFromJSON("QueryBuilder/queryDescriptorWithSimpleCondition.json", QueryDescriptor.class);
        String query = new NoGroupByQueryBuilder(descriptor, tablesService).buildSelectQuery();
        Assert.assertEquals(QUERY_WITH_SIMPLE_WHERE, query);
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
        QueryDescriptor descriptor = Util.readObjectFromJSON("QueryBuilder/queryDescriptorWithWhereContains.json", QueryDescriptor.class);
        String query = new NoGroupByQueryBuilder(descriptor, tablesService).buildSelectQuery();
        Assert.assertEquals(Util.readResource("QueryBuilder/queryDescriptorWithWhereContains_Expected.sql"), query);
    }

    @Test
    public void buildSqlWithWhereLike() {
        QueryDescriptor descriptor = Util.readObjectFromJSON("QueryBuilder/queryDescriptorWithWhereLike.json", QueryDescriptor.class);
        String query = new NoGroupByQueryBuilder(descriptor, tablesService).buildSelectQuery();
        Assert.assertEquals(Util.readResource("QueryBuilder/queryDescriptorWithWhereLike_Expected.sql"), query);
    }

    @Test
    public void buildSqlWithWhereNotIn() {
        QueryDescriptor descriptor = Util.readObjectFromJSON("QueryBuilder/queryDescriptorWithWhereNotIn.json", QueryDescriptor.class);
        String query = new NoGroupByQueryBuilder(descriptor, tablesService).buildSelectQuery();
        Assert.assertEquals(Util.readResource("QueryBuilder/queryDescriptorWithWhereNotIn_Expected.sql"), query);
    }

}
