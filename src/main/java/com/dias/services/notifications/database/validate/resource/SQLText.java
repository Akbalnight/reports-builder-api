package com.dias.services.notifications.database.validate.resource;

/**
 * SQLText.java
 * Date: 19 февр. 2019 г.
 * Users: vmeshkov
 * Description: Реализация интерфейса для sql запроса, как строки
 */
public class SQLText
        implements IResource {
    /**
     * Имя схемы
     */
    private final String schemeName;
    /**
     * Текст запроса
     */
    private String sqlText;

    public SQLText(String sqlText, String schemeName) {
        this.sqlText = sqlText;
        this.schemeName = schemeName;
    }

    @Override
    public String getSQL() {
        return sqlText.replaceAll("\\{scheme.name\\}", schemeName);
    }
}
