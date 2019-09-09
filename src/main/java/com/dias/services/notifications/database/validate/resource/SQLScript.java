package com.dias.services.notifications.database.validate.resource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;

import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * SQLScript.java
 * Date: 19 февр. 2019 г.
 * Users: vmeshkov
 * Description: Реализация интерфейса для sql запросов, в виде файла
 */
public class SQLScript
        implements IResource {
    /**
     * Имя схемы
     */
    private final String schemeName;
    /**
     * Файл со скриптом
     */
    private String sqlScript;

    public SQLScript(String sqlScript, String schemeName) {
        this.sqlScript = sqlScript;
        this.schemeName = schemeName;
    }

    @Override
    public String getSQL() throws IOException {
        InputStreamReader streamReader = new InputStreamReader(getClass().getResourceAsStream(sqlScript),
                StandardCharsets.UTF_8);
        LineNumberReader reader = new LineNumberReader(streamReader);
        String script = ScriptUtils.readScript(reader, "--", ";");
        return script.replaceAll("\\{scheme.name\\}", schemeName);
    }
}
