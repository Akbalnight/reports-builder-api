package com.dias.services.reports;

import com.dias.services.reports.repository.ReportRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.logging.Logger;

@SpringBootApplication(scanBasePackages = {"com.dias.services.reports"})
public class ReportsApplication {

    private static final Logger LOG = Logger.getLogger(ReportsApplication.class.getName());
    private static final String TABLE_EXISTS_SCRIPT = "SELECT EXISTS ( " +
            "        SELECT 1 " +
            "        FROM   information_schema.tables " +
            "        WHERE  table_schema = 'public' " +
            "        AND    table_name = '" + ReportRepository.REPORTS_TABLE_NAME + "')";

    private static final String SEQUENCE_EXISTS_SCRIPT = "SELECT EXISTS ( " +
            "        SELECT 1 FROM pg_class where relname = '" + ReportRepository.REPORTS_SEQUENCE_NAME + "')";


    private static final RowMapper<Boolean> ROW_MAPPER = (resultSet, i) -> resultSet.getBoolean(1);

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ReportsApplication.class, args);
        checkServiceTablesExists(context);
    }

    private static void checkServiceTablesExists(ConfigurableApplicationContext context) {
        NamedParameterJdbcTemplate template = context.getBean(NamedParameterJdbcTemplate.class);
        LOG.info("Проверка наличия таблиц модуля отчетов");
        Boolean exists = template.queryForObject(TABLE_EXISTS_SCRIPT, new HashMap<>(), ROW_MAPPER);
        if (exists == null || !exists) {
            LOG.severe("Ошибка инициализации модуля отчетов - необходимые таблицы отсутствуют");
            context.close();
        } else {
            LOG.info("Проверка наличия генератора последовательности");
            exists = template.queryForObject(SEQUENCE_EXISTS_SCRIPT, new HashMap<>(), ROW_MAPPER);
            if (exists == null || !exists) {
                LOG.severe("Ошибка инициализации модуля отчетов - отсутствует генератор последовательности");
                context.close();
            } else {
                LOG.info("Проверка прошла успешно");
            }
        }
    }
}
