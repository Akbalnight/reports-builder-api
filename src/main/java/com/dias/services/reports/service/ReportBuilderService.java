package com.dias.services.reports.service;

import com.dias.services.reports.model.SubSystem;
import com.dias.services.reports.repository.SubSystemRepository;
import com.dias.services.reports.subsystem.ColumnWithType;
import com.dias.services.reports.subsystem.TablesService;
import com.dias.services.reports.translation.Translator;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class ReportBuilderService extends AbstractService<SubSystem> {

    public static final String PG_TYPE_TIMESTAMP = "timestamp";
    public static final String JAVA_TYPE_NUMERIC = "numeric";
    public static final String JAVA_TYPE_DATE = "date";
    public static final String JAVA_TYPE_STRING = "string";
    private static Logger LOG = Logger.getLogger(ReportBuilderService.class.getName());

    private static final String TABLE_STRUCTURE_QUERY = "SELECT LOWER(column_name) as column_name, data_type FROM information_schema.columns WHERE LOWER(table_name) = '%s'";

    private final SubSystemRepository subSystemRepository;
    private final NamedParameterJdbcTemplate template;
    private final TablesService tablesService;

    private static Map<String, Pair<Integer, Boolean>> typesMap = new HashMap<>();

    @Autowired
    public ReportBuilderService(SubSystemRepository subSystemRepository, Translator translator, TablesService tablesService) {
        this.subSystemRepository = subSystemRepository;
        this.template = subSystemRepository.getTemplate();
        this.tablesService = tablesService;
    }

    static {

        typesMap.put("timestamp without time zone", Pair.of(Types.OTHER, true));
        typesMap.put("timestamp", Pair.of(Types.OTHER, true));
        typesMap.put("character varying", Pair.of(Types.OTHER, true));
        typesMap.put("text", Pair.of(Types.VARCHAR, true));

        typesMap.put("numeric", Pair.of(Types.NUMERIC, false));
        typesMap.put("integer", Pair.of(Types.OTHER, false));
        typesMap.put("bigint", Pair.of(Types.OTHER, false));
        typesMap.put("smallint", Pair.of(Types.NUMERIC, false));
        typesMap.put("decimal", Pair.of(Types.NUMERIC, false));
        typesMap.put("real", Pair.of(Types.NUMERIC, false));
        typesMap.put("smallserial", Pair.of(Types.NUMERIC, false));
        typesMap.put("serial", Pair.of(Types.NUMERIC, false));
        typesMap.put("bigserial", Pair.of(Types.NUMERIC, false));
        typesMap.put("double precision", Pair.of(Types.NUMERIC, false));

    }

    @Override
    protected SubSystemRepository getRepository() {
        return subSystemRepository;
    }


    public List<ColumnWithType> getTableDescription(String tableName, boolean filterIgnorableFields) {
        List<ColumnWithType> columns = template.query(String.format(TABLE_STRUCTURE_QUERY, tableName), new RowMapper<ColumnWithType>() {
            @Nullable
            @Override
            public ColumnWithType mapRow(ResultSet rs, int rowNum) throws SQLException {
                String columnName = rs.getString("column_name");
                String title = tablesService.translateColumnInTable(columnName, tableName);
                String pgType = rs.getString("data_type");
                String type;
                Pair<Integer, Boolean> typeDescription = typesMap.get(pgType);
                if (typeDescription == null) {
                    typeDescription = Pair.of(Types.OTHER, true);
                }
                boolean requiresQuoting = typeDescription.getRight();
                int sqlType = typeDescription.getLeft();
                JDBCType jdbcType = JDBCType.valueOf(sqlType);
                if (!requiresQuoting) {
                    type = JAVA_TYPE_NUMERIC;
                } else if (jdbcType == JDBCType.DATE
                        || jdbcType == JDBCType.TIME
                        || jdbcType == JDBCType.TIME_WITH_TIMEZONE
                        || jdbcType == JDBCType.TIMESTAMP_WITH_TIMEZONE
                        || jdbcType == JDBCType.TIMESTAMP
                        || pgType.contains(PG_TYPE_TIMESTAMP)) {
                    type = JAVA_TYPE_DATE;
                } else {
                    type = JAVA_TYPE_STRING;
                }

                return ColumnWithType.builder()
                        .column(columnName)
                        .type(type)
                        .title(title)
                        .requiresQuoting(requiresQuoting)
                        .build();
            }
        });

        if (filterIgnorableFields) {
            columns = columns.stream().filter(columnWithType -> !tablesService.isColumnIgnoredInTable(columnWithType, tableName)).collect(Collectors.toList());
        }

        return columns;
    }

    public Map<String, ColumnWithType> getTableColumnsDescription(String tableName) {
        List<ColumnWithType> columns = getTableDescription(tableName, false);
        return columns.stream().collect(Collectors.toMap(ColumnWithType::getColumn, Function.identity()));
    }


}
