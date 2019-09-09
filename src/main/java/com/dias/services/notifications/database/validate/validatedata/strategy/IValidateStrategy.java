package com.dias.services.notifications.database.validate.validatedata.strategy;

import java.io.IOException;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * ValidateStrategy.java
 * Date: 19 февр. 2019 г.
 * Users: vmeshkov
 * Description: Интерфейс описывающий стратегию поведения, что делать, если данные в таблице невалидные
 */
public interface IValidateStrategy {
    void behavior(NamedParameterJdbcTemplate jdbcTemplate) throws DataAccessException, IOException;
}
