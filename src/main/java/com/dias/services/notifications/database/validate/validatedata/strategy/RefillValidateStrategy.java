package com.dias.services.notifications.database.validate.validatedata.strategy;

import com.dias.services.notifications.database.validate.resource.SQLScript;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * RefillValidateStrategy.java
 * Date: 19 февр. 2019 г.
 * Users: vmeshkov
 * Description: Стратегия поведения перезаписи данных в таблице
 */
public class RefillValidateStrategy
        implements IValidateStrategy {
    private SQLScript sqlScript;

    public RefillValidateStrategy(SQLScript sqlScript) {
        this.sqlScript = sqlScript;
    }

    @Transactional
    @Override
    public void behavior(NamedParameterJdbcTemplate jdbcTemplate) throws DataAccessException, IOException {
        jdbcTemplate.getJdbcOperations().execute(sqlScript.getSQL());
    }
}
