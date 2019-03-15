package com.dias.services.reports.service;

import com.dias.services.reports.dto.reports.ReportDTO;
import com.dias.services.reports.exception.ObjectNotFoundException;
import com.dias.services.reports.exception.ReportsException;
import com.dias.services.reports.export.ReportExcelWriter;
import com.dias.services.reports.export.ReportPdfWriter;
import com.dias.services.reports.model.Report;
import com.dias.services.reports.query.NoGroupByQueryBuilder;
import com.dias.services.reports.query.TotalValue;
import com.dias.services.reports.report.chart.ChartDescriptor;
import com.dias.services.reports.report.query.*;
import com.dias.services.reports.repository.ReportRepository;
import com.dias.services.reports.subsystem.ColumnWithType;
import com.dias.services.reports.subsystem.TablesService;
import com.dias.services.reports.translation.Translator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.DocumentException;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.ResultSetMetaData;
import java.util.*;
import java.util.logging.Logger;

@Service
public class ReportService extends AbstractService<Report> {

    private static final String PROPERTY_VALUE_AXIS = "valueAxis";
    private static final String PROPERTY_KEY = "key";
    private static final String PROPERTY_NAME = "name";
    private static final String PROPERTY_COLOR = "color";
    private static final String PROPERTY_ROWS = "rows";
    private static final String PROPERTY_FROM = "from";
    private static final String PROPERTY_TO = "to";
    private static final String PROPERTY_NAMES = "names";
    private static final String PROPERTY_CHART = "chart";
    private static final String PROPERTY_X_AXIS = "xAxis";
    private static final String PROPERTY_Y_AXIS = "yAxis";
    private static final String PROPERTY_GENERAL = "general";
    private static final String PROPERTY_SHOW_LEGEND = "showLegend";
    private static final String PROPERTY_DATA_AXIS = "dataAxis";
    public static final String PROPERTY_CALCULATED_X_RANGE = "calculatedXRange";
    public static final String PROPERTY_CALCULATED_Y_RANGE = "calculatedYRange";
    public static final String PROPERTY_SHOW_DOT_VALUES = "showDotValues";

    private static Logger LOG = Logger.getLogger(ReportService.class.getName());

    private final ReportRepository reportRepository;

    private final NamedParameterJdbcTemplate template;

    private final ReportBuilderService reportBuilderService;
    private final Translator translator;
    private final TablesService tablesService;
    private final ObjectMapper objectMapper;
    private ModelMapper modelMapper = new ModelMapper();

    @Value("${com.dias.services.reports.preview.limit:100}")
    private Long previewRecordsLimit;

    @Value("${com.dias.services.reports.null.symbol:-}")
    private String nullSymbol;

    @Autowired
    public ReportService(ReportRepository reportRepository, ReportBuilderService reportBuilderService, Translator translator, TablesService tablesService, ObjectMapper objMapper) {
        this.reportRepository = reportRepository;
        this.reportBuilderService = reportBuilderService;
        this.template = reportRepository.getTemplate();
        this.translator = translator;
        this.tablesService = tablesService;
        this.objectMapper = objMapper;

        Converter<String, QueryDescriptor> stringToQryDescriptorConverter = mappingContext -> {
            try {
                String source = mappingContext.getSource();
                if (source != null) {
                    return objectMapper.readerFor(QueryDescriptor.class).readValue(source);
                }
            } catch (IOException ignore) {
            }
            return null;
        };
        Converter<String, JsonNode> stringToJsonNodeConverter = mappingContext -> {
            try {
                String source = mappingContext.getSource();
                if (source != null) {
                    return objectMapper.readTree(source);
                }
            } catch (IOException ignore) {
            }
            return null;
        };

        Converter<QueryDescriptor, String> qryDescriptorToStringConverter = mappingContext -> {
            try {

                QueryDescriptor source = mappingContext.getSource();
                if (source != null) {
                    return objectMapper.writerFor(QueryDescriptor.class).writeValueAsString(source);
                }
            } catch (IOException ignore) {
            }
            return null;
        };
        Converter<JsonNode, String> jsonNodeToStringConverter = mappingContext -> {
            try {
                JsonNode source = mappingContext.getSource();
                if (source != null) {
                    return objectMapper.writeValueAsString(source);
                }
            } catch (IOException ignore) {
            }
            return null;
        };


        PropertyMap<Report, ReportDTO> stringToQryDescriptorMappings = new PropertyMap<Report, ReportDTO>() {
            protected void configure() {
                using(stringToQryDescriptorConverter).map(source.getQueryDescriptor()).setQueryDescriptor(null);
                using(stringToJsonNodeConverter).map(source.getDescription()).setDescription(null);
            }
        };
        PropertyMap<ReportDTO, Report> mapToStringMappings = new PropertyMap<ReportDTO, Report>() {
            protected void configure() {
                using(qryDescriptorToStringConverter).map(source.getQueryDescriptor()).setQueryDescriptor(null);
                using(jsonNodeToStringConverter).map(source.getDescription()).setDescription(null);
            }
        };


        modelMapper.addMappings(stringToQryDescriptorMappings);
        modelMapper.addMappings(mapToStringMappings);

    }

