package com.dias.services.reports.controller;

import com.dias.services.reports.service.ReportBuilderService;
import com.dias.services.reports.subsystem.ColumnWithType;
import com.dias.services.reports.subsystem.TablesService;
import com.dias.services.reports.utils.SubsystemUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/reports/analytics")
@Api(value = "Api for SubSystemController", description = "Контроллер для подсистем")
public class ReportBuilderController {

    private final ReportBuilderService reportBuilderService;
    private final ObjectMapper objectMapper;
    private final TablesService tablesService;
    private final SubsystemUtils subsystemUtils;

    @Value("${com.dias.services.reports.service.name:Reports}")
    private String serviceName;

    @Autowired
    public ReportBuilderController(ReportBuilderService reportBuilderService, ObjectMapper objectMapper, TablesService tablesService, SubsystemUtils subsystemUtils) {
        this.reportBuilderService = reportBuilderService;
        this.objectMapper = objectMapper;
        this.tablesService = tablesService;
        this.subsystemUtils = subsystemUtils;
    }

    @ApiOperation(value = "Получение списка подсистем с полным составом колонок для каждой таблицы")
    @GetMapping(value = "/subsystems", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getSubSystems(@RequestHeader(name = "userRoles", required = false) String userRoles) throws IOException {

        try {
            byte[] bytes = subsystemUtils.loadResource("/data/subsystems.json");
            String content = new String(bytes, StandardCharsets.UTF_8);
            JsonNode subsystems = objectMapper.readTree(content);
            enrichWithColumns(subsystems);
            return new ResponseEntity<>(subsystems, HttpStatus.OK);
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    private void enrichWithColumns(JsonNode subsystems) {

        for (int i = 0; i < subsystems.size(); i++) {
            JsonNode subsystem = subsystems.get(i);
            JsonNode children = subsystem.get("children");
            for (int j = 0; j < children.size(); j++) {
                JsonNode tablesNode = children.get(j).get("children");
                for (int k = 0; k < tablesNode.size(); k++) {
                    JsonNode tableNode = tablesNode.get(k);
                    String tableName = tableNode.get("name").asText();
                    ((ObjectNode)tableNode).put("displayName", tablesService.getTableTitleByName(tableName));
                    List<ColumnWithType> columns = reportBuilderService.getTableDescription(tableName, true);
                    ArrayNode columnsNode = ((ObjectNode) tableNode).withArray("children");
                    for (ColumnWithType col : columns) {
                        ObjectNode columnNode = new ObjectNode(JsonNodeFactory.instance);
                        columnNode.put("name", col.getColumn());
                        columnNode.put("displayName", col.getTitle());
                        columnNode.put("type", col.getType());
                        columnNode.put("requiresQuoting", col.isRequiresQuoting());
                        columnsNode.add(columnNode);
                    }
                }
            }
        }
    }

    @ApiOperation(value = "Описание таблицы")
    @GetMapping(value="/tables/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ColumnWithType> getTableDescription(@PathVariable(value = "name") String viewName) {
        return reportBuilderService.getTableDescription(viewName, false);
    }

}
