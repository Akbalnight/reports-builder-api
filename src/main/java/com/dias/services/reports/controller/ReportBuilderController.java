package com.dias.services.reports.controller;

import com.dias.services.reports.security.Role;
import com.dias.services.reports.service.ReportBuilderService;
import com.dias.services.reports.subsystem.ColumnWithType;
import com.dias.services.reports.subsystem.TablesService;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reports/analytics")
@Api(value = "Api for SubSystemController", description = "Контроллер для подсистем")
public class ReportBuilderController {

    private static Logger LOG = Logger.getLogger(ReportBuilderController.class.getName());

    private final ReportBuilderService reportBuilderService;
    private final TablesService tablesService;
    private final ObjectMapper objectMapper;

    @Value("${com.dias.services.reports.service.name:Reports}")
    private String serviceName;

    @Autowired
    public ReportBuilderController(ReportBuilderService reportBuilderService, TablesService tablesService, ObjectMapper objectMapper) {
        this.reportBuilderService = reportBuilderService;
        this.tablesService = tablesService;
        this.objectMapper = objectMapper;
    }

    @ApiOperation(value = "Получение списка подсистем с полным составом колонок для каждой таблицы")
    @GetMapping(value = "/subsystems", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Map<String, Map<String, List<Object>>>>> getSubSystems(@RequestHeader(name = "userRoles", required = false) String userRoles) throws IOException {

        List<String> availableTables = extractUserTablesFromRoles(userRoles);

        //subsystem is hardcoded with list of available tables/views
        try {
            byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/data/subsystems.json"));
            String content = new String(bytes, StandardCharsets.UTF_8);
            Map<String, Map<String, Map<String, List<Object>>>> subsystems = objectMapper.readerFor(Map.class).readValue(content);
            if (subsystems != null) {
                filterSubSystemsByPermissions(subsystems, availableTables);
                return new ResponseEntity<>(subsystems, HttpStatus.OK);
            }
        } catch (Exception ex) {
            throw new IOException(ex);
        }
        return new ResponseEntity<>(Collections.EMPTY_MAP, HttpStatus.OK);
    }

    private List<String> extractUserTablesFromRoles(String userRoles) throws IOException {

        //TODO Подключить работу с пермиссиями по новой схеме (через компонент)
        if (true || userRoles == null) {
            return Collections.emptyList();
        }

        MappingIterator<Object> roles = objectMapper.readerFor(Role.class).readValues(URLDecoder.decode(userRoles, "UTF-8"));
        List<String> tables = new ArrayList<>();
        while (roles.hasNext()) {
            Role role = (Role) roles.next();
            Role.RoleJsonObject jsonData = role.getJsonData();
            if (jsonData != null) {
                Map<String, String> objects = jsonData.getObjects();
                if (objects != null) {
                    Map<String, String> filtered = objects.entrySet().stream()
                            .filter(stringStringEntry -> stringStringEntry.getKey().equals(serviceName))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    filtered.values().forEach(s -> {
                        try {
                            Map<String, Map<String, List<String>>> subSystem = objectMapper.readerFor(Map.class).readValue(s);
                            subSystem.values().forEach(stringListMap -> stringListMap.values().forEach(tables::addAll));
                        } catch (IOException ignore) {
                        }
                    });
                }
            }
        }
        return tables;
    }

    /**
     * Переданный список подсистем фильтруется в зависимости от доступности пользователю
     * Доступность должна определяться пермиссиями, которые передаются в заголовке запроса
     * @param subsystems
     * @param userTables
     */
    private void filterSubSystemsByPermissions(Map<String, Map<String, Map<String, List<Object>>>> subsystems, List<String> userTables) {
        Iterator<Map.Entry<String, Map<String, Map<String, List<Object>>>>> subSystemEntriesIterator = subsystems.entrySet().iterator();
        while (subSystemEntriesIterator.hasNext()) {
            Map.Entry<String, Map<String, Map<String, List<Object>>>> subSystemEntry = subSystemEntriesIterator.next();
            Map<String, Map<String, List<Object>>> reportsGroupMap = subSystemEntry.getValue();
            Iterator<Map.Entry<String, Map<String, List<Object>>>> reportGroupsIterator = reportsGroupMap.entrySet().iterator();
            while (reportGroupsIterator.hasNext()) {
                Map.Entry<String, Map<String, List<Object>>> entry = reportGroupsIterator.next();
                Iterator<Map.Entry<String, List<Object>>> tablesIterator = entry.getValue().entrySet().iterator();
                while (tablesIterator.hasNext()) {
                    Map.Entry<String, List<Object>> tableEntry = tablesIterator.next();
                    String table = tableEntry.getKey();
                    if (!isTableAvailableForUser(userTables, table)) {
                        tablesIterator.remove();
                    } else {
                        ArrayList<Object> columns = (ArrayList<Object>) tableEntry.getValue();
                        String tableName = tablesService.getTableNameByTitle(table);
                        columns.addAll(reportBuilderService.getTableDescription(tableName));
                    }
                }
                if (entry.getValue().entrySet().isEmpty()) {
                    reportGroupsIterator.remove();
                }
            }
            if (reportsGroupMap.entrySet().isEmpty()) {
                subSystemEntriesIterator.remove();
            }
        }
    }

    private boolean isTableAvailableForUser(List<String> userTables, String table) {
        //TODO всегда проверять на доступность
        if (userTables.isEmpty()) {
            //LOG.severe("User roles are not provided in request header or list of available objects for report service is empty");
            return true;
        }
        return userTables.contains(table);
    }

    @ApiOperation(value = "Описание таблицы")
    @GetMapping(value="/tables/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ColumnWithType> getTableDescription(@PathVariable(value = "name") String viewName) {
        return reportBuilderService.getTableDescription(viewName);
    }

    /*@ApiOperation(value = "Получение списка подсистем с доступными текущему пользователю views")
    @GetMapping(value = "/subsystems", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SubSystemWithFilteredViews>> getSubSystems(@RequestHeader(value = "PERMISSIONS") String permissions) {
        return new ResponseEntity<>(subSystemService.getAvailableSubSystems(permissions), HttpStatus.OK);
    }

    @ApiOperation(value = "Создание подсистемы")
    @PostMapping(value="/subsystems", produces = MediaType.APPLICATION_JSON_VALUE)
    public SubSystem createSubSystem(@RequestBody SubSystem subSystem) {
        subSystemService.create(subSystem);
        return subSystem;
    }

    @ApiOperation(value = "Описание view")
    @GetMapping(value="/views/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ViewDescription getTableDescription(@PathVariable(value = "name") String viewName) {
        return subSystemService.getTableDescription(viewName);
    }*/
}
