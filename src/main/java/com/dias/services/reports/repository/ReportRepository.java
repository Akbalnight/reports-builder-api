package com.dias.services.reports.repository;

import com.dias.services.reports.model.Report;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Repository
public class ReportRepository extends AbstractRepository<Report> {

    private static final String INIT_SCRIPT = "/db/schema.sql";

    private static Logger LOG = Logger.getLogger(ReportRepository.class.getName());

    @Autowired
    public ReportRepository(NamedParameterJdbcTemplate template) {
        super(template);
    }

    static class ReportRowMapper implements RowMapper {

        @Override
        public Report mapRow(ResultSet rs, int rowNumber) throws SQLException {
            Report report = new Report();
            report.setId(rs.getLong("id"));
            report.setName(rs.getString("name"));
            report.setTitle(rs.getString("title"));
            report.setType(rs.getString("type"));
            report.setQueryDescriptor(rs.getString("query_descriptor"));
            report.setCreatedBy(rs.getString("created_by"));
            report.setDescription(rs.getString("description"));
            report.setIsFavorite(rs.getBoolean("is_favorite"));
            report.setIsPublic(rs.getBoolean("is_public"));
            return report;
        }
    }

    private static final RowMapper<Report> ROW_MAPPER = new ReportRowMapper();


    @PostConstruct
    public void init() {
        createTablesIfNeeded();
    }

    /**
     * Создание таблиц в БД если они не существуют
     */
    private void createTablesIfNeeded() {
        try {
            executeSqlFromFile(getClass(), template, INIT_SCRIPT);
        } catch (Exception e) {
            LOG.severe("Ошибка инициализации модуля отчетов");
        }
    }

    /**
     * Выполняет SQL скрипт из указанного файла
     *
     * @param path
     * @throws IOException
     */
    @Transactional
    public void executeSqlFromFile(Class clazz, NamedParameterJdbcTemplate template, String path) throws IOException {
        InputStreamReader streamReader = new InputStreamReader(clazz.getResourceAsStream(path), "UTF-8");
        LineNumberReader reader = new LineNumberReader(streamReader);
        String query = ScriptUtils.readScript(reader, "--", ";");
        List<String> queries = new ArrayList<>();
        ScriptUtils.splitSqlScript(query, ";", queries);
        for (String qry: queries) {
            try {
                template.getJdbcOperations().execute(qry);
            } catch (Exception ignore) {
            }
        }

    }

    @Override
    public String getTableName() {
        return "report";
    }

    @Override
    public RowMapper<Report> getRowMapper() {
        return ROW_MAPPER;
    }

    @Override
    protected String getInsertSql() {
        return "insert into report (" +
                "id," +
                "name," +
                "title," +
                "type," +
                "created_by," +
                "description," +
                "is_favorite," +
                "is_public," +
                "query_descriptor" +
                ") values (" +
                "nextval('report_id_seq')," +
                ":name," +
                ":title," +
                ":type," +
                ":createdBy," +
                ":description," +
                ":isFavorite," +
                ":isPublic," +
                ":queryDescriptor)";
    }

    @Override
    protected String getUpdateSql() {
        return "update report set " +
                "name=:name, " +
                "title=:title, " +
                "type=:type, " +
                "created_by=:createdBy, " +
                "description=:description, " +
                "is_favorite=:isFavorite, " +
                "is_public=:isPublic, " +
                "query_descriptor=:queryDescriptor " +
                "where id=:id";
    }

}
