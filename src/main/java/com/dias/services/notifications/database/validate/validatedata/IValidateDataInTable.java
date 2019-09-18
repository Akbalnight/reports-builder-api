package com.dias.services.notifications.database.validate.validatedata;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.IOException;

/**
 * IValidateDataInTable.java
 * Date: 19 февр. 2019 г.
 * Users: vmeshkov
 * Description: Интерфейс для валидации данных в таблице
 */
public interface IValidateDataInTable {
    void verify(NamedParameterJdbcTemplate jdbcTemplate) throws DataAccessException, IOException;
}
