package com.dias.services.reports.subsystem;

import com.dias.services.reports.report.query.Column;
import com.dias.services.reports.report.query.QueryDescriptor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 *
 * Сервис предоставляет информацию о доступных для отчетов таблицах, структуре, локализованных названий полей
 * Данные храняться в файле tables.json
 *
 */
@Component
public class TablesService {

    private final ObjectMapper objectMapper;
    private Map<String, Table> tables;
    private Map<String, String> tableNamesRussianToEnglish;
    private Map<String, String> tableNamesEnglishToRussian;
    private Map<List<String>, String>  tablesJoinRules;

    @Autowired
    public TablesService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        Map<String, Map<String, Object>> map = new HashMap<>();
        this.tables = new HashMap<>();
        tableNamesRussianToEnglish = new HashMap<>();
        tableNamesEnglishToRussian = new HashMap<>();
        try {
            Path path = new File(getClass().getResource("/data/tables.json").toURI()).toPath();
            byte[] data = Files.readAllBytes(path);
            String strData = new String(data, "UTF-8");
            map = objectMapper.readerFor(Map.class).readValue(strData);
            map.forEach((s, stringObjectMap) -> {
                Table t = new Table();
                t.setTitle((String) stringObjectMap.get("title"));
                t.setIgnoreFields((List<String>) stringObjectMap.get("ignoreFields"));
                t.setTranslations((Map<String, String>) stringObjectMap.get("translations"));
                tables.put(s, t);
                tableNamesRussianToEnglish.put(t.getTitle(), s);
                tableNamesEnglishToRussian.put(s, t.getTitle());
            });
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        try {
            tablesJoinRules = new HashMap<>();
            Path path = new File(getClass().getResource("/data/joins.json").toURI()).toPath();
            byte[] data = Files.readAllBytes(path);
            String strData = new String(data, "UTF-8");
            JsonNode joins = objectMapper.readTree(strData);
            for (int i = 0; i < joins.size(); i++) {
                List<String> tables = new ArrayList<>();
                JsonNode joinNode = joins.get(i);
                JsonNode tablesNode = joinNode.get("tables");
                for (int j = 0; j < tablesNode.size(); j++) {
                    tables.add(tablesNode.get(j).asText());
                }
                tablesJoinRules.put(tables, joinNode.get("rule").asText());
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }




    }

    public String getTableNameByTitle(String title) {
        if (tableNamesRussianToEnglish.containsKey(title)) {
            return tableNamesRussianToEnglish.get(title);
        }
        return title;
    }

    public String getTableTitleByName(String tableName) {
        if (tableNamesEnglishToRussian.containsKey(tableName)) {
            return tableNamesEnglishToRussian.get(tableName);
        }
        return tableName;
    }


    public String translateColumnInTable(String columnName, String tableName) {
        Table table = tables.get(tableName);
        String translation = columnName;
        if (table != null) {
            Map<String, String> translations = table.getTranslations();
            if (translations != null && translations.containsKey(columnName)) {
                translation = translations.get(columnName);
            }
        }
        return translation;
    }

    public boolean isColumnIgnoredInTable(ColumnWithType columnWithType, String tableName) {
        boolean result = false;
        Table table = tables.get(tableName);
        if (table != null) {
            List<String> ignoreFields = table.getIgnoreFields();
            result = ignoreFields != null && ignoreFields.contains(columnWithType.getColumn());
        }
        return result;
    }

    /**
     *
     * Создается копия исходного дескриптора с приведенными к исходным именами таблиц
     *
     * @param descriptor
     * @return
     */
    public QueryDescriptor createNewWithTableNames(QueryDescriptor descriptor) {
        QueryDescriptor newDescriptor = descriptor.copy();
        tableNamesToEnglish(newDescriptor.getSelect());
        tableNamesToEnglish(newDescriptor.getGroupBy());
        tableNamesToEnglish(newDescriptor.getOrderBy());
        tableNamesToEnglish(newDescriptor.getWhere());
        tableNamesToEnglish(newDescriptor.getAggregations());
        return newDescriptor;
    }

    private void tableNamesToEnglish(Column[] columns) {
        if (columns != null) {
            for (Column column : columns) {
                String col = column.getColumn();
                if (col != null) {
                    int index = col.indexOf(".");
                    if (index > 0) {
                        String tableTitle = col.substring(0, index);
                        column.setColumn(getTableNameByTitle(tableTitle) + "." + col.substring(index + 1));
                    }
                }
            }
        }
    }

    private Set<String> uniqueTableNamesFromColumns(Column[] columns) {
        Set<String> tableNames = new HashSet<>();
        if (columns != null) {
            for (Column column : columns) {
                String col = column.getColumn();
                if (col != null) {
                    int index = col.indexOf(".");
                    if (index > 0) {
                        String tableTitle = col.substring(0, index);
                        tableNames.add(getTableNameByTitle(tableTitle));
                    }
                }
            }
        }
        return tableNames;
    }

    /**
     *
     * Извлечение уникальных имен таблиц, встречающихся в выражениях
     *
     * @param descriptor
     * @return
     */
    public Set<String> extractTableNames(QueryDescriptor descriptor) {
        Set<String> tableNames = new HashSet<>();
        if (descriptor.getTable() != null) {
            tableNames.add(getTableNameByTitle(descriptor.getTable()));
        }
        tableNames.addAll(uniqueTableNamesFromColumns(descriptor.getSelect()));
        tableNames.addAll(uniqueTableNamesFromColumns(descriptor.getGroupBy()));
        tableNames.addAll(uniqueTableNamesFromColumns(descriptor.getOrderBy()));
        tableNames.addAll(uniqueTableNamesFromColumns(descriptor.getWhere()));
        tableNames.addAll(uniqueTableNamesFromColumns(descriptor.getAggregations()));
        return tableNames;
    }

    /**
     * Получение условия соединения таблиц
     * Возвращается то условие, в котором каждая перечисленная таблица содержится в переданной коллекции
     *
     * @param tableNames
     * @return
     */
    public String getTablesJoin(Set<String> tableNames) {
        String rule = null;
        for (List<String> tables: tablesJoinRules.keySet()){
            boolean found = true;
            for (String t: tables) {
                if (!tableNames.contains(t)) {
                    found = false;
                    break;
                }
            }
            if (found) {
                rule = tablesJoinRules.get(tables);
                break;
            }
        }
        return rule;
    }

    public String toRussianTableAndColumn(String column) {
        String[] parts = column.split("\\.");
        return parts.length == 2 ? getTableTitleByName(parts[0]) + "." + parts[1] : column;
    }

}
