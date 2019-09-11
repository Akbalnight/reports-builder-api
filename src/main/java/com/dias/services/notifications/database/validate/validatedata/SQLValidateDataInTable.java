package com.dias.services.notifications.database.validate.validatedata;

import com.dias.services.notifications.database.validate.validatedata.strategy.IValidateStrategy;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowCountCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * SQLValidateDataInTable.java
 * Date: 19 февр. 2019 г.
 * Users: vmeshkov
 * Description: TODO
 */
public class SQLValidateDataInTable implements IValidateDataInTable {
    /**
     * Стратегия поведения для случая невалидных данных
     */
    private IValidateStrategy validateStrategy;
    /**
     * Текст запроса
     */
    private String sql;
    /**
     * Проверяемое количество строк
     */
    private int countRows;

    public SQLValidateDataInTable(
            String sql, int countRows,
            IValidateStrategy validateStrategy, String schemeName) {
        this.sql = sql.replaceAll("\\{scheme.name\\}", schemeName);
        this.countRows = countRows;
        this.validateStrategy = validateStrategy;
    }

    @Transactional
    @Override
    public void verify(NamedParameterJdbcTemplate jdbcTemplate) throws DataAccessException, IOException {
        // Проверим количество строк в таблице
        RowCountCallbackHandler countCallback = new RowCountCallbackHandler();
        jdbcTemplate.query(sql, countCallback);
        if (countCallback.getRowCount() != countRows) {
            validateStrategy.behavior(jdbcTemplate);
        }
    }
}