    public ReportDTO getReportById(Long id) throws ObjectNotFoundException {
        Report report = getById(id);
        return convertToDTO(report);
    }

    private ReportDTO convertToDTO(Report report) {
        return modelMapper.map(report, ReportDTO.class);
    }

    private Report convertToBO(ReportDTO originalReport) {
        return modelMapper.map(originalReport, Report.class);
    }

    protected ReportRepository getRepository() {
        return reportRepository;
    }

    public ResultSet syncExecuteReport(QueryDescriptor queryDescriptor, Long limit, Long offset) {

        Map<String, Map<String, ColumnWithType>> columnTypesMap = getTablesColumnTypesMap(queryDescriptor);
        String result = new NoGroupByQueryBuilder(queryDescriptor, tablesService)
                .withRowLimit(limit)
                .withOffset(offset)
                .withColumns(columnTypesMap)
                .buildSelectQuery();

        LOG.info("Execute query: " + result);

        return template.query(result, getResultSetResultSetExtractor(queryDescriptor, columnTypesMap));
    }

    public ResultSet previewByDescriptor(QueryDescriptor queryDescriptor) {
        Map<String, Map<String, ColumnWithType>> columnTypesMap = getTablesColumnTypesMap(queryDescriptor);
        String result = new NoGroupByQueryBuilder(queryDescriptor, tablesService)
                .withRowLimit(previewRecordsLimit)
                .withColumns(columnTypesMap)
                .buildSelectQuery();
        return template.query(result, getResultSetResultSetExtractor(queryDescriptor, columnTypesMap));
    }

