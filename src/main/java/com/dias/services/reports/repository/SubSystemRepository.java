package com.dias.services.reports.repository;

import com.dias.services.reports.model.SubSystem;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

@Repository
public class SubSystemRepository extends AbstractRepository<SubSystem> {

    @Autowired
    public SubSystemRepository(NamedParameterJdbcTemplate template) {
        super(template);
    }

    public NamedParameterJdbcTemplate getTemplate() {
        return template;
    }

    static class SubSystemRowMapper implements RowMapper {

        @Override
        public SubSystem mapRow(ResultSet rs, int rowNumber) throws SQLException {
            SubSystem report = new SubSystem();
            report.setId(rs.getLong("id"));
            report.setName(rs.getString("name"));
            report.setTitle(rs.getString("title"));
            report.setViews((String[]) rs.getArray("views").getArray());
            return report;
        }
    }

    private static final RowMapper<SubSystem> ROW_MAPPER = new SubSystemRowMapper();



    @Override
    public String getTableName() {
        return "report";
    }

    @Override
    public RowMapper<SubSystem> getRowMapper() {
        return ROW_MAPPER;
    }

    @Override
    protected String getInsertSql() {
        return "insert into subsystem (" +
                "id," +
                "name," +
                "title," +
                "views" +
                ") values (" +
                "nextval('subsystem_id_seq')," +
                ":name," +
                ":title," +
                ":views)";
    }

    @Override
    protected String getUpdateSql() {
        return "update subsystem set " +
                "name=:name, " +
                "title=:title, " +
                "views=:views " +
                "where id=:id";
    }

    public List<SubSystem> getAvailabeSubSystems(Set<String> filterByName) {
        String subsystemNames = StringUtils.join(filterByName, "','");
        String sql = "select * from subsystem where name in ('" + subsystemNames + "')";
        return template.query(sql, ROW_MAPPER);
    }

}
