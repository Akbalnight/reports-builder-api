package com.dias.services.notifications.database.validate;

import java.io.IOException;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.dias.services.notifications.database.validate.validatedata.IValidateDataInTable;

/**
 * ValidateDao.java
 * Date: 23 янв. 2019 г.
 * Users: vmeshkov
 * Description: Абстрактный класс для DAO классов с валидацией базы данных
 */
public abstract class ValidateDao {
    /**
     * Темплейт для выполнения запросов
     *
     * @return Темплейт для выполнения запросов
     */
    abstract protected NamedParameterJdbcTemplate getJDBCTemplate();

    /**
     * Проверка целостности базы
     *
     * @throws IOException
     */
    @Transactional
    public void validate(ValidateTable[] validateElements) throws IOException {
        StringBuilder executeQuery = new StringBuilder();
        NamedParameterJdbcTemplate jdbcTemplate = getJDBCTemplate();
        // Целостность базы
        if (validateElements != null) {
            for (ValidateTable v : validateElements) {
                try {
                    jdbcTemplate.getJdbcOperations().execute(v.getValidateResource().getSQL());
                } catch (DataAccessException e) {
                    String sql = v.getCreateResource().getSQL();
                    executeQuery.append(sql + (sql.endsWith(";") ? "" : ";"));
                }
            }
        }

        // Выполним скрипты по созданию, пересозданию таблиц
        if (executeQuery.length() > 0) {
            jdbcTemplate.getJdbcOperations().execute(executeQuery.toString());
        }

        // Целостность данных
        for (ValidateTable v : validateElements) {
            IValidateDataInTable validateRows = v.getValidateRows();
            if (validateRows != null) {
                validateRows.verify(jdbcTemplate);
            }
        }
    }
}