    private ResultSetExtractor<ResultSet> getResultSetResultSetExtractor(QueryDescriptor queryDescriptor, Map<String, Map<String, ColumnWithType>> columnTypesMap) {
        return rs -> {
            ResultSet rows = new ResultSet();
            List<ColumnWithType> headers = new ArrayList<>();
            Select[] select = queryDescriptor.getSelect();

            for (int i = 0; i < select.length; i++) {
                ColumnWithType nonTranslatedColumnWithType = columnWithTypeByColumn(select[i].getColumn(), columnTypesMap);
                ColumnWithType columnWithType = ColumnWithType.builder()
                        .column(tablesService.toRussianTableAndColumn(select[i].getColumn()))
                        .title(select[i].getTitle())
                        .type(nonTranslatedColumnWithType.getType())
                        .build();
                headers.add(columnWithType);
            }
            rows.setHeaders(headers);
            ResultSetMetaData metadata = rs.getMetaData();
            int columnCount = metadata.getColumnCount();
            List<List<Object>> listRows = new ArrayList<>();
            while (rs.next()) {
                List<Object> listColumns = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    listColumns.add(rs.getObject(i));
                }
                listRows.add(listColumns);
            }
            rows.setRows(listRows);
            return rows;
        };
    }

    @Async
    public void asyncExecuteReport(Report report) {

    }

    public void merge(ReportDTO originalReport, ReportDTO report) throws Exception {
        Optional.ofNullable(report).ifPresent ((ReportDTO updates) -> {
            Optional.ofNullable(updates.getName()).ifPresent(originalReport::setName);
            Optional.ofNullable(updates.getTitle()).ifPresent(originalReport::setTitle);
            Optional.ofNullable(updates.getType()).ifPresent(originalReport::setType);
            Optional.ofNullable(updates.getDescription()).ifPresent(originalReport::setDescription);
            Optional.ofNullable(updates.getIsFavorite()).ifPresent(originalReport::setIsFavorite);
            Optional.ofNullable(updates.getIsPublic()).ifPresent(originalReport::setIsPublic);
            Optional.ofNullable(updates.getQueryDescriptor()).ifPresent(originalReport::setQueryDescriptor);
        });
        Report model = convertToBO(originalReport);
        getRepository().update(model);
    }

    public ResultSetWithTotal previewWithTotalByDescriptor(QueryDescriptor descriptor) {

        ResultSet resultSet = previewByDescriptor(descriptor);

        String result = new NoGroupByQueryBuilder(descriptor, tablesService)
                .withColumns(getTablesColumnTypesMap(descriptor))
                .buildSummaryQuery();

        List<TotalValue> total = template.query(result, getTotalExtractor(descriptor));

        return new ResultSetWithTotal(resultSet.getRows(), resultSet.getHeaders(), total, null);
    }

    private ResultSetExtractor<List<TotalValue>> getTotalExtractor(QueryDescriptor queryDescriptor) {
        return rs -> {
            Calculation[] aggregations = queryDescriptor.getAggregations();
            ResultSetMetaData metadata = rs.getMetaData();
            int columnCount = metadata.getColumnCount();
            List<TotalValue> totalRecord = new ArrayList<>();
            rs.next();
            for (int i = 1; i <= columnCount; i++) {
                totalRecord.add(new TotalValue(tablesService.toRussianTableAndColumn(aggregations[i - 1].getColumn()), rs.getObject(i)));
            }
            return totalRecord;
        };
    }

    public ResultSetWithTotal syncExecuteWithTotalReport(QueryDescriptor queryDescriptor, Long limit, Long offset) {

        ResultSet resultSet = syncExecuteReport(queryDescriptor, limit, offset);

        String result =
                new NoGroupByQueryBuilder(queryDescriptor, tablesService)
                        .withColumns(getTablesColumnTypesMap(queryDescriptor))
                        .buildSummaryQuery();

        List<TotalValue> total = template.query(result, getTotalExtractor(queryDescriptor));

        return new ResultSetWithTotal(resultSet.getRows(), resultSet.getHeaders(), total, null);
    }

    public void exportToExcel(Report report, ByteArrayOutputStream out) throws IOException {
        ReportDTO reportDTO = convertToDTO(report);
        ResultSetWithTotal rs = syncExecuteWithTotalReport(reportDTO.getQueryDescriptor(), null, null);
        new ReportExcelWriter(this, translator, nullSymbol).writeExcel(reportDTO, rs, out);
    }

    public void exportToPdf(Report report, ByteArrayOutputStream out) throws DocumentException, IOException {
        ReportDTO reportDTO = convertToDTO(report);
        ResultSetWithTotal rs = syncExecuteWithTotalReport(reportDTO.getQueryDescriptor(), null, null);
        if (reportDTO.getQueryDescriptor().getGroupBy() != null) {
            rs = rs.convertToGroupped(reportDTO.getQueryDescriptor().getGroupBy(), reportDTO.getQueryDescriptor().getOrderBy());
        }
        new ReportPdfWriter(this, translator, nullSymbol).writePdf(reportDTO, rs, out);
    }

    public ResultSet executeReport(Report report, Long limit, Long offset) {
        ReportDTO reportDTO = convertToDTO(report);
        return syncExecuteReport(reportDTO.getQueryDescriptor(), limit, offset);
    }

    public ResultSetWithTotal executeWithTotalReport(Report report, Long limit, Long offset) {
        ReportDTO reportDTO = convertToDTO(report);
        return syncExecuteWithTotalReport(reportDTO.getQueryDescriptor(), limit, offset);
    }

    public List<ReportDTO> getAllReports() {
        List<Report> reports = reportRepository.getAll();
        return modelMapper.map(reports, new TypeToken<ArrayList<ReportDTO>>() {}.getType());
    }

    public void createReport(ReportDTO reportDTO) {
        Report report = convertToBO(reportDTO);
        create(report);
        reportDTO.setId(report.getId());
    }

    /**
     *
     * Получаем сопоставление имя таблицы -> список колонок с типами по
     * описанию запроса
     *
     * @param descriptor Описание запроса
     * @return Сопоставление имя таблицы -> список колонок с типами
     */
    public Map<String, List<ColumnWithType>> getColumnTypesMap(QueryDescriptor descriptor) {
        Map<String, List<ColumnWithType>> result = new HashMap<>();
        Set<String> tableNames = tablesService.extractTableNames(descriptor);
        for(String tableName: tableNames) {
            result.put(tableName, reportBuilderService.getTableDescription(tableName));
        }
        return result;
    }

    public ChartDescriptor extractChartDescriptor(ReportDTO reportDTO) throws IOException {
        JsonNode description = reportDTO.getDescription();
        if (description == null) {
            throw new ReportsException(ReportsException.WRONG_DIAGRAMM_FORMAT, reportDTO.getId(), PROPERTY_VALUE_AXIS);
        }
        ChartDescriptor descriptor = new ChartDescriptor();
        JsonNode seriesNode = description.get(PROPERTY_VALUE_AXIS);
        if (seriesNode == null) {
            throw new ReportsException(ReportsException.WRONG_DIAGRAMM_FORMAT, reportDTO.getId(), PROPERTY_VALUE_AXIS);
        }
        List<ChartDescriptor.Series> seriesList = new ArrayList<>();
        descriptor.setSeries(seriesList);
        int wrongSeriesCount = 0;
        for (int i = 0; i < seriesNode.size(); i++) {
            JsonNode sNode = seriesNode.get(i);
            ChartDescriptor.Series series = new ChartDescriptor.Series();
            JsonNode columnKeyNode = sNode.get(PROPERTY_KEY);
            if (columnKeyNode != null) {
                series.setValueColumn(columnKeyNode.asText());
                JsonNode seriesColorNode = sNode.get(PROPERTY_COLOR);
                if (seriesColorNode != null) {
                    series.setColor(seriesColorNode.asText());
                }
                JsonNode rowsRangeNode = sNode.get(PROPERTY_ROWS);
                if (rowsRangeNode != null && rowsRangeNode.get(PROPERTY_FROM) != null) {
                    series.setStartRow(rowsRangeNode.get(PROPERTY_FROM).asInt());
                }
                if (rowsRangeNode != null && rowsRangeNode.get(PROPERTY_TO) != null) {
                    series.setEndRow(rowsRangeNode.get(PROPERTY_TO).asInt());
                }
                String title = sNode.get(PROPERTY_NAME) != null ? sNode.get(PROPERTY_NAME).asText() : columnKeyNode.asText();
                series.setTitle(title);
                seriesList.add(series);
            } else {
                wrongSeriesCount++;
            }
        }
        if (wrongSeriesCount > 0) {
            ReportsException reportsException = new ReportsException(ReportsException.WRONG_DIAGRAMM_SERIES_FORMAT, reportDTO.getId(), PROPERTY_KEY);
            if (wrongSeriesCount == seriesNode.size()) {
                throw reportsException;
            } else {
                //есть что вывести, поэтому запишем ошибку в лог
                LOG.severe(reportsException.getMessage());
            }
        }

        JsonNode namesNode = description.get(PROPERTY_NAMES);
        if (!StringUtils.isEmpty(namesNode.get(PROPERTY_CHART).asText())) {
            descriptor.setTitle(namesNode.get(PROPERTY_CHART).asText());
        }
        if (!StringUtils.isEmpty(namesNode.get(PROPERTY_X_AXIS).asText())) {
            descriptor.setAxisXTitle(namesNode.get(PROPERTY_X_AXIS).asText());
        }
        if (!StringUtils.isEmpty(namesNode.get(PROPERTY_Y_AXIS).asText())) {
            descriptor.setAxisYTitle(namesNode.get(PROPERTY_Y_AXIS).asText());
        }
        JsonNode generalNode = description.get(PROPERTY_GENERAL);
        if (generalNode != null) {
            descriptor.setShowLegend(generalNode.get(PROPERTY_SHOW_LEGEND) != null && generalNode.get(PROPERTY_SHOW_LEGEND).asBoolean());
            descriptor.setCalculatedXRange(generalNode.get(PROPERTY_CALCULATED_X_RANGE) != null && generalNode.get(PROPERTY_CALCULATED_X_RANGE).asBoolean());
            descriptor.setCalculatedYRange(generalNode.get(PROPERTY_CALCULATED_Y_RANGE) != null && generalNode.get(PROPERTY_CALCULATED_Y_RANGE).asBoolean());
            descriptor.setShowDotValues(generalNode.get(PROPERTY_SHOW_DOT_VALUES) != null && generalNode.get(PROPERTY_SHOW_DOT_VALUES).asBoolean());
        }
        

        JsonNode categoriesNode = description.get(PROPERTY_DATA_AXIS);
        descriptor.setAxisXColumn(categoriesNode.get(PROPERTY_KEY).asText());

        return descriptor;
    }

    /**
     *
     * Получаем сопоставление имя таблицы -> список колонок с типами по
     * описанию запроса
     *
     * @param descriptor Описание запроса
     * @return Сопоставление имя таблицы -> Сопоставление колонка-колонка с типами
     */
    public Map<String, Map<String, ColumnWithType>> getTablesColumnTypesMap(QueryDescriptor descriptor) {
        Map<String, Map<String, ColumnWithType>> result = new HashMap<>();
        Set<String> tableNames = tablesService.extractTableNames(descriptor);
        for(String tableName: tableNames) {
            result.put(tableName, reportBuilderService.getTableColumnsDescription(tableName));
        }
        return result;
    }

    private ColumnWithType columnWithTypeByColumn(String column, Map<String, Map<String, ColumnWithType>> columnWithTypes) {

        String table = null;
        if (column.contains(".")) {
            String[] parts = column.split("\\.");
            table = parts[0];
            column = parts[1];
        }
        Map<String, ColumnWithType> columnsToSearchIn;
        if (table != null) {
            columnsToSearchIn = columnWithTypes.get(table);
        } else {
            columnsToSearchIn = new HashMap<>();
            Map<String, ColumnWithType> finalColumnsToSearchIn = columnsToSearchIn;
            columnWithTypes.values().forEach(finalColumnsToSearchIn::putAll);
        }

        return columnsToSearchIn.get(column);
    }


}
