package com.dias.services.reports.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.annotation.Transactional;
import com.dias.services.reports.model.AbstractModel;

import java.util.List;

import static java.util.Collections.singletonMap;
import static org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils.createBatch;

public abstract class AbstractRepository<T extends AbstractModel> {

    protected NamedParameterJdbcTemplate template;

    public static final String ID = "id";

    public AbstractRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public NamedParameterJdbcTemplate getTemplate() {
        return template;
    }

    public abstract String getTableName();

    public abstract RowMapper<T> getRowMapper();

    /* Getting all Items from table */
    public List<T> getAll() {
        String sql = "SELECT * FROM " + getTableName() + " ORDER BY " + ID;
        return template.query(sql, getRowMapper());
    }

    /* Getting a specific item by item id from table */
    public T getById(Long id) {
        try {
            String sql = "SELECT * FROM " + getTableName() + " WHERE id = :id";
            return template.queryForObject(sql, singletonMap("id", id), getRowMapper());
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    /**
     * Common method generating insert sql.
     * Override getColumnFields() to provide model's columns and fields.
     * @return insert sql
     */
    protected abstract String getInsertSql();

    /**
     * Common method generating update sql.
     * Override getColumnFields() to provide model's columns and fields.
     * @return update sql
     */
    protected abstract String getUpdateSql();

    /* Create item */
    @Transactional
    public void create(T model) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        template.update(getInsertSql(), getSqlParameterSource(model), keyHolder, new String[]{"id"});
        model.setId(keyHolder.getKey().longValue());
    }

    /* Create items */
    @Transactional
    public void create(List<T> models) {
        template.batchUpdate(getInsertSql(), createBatch(models));
    }

    /* Update item */
    @Transactional
    public void update(T model) {
        template.update(getUpdateSql(), getSqlParameterSource(model));
    }

    /* Delete item */
    @Transactional
    public int delete(Long id) {
        String sql = "DELETE FROM " + getTableName() + " WHERE id = :id";
        return template.update(sql, singletonMap("id", id));
    }

    protected SqlParameterSource getSqlParameterSource(T model) {
        return new BeanPropertySqlParameterSource(model);
    }





}
