package com.dias.services.reports;

import com.dias.services.core.Details;
import com.dias.services.notifications.NotifificationsData;
import com.dias.services.notifications.interfaces.INotificationsService;
import com.dias.services.reports.dto.reports.ReportDTO;
import com.dias.services.reports.export.pdf.ReportPdfWriter;
import com.dias.services.reports.mocks.TestNotificationMessage;
import com.dias.services.reports.mocks.TestNotificationListener;
import com.dias.services.reports.mocks.TestNotificationService;
import com.dias.services.reports.model.Report;
import com.dias.services.reports.query.TotalValue;
import com.dias.services.reports.report.query.ResultSet;
import com.dias.services.reports.report.query.ResultSetWithTotal;
import com.dias.services.reports.service.ReportService;
import com.dias.services.reports.subsystem.ColumnWithType;
import com.dias.services.reports.translation.Translator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itextpdf.text.DocumentException;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.jfree.chart.JFreeChart;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ReportsControllerTest extends AbstractReportsModuleTest {

    private static final Integer USER_ID = 999;
    private static Integer createdReportId;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReportService reportService;

    @Autowired
    private Translator translator;

    @Autowired
    private INotificationsService notificationsService;

    private ModelMapper modelMapper = new ModelMapper();


    @Before
    public void setUp() throws IOException {
        super.setUp();
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        ReportService.updateModelMapper(objectMapper, modelMapper);
    }

    @Test
    public void order010createNewReport() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/reports/analytics/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(Util.readResource("ReportsController/report.json")))
                .andExpect(status().isCreated()).andReturn();
        Map resultMap = new JacksonJsonParser().parseMap(result.getResponse().getContentAsString());
        createdReportId = (Integer) resultMap.get("id");
        Assert.assertNotNull(createdReportId);
    }

    @Test
    public void order020updateReport() throws Exception {
        TestNotificationListener notificationListener = new TestNotificationListener();
        ((TestNotificationService)notificationsService).setNotificationListener(notificationListener);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/reports/analytics/reports/" + createdReportId)
                .header(Details.HEADER_USER_ID, USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(Util.readResource("ReportsController/updateReport.json")))
                .andExpect(status().isOk()).andReturn();
        Map resultMap = new JacksonJsonParser().parseMap(result.getResponse().getContentAsString());
        String updatedName = (String) resultMap.get("name");
        Assert.assertEquals("repo_new_2", updatedName);

        //проверим, пришло ли уведомление
        List<TestNotificationMessage> messages = notificationListener.getMessages();
        Assert.assertEquals(1, messages.size());

        //проверим что именно пришло
        TestNotificationMessage message = messages.get(0);
        Assert.assertEquals(NotifificationsData.REPORT_UPDATED.value(), message.getTypeId());
        Assert.assertEquals(Long.toString(createdReportId), message.getTargetId());
        Assert.assertEquals(USER_ID, message.getInitiatorId());
    }

    @Test
    public void order021updateReportToPrivate() throws Exception {
        TestNotificationListener notificationListener = new TestNotificationListener();
        ((TestNotificationService)notificationsService).setNotificationListener(notificationListener);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/reports/analytics/reports/" + createdReportId)
                .header(Details.HEADER_USER_ID, USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(Util.readResource("ReportsController/updateReportToPrivate.json")))
                .andExpect(status().isOk()).andReturn();
        Map resultMap = new JacksonJsonParser().parseMap(result.getResponse().getContentAsString());

        //проверим, пришло ли уведомление
        List<TestNotificationMessage> messages = notificationListener.getMessages();
        Assert.assertEquals(2, messages.size());

        //проверим что именно пришло
        TestNotificationMessage message = messages.get(1);
        Assert.assertEquals(NotifificationsData.REPORT_ADDED_PRIVATE.value(), message.getTypeId());
        Assert.assertEquals(Long.toString(createdReportId), message.getTargetId());
        Assert.assertEquals(USER_ID, message.getInitiatorId());
    }

    @Test
    public void order022updateReportToFavorite() throws Exception {
        TestNotificationListener notificationListener = new TestNotificationListener();
        ((TestNotificationService)notificationsService).setNotificationListener(notificationListener);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/reports/analytics/reports/" + createdReportId)
                .header(Details.HEADER_USER_ID, USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(Util.readResource("ReportsController/updateReportToFavorite.json")))
                .andExpect(status().isOk()).andReturn();
        Map resultMap = new JacksonJsonParser().parseMap(result.getResponse().getContentAsString());

        //проверим, пришло ли уведомление
        List<TestNotificationMessage> messages = notificationListener.getMessages();
        Assert.assertEquals(2, messages.size());

        //проверим что именно пришло
        TestNotificationMessage message = messages.get(1);
        Assert.assertEquals(NotifificationsData.REPORT_ADDED_FAVORITE.value(), message.getTypeId());
        Assert.assertEquals(Long.toString(createdReportId), message.getTargetId());
        Assert.assertEquals(USER_ID, message.getInitiatorId());
    }

    @Test
    public void order023updateReportToPublic() throws Exception {
        TestNotificationListener notificationListener = new TestNotificationListener();
        ((TestNotificationService)notificationsService).setNotificationListener(notificationListener);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/reports/analytics/reports/" + createdReportId)
                .header(Details.HEADER_USER_ID, USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(Util.readResource("ReportsController/updateReportToPublic.json")))
                .andExpect(status().isOk()).andReturn();
        Map resultMap = new JacksonJsonParser().parseMap(result.getResponse().getContentAsString());

        //проверим, пришло ли уведомление
        List<TestNotificationMessage> messages = notificationListener.getMessages();
        Assert.assertEquals(2, messages.size());

        //проверим что именно пришло
        TestNotificationMessage message = messages.get(1);
        Assert.assertEquals(NotifificationsData.REPORT_ADDED_PUBLIC.value(), message.getTypeId());
        Assert.assertEquals(Long.toString(createdReportId), message.getTargetId());
        Assert.assertEquals(USER_ID, message.getInitiatorId());
    }
    @Test
    public void order030getAllReports() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/reports/analytics/reports")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        List<Object> listResports = new JacksonJsonParser().parseList(result.getResponse().getContentAsString());
        Assert.assertEquals(1, listResports.size());
        Assert.assertEquals(createdReportId, ((Map)listResports.get(0)).get("id"));
    }

    @Test
    public void order040executeReport() throws Exception {

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/reports/analytics/reports/" + createdReportId + "/_execute")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        Map listResports = new JacksonJsonParser().parseMap(result.getResponse().getContentAsString());
        List<String> headers = (List<String>) listResports.get("headers");
        Assert.assertNotNull(headers);
        Assert.assertEquals(3, headers.size());
        Assert.assertNotNull(listResports.get("rows"));
    }


    @Test
    public void order045previewByDescriptor() throws Exception {


        MvcResult previewResult = mockMvc.perform(MockMvcRequestBuilders.post("/reports/analytics/reports/_preview")
                .content(getQueryDescriptorByReportId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        ResultSet data = objectMapper.readerFor(ResultSet.class).readValue(previewResult.getResponse().getContentAsString());
        List<List<Object>> rows = data.getRows();
        List<ColumnWithType> headers = data.getHeaders();
        Assert.assertEquals(1, headers.stream().filter(column -> column.getColumn().equalsIgnoreCase("history_values_by_day.abnormal_worktime")).count());
        Assert.assertEquals(1, headers.stream().filter(column -> column.getColumn().equalsIgnoreCase("history_values_by_day.heat_pipe")).count());
        Assert.assertEquals(1, headers.stream().filter(column -> column.getColumn().equalsIgnoreCase("day_statement.date_from")).count());

        Assert.assertNotNull(rows);
        Assert.assertEquals(1, rows.size());
        Assert.assertEquals(1794.94, rows.get(0).get(1));
    }

    private String getQueryDescriptorByReportId() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/reports/analytics/reports/" + createdReportId))
                .andExpect(status().isOk()).andReturn();

        JsonNode reportJson = objectMapper.readTree(result.getResponse().getContentAsString());
        return objectMapper.writeValueAsString(reportJson.get("queryDescriptor"));
    }

    @Test
    public void order046previewByDescriptorWithTotal() throws Exception {

        MvcResult previewResult = mockMvc.perform(MockMvcRequestBuilders.post("/reports/analytics/reports/_previewWithTotal")
                .content(getQueryDescriptorByReportId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        ResultSetWithTotal data = objectMapper.readerFor(ResultSetWithTotal.class).readValue(previewResult.getResponse().getContentAsString());
        List<List<Object>> rows = data.getRows();
        List<ColumnWithType> headers = data.getHeaders();
        List<TotalValue> totalRow = data.getTotal();

        Assert.assertEquals(1, headers.stream().filter(column -> column.getColumn().equalsIgnoreCase("history_values_by_day.abnormal_worktime")).count());
        Assert.assertEquals(1, headers.stream().filter(column -> column.getColumn().equalsIgnoreCase("history_values_by_day.heat_pipe")).count());
        Assert.assertEquals(1, headers.stream().filter(column -> column.getColumn().equalsIgnoreCase("day_statement.date_from")).count());

        TotalValue totalValue = totalRow.get(0);
        Assert.assertEquals(1794.94, totalValue.getValue());
        Assert.assertEquals("history_values_by_day.heat_pipe", totalValue.getColumn());

    }

    @Test
    public void order047executeWithTotal() throws Exception {

        MvcResult previewResult = mockMvc.perform(MockMvcRequestBuilders.post("/reports/analytics/reports/" + createdReportId + "/_executeWithTotal"))
                .andExpect(status().isOk()).andReturn();

        ResultSetWithTotal data = objectMapper.readerFor(ResultSetWithTotal.class).readValue(previewResult.getResponse().getContentAsString());
        List<ColumnWithType> headers = data.getHeaders();
        List<TotalValue> totalRow = data.getTotal();

        Assert.assertEquals(1, headers.stream().filter(column -> column.getColumn().equalsIgnoreCase("history_values_by_day.abnormal_worktime")).count());
        Assert.assertEquals(1, headers.stream().filter(column -> column.getColumn().equalsIgnoreCase("history_values_by_day.heat_pipe")).count());
        Assert.assertEquals(1, headers.stream().filter(column -> column.getColumn().equalsIgnoreCase("day_statement.date_from")).count());

        TotalValue totalValue = totalRow.get(0);
        Assert.assertEquals(1794.94, totalValue.getValue());
        Assert.assertEquals("history_values_by_day.heat_pipe", totalValue.getColumn());

    }

    @Test()
    public void order050exportToExcel() throws Exception {

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/reports/analytics/reports/" + createdReportId + "/_export?format=XLSX"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes));
        XSSFSheet sheet = wb.getSheetAt(0);
        String excelActual = sheet.getCTWorksheet().toString();
        Diff diff = XMLUnit.compareXML(Util.readResource("ReportsController/report_excel.txt"), excelActual);
        Assert.assertTrue(diff.similar());

    }

    @Test
    public void order060deleteReport() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.delete("/reports/analytics/reports/" + createdReportId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/reports/analytics/reports"))
                .andExpect(status().isOk()).andReturn();
        List<Object> resultList = new JacksonJsonParser().parseList(result.getResponse().getContentAsString());
        Assert.assertEquals(0, resultList.size());

    }

    @Test
    public void order070previewReportWithJoin() throws Exception {

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/reports/analytics/reports/_preview")
                .content(Util.readResource("ReportsController/descriptor_join.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

    }

    @Test
    public void order080createNewPieChart() throws Exception {
        doExportAndCheck("ReportsController/report_pie.json", "Pie", "ReportsController/report_pie.txt", "ReportsController/pdf_report_pie.json");
    }

    @Test
    public void order090createNewLineChart() throws Exception {
        doExportAndCheck("ReportsController/report_linear_date.json", "Linear x-date", "ReportsController/report_linear_date.txt", "ReportsController/pdf_report_linear_date.json");
    }

    @Test
    public void order100createNewLineChartXNum() throws Exception {
        doExportAndCheck("ReportsController/report_linear_num.json", "Linear x-num", "ReportsController/report_linear_num.txt", "ReportsController/pdf_report_linear_num.json");
    }

    @Test
    public void order110createNewBarChartXStr() throws Exception {
        doExportAndCheck("ReportsController/report_bar_str.json", "Bar x-str", "ReportsController/report_bar_str.txt", "ReportsController/pdf_report_bar_str.json");
    }

    @Test
    public void order120createNewBarChartXNum() throws Exception {
        doExportAndCheck("ReportsController/report_bar_num.json", "Bar x-num", "ReportsController/report_bar_num.txt", "ReportsController/pdf_report_bar_num.json");
    }

    @Test
    public void order130createNewBarChartXDate() throws Exception {
        doExportAndCheck("ReportsController/report_bar_date.json", "Bar x-date", "ReportsController/report_bar_date.txt", "ReportsController/pdf_report_bar_date.json");
    }

    @Test
    public void order140createNewHBarChartXStr() throws Exception {
        doExportAndCheck("ReportsController/report_hbar_str.json", "HBar x-str", "ReportsController/report_hbar_str.txt", "ReportsController/pdf_report_hbar_str.json");
    }

    @Test
    public void order150createNewHBarChartXNum() throws Exception {
        doExportAndCheck("ReportsController/report_hbar_num.json", "HBar x-num", "ReportsController/report_hbar_num.txt", "ReportsController/pdf_report_hbar_num.json");
    }

    @Test
    public void order160createNewScatterChartXDate() throws Exception {
        doExportAndCheck("ReportsController/report_scatter_date.json", "Scatter x-date", "ReportsController/report_scatter_date.txt", "ReportsController/pdf_report_scatter_date.json");
    }

    @Test
    public void order170createNewScatterChartXStr() throws Exception {
        doExportAndCheck("ReportsController/report_scatter_str.json", "Scatter x-str", "ReportsController/report_scatter_str.txt", "ReportsController/pdf_report_scatter_str.json");
    }

    @Test
    public void order180createNewScatterChartXNum() throws Exception {
        doExportAndCheck("ReportsController/report_scatter_num.json", "Scatter x-num", "ReportsController/report_scatter_num.txt", "ReportsController/pdf_report_scatter_num.json");
    }

    @Test
    public void order190createNewCascadeChartXStr() throws Exception {
        doExportAndCheck("ReportsController/report_cascade_str.json", "Cascade x-str", "ReportsController/report_cascade_str.txt", "ReportsController/pdf_report_cascade_str.json");
    }

    @Test
    public void order200createNewCascadeChartXDate() throws Exception {
        doExportAndCheck("ReportsController/report_cascade_date.json", "Cascade x-date", "ReportsController/report_cascade_date.txt", "ReportsController/pdf_report_cascade_date.json");
    }

    @Test
    public void order210createNewCascadeChartXNum() throws Exception {
        doExportAndCheck("ReportsController/report_cascade_num.json", "Cascade x-num", "ReportsController/report_cascade_num.txt", "ReportsController/pdf_report_cascade_num.json");
    }

    @Test
    public void order220createNewComboChartXStr() throws Exception {
        doExportAndCheck("ReportsController/report_combo_str.json", "Combo x-str", "ReportsController/report_combo_str.txt", "ReportsController/pdf_report_combo_str.json");
    }

    @Test
    public void order230createNewComboChartXDate() throws Exception {
        doExportAndCheck("ReportsController/report_combo_date.json", "Combo x-date", "ReportsController/report_combo_date.txt", "ReportsController/pdf_report_combo_date.json");
    }

    @Test
    public void order240createNewComboChartXNum() throws Exception {
        doExportAndCheck("ReportsController/report_combo_num.json", "Combo x-num", "ReportsController/report_combo_num.txt", "ReportsController/pdf_report_combo_num.json");
    }

    private void doExportAndCheck(String reportPath, String reportName, String reportExpectedResultPath, String pdfReportExpectedResultPath) throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/reports/analytics/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(Util.readResource(reportPath)))
                .andExpect(status().isCreated()).andReturn();
        Map resultMap = new JacksonJsonParser().parseMap(result.getResponse().getContentAsString());
        Integer reportId = (Integer) resultMap.get("id");

        exportToExcelAndCompare(reportId, reportName, reportExpectedResultPath);
        exportToPdfAndCompare(reportId, pdfReportExpectedResultPath);

    }

    private void exportToExcelAndCompare(Integer reportId, String reportName, String reportExpectedResultPath) throws Exception {

        TestNotificationListener notificationListener = new TestNotificationListener();
        ((TestNotificationService)notificationsService).setNotificationListener(notificationListener);

        MvcResult result;
        result = mockMvc.perform(MockMvcRequestBuilders.post("/reports/analytics/reports/" + reportId + "/_export")
                .header(Details.HEADER_USER_ID, USER_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));
        XSSFSheet sheet = workbook.getSheet(reportName);
        XSSFDrawing drawing = sheet.getDrawingPatriarch();
        List<XSSFChart> charts = drawing.getCharts();

        Assert.assertEquals(1, charts.size());

        String actualChartData = charts.get(0).getCTChart().toString();
        Diff diff = XMLUnit.compareXML(Util.readResource(reportExpectedResultPath), actualChartData);
        Assert.assertTrue(diff.similar());

        // на экспорт отчета должно создаваться только одно уведомление
        List<TestNotificationMessage> messages = notificationListener.getMessages();
        Assert.assertEquals(1, messages.size());
        TestNotificationMessage message = messages.get(0);
        Assert.assertEquals(NotifificationsData.REPORT_EXPORTED.value(), message.getTypeId());
        Assert.assertEquals(Long.toString(reportId), message.getTargetId());
        Assert.assertEquals(USER_ID, message.getInitiatorId());

    }

    private void exportToPdfAndCompare(Integer createdPieChartId, String reportExpectedResultPath) throws com.dias.services.reports.exception.ObjectNotFoundException, DocumentException, IOException {
        Report report = reportService.getById(createdPieChartId.longValue());
        ReportDTO reportDTO = convertToDTO(report);
        ResultSetWithTotal rs = reportService.syncExecuteWithTotalReport(reportDTO.getQueryDescriptor(), null, null);
        JFreeChart chart = new ReportPdfWriter(reportService, translator, "").writePdf(reportDTO, rs, new ByteArrayOutputStream()).getLeft();

        ObjectNode node = objectMapper.createObjectNode();
        serializeChartObject(chart, node);
        Assert.assertTrue(node.equals(objectMapper.readTree(Util.readResource(reportExpectedResultPath))));

    }

    private ReportDTO convertToDTO(Report report) {
        return modelMapper.map(report, ReportDTO.class);
    }

    private void serializeChartObject(Object o, ObjectNode objectNode) {
        Field[] fields = o.getClass().getDeclaredFields();
        for (Field field: fields) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                Class<?> type = field.getType();
                field.setAccessible(true);
                if (isSimpleField(type)) {
                    try {
                        objectNode.put(field.getName(), Objects.toString(field.get(o)));
                    } catch (final IllegalAccessException ignore) {
                    }

                } else if (Collection.class.isAssignableFrom(type)) {

                    try {
                        Collection fieldValue = (Collection) field.get(o);
                        if (fieldValue != null) {
                            ArrayNode collection = objectNode.putArray(field.getName());
                            for (Object child : fieldValue) {
                                if (isSimpleField(child.getClass())) {
                                    collection.add(Objects.toString(child));
                                } else {
                                    ObjectNode childObject = collection.addObject();
                                    serializeChartObject(child, childObject);
                                }
                            }
                        }
                    } catch (IllegalAccessException ignore) {
                    }

                } else if (type.getName().contains("org.jfree")
                        && !"org.jfree.data.xy.IntervalXYDelegate".equals(type.getName())) { // игнорируем во избежание stackOverflowException

                    try {
                        Object fieldValue = field.get(o);
                        if (fieldValue != null) {
                            ObjectNode refObject = objectNode.putObject(field.getName());
                            serializeChartObject(fieldValue, refObject);
                        }
                    } catch (IllegalAccessException ignore) {
                    }

                }
            }
        }
    }

    private boolean isSimpleField(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || Number.class.isAssignableFrom(type)
                || type.isEnum()
                || Boolean.class.isAssignableFrom(type)
                || Color.class.isAssignableFrom(type)
                || Paint.class.isAssignableFrom(type);
    }

}
