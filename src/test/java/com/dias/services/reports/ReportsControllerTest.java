package com.dias.services.reports;

import com.dias.services.reports.query.TotalValue;
import com.dias.services.reports.report.query.ResultSet;
import com.dias.services.reports.report.query.ResultSetWithTotal;
import com.dias.services.reports.subsystem.ColumnWithType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xssf.usermodel.*;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ReportsControllerTest extends AbstractReportsModuleTest {

    private static Integer createdReportId;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws IOException {
        super.setUp();
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
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/reports/analytics/reports/" + createdReportId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(Util.readResource("ReportsController/updateReport.json")))
                .andExpect(status().isOk()).andReturn();
        Map resultMap = new JacksonJsonParser().parseMap(result.getResponse().getContentAsString());
        String updatedName = (String) resultMap.get("name");
        Assert.assertEquals("repo_new_2", updatedName);
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
        checkExportExcelResult("ReportsController/report_pie.json", "Pie", "ReportsController/report_pie.txt");
    }

    @Test
    public void order090createNewLineChart() throws Exception {
        checkExportExcelResult("ReportsController/report_linear_date.json", "Linear x-date", "ReportsController/report_linear_date.txt");
    }

    @Test
    public void order100createNewLineChartXNum() throws Exception {
        checkExportExcelResult("ReportsController/report_linear_num.json", "Linear x-num", "ReportsController/report_linear_num.txt");
    }

    @Test
    public void order110createNewBarChartXStr() throws Exception {
        checkExportExcelResult("ReportsController/report_bar_str.json", "Bar x-str", "ReportsController/report_bar_str.txt");
    }

    @Test
    public void order120createNewBarChartXNum() throws Exception {
        checkExportExcelResult("ReportsController/report_bar_num.json", "Bar x-num", "ReportsController/report_bar_num.txt");
    }

    @Test
    public void order130createNewBarChartXDate() throws Exception {
        checkExportExcelResult("ReportsController/report_bar_date.json", "Bar x-date", "ReportsController/report_bar_date.txt");
    }

    @Test
    public void order140createNewHBarChartXStr() throws Exception {
        checkExportExcelResult("ReportsController/report_hbar_str.json", "HBar x-str", "ReportsController/report_hbar_str.txt");
    }

    @Test
    public void order150createNewHBarChartXNum() throws Exception {
        checkExportExcelResult("ReportsController/report_hbar_num.json", "HBar x-num", "ReportsController/report_hbar_num.txt");
    }

    @Test
    public void order160createNewScatterChartXDate() throws Exception {
        checkExportExcelResult("ReportsController/report_scatter_date.json", "Scatter x-date", "ReportsController/report_scatter_date.txt");
    }

    @Test
    public void order170createNewScatterChartXStr() throws Exception {
        checkExportExcelResult("ReportsController/report_scatter_str.json", "Scatter x-str", "ReportsController/report_scatter_str.txt");
    }

    @Test
    public void order180createNewScatterChartXNum() throws Exception {
        checkExportExcelResult("ReportsController/report_scatter_num.json", "Scatter x-num", "ReportsController/report_scatter_num.txt");
    }

    @Test
    public void order190createNewCascadeChartXStr() throws Exception {
        checkExportExcelResult("ReportsController/report_cascade_str.json", "Cascade x-str", "ReportsController/report_cascade_str.txt");
    }

    @Test
    public void order200createNewCascadeChartXDate() throws Exception {
        checkExportExcelResult("ReportsController/report_cascade_date.json", "Cascade x-date", "ReportsController/report_cascade_date.txt");
    }

    @Test
    public void order210createNewCascadeChartXNum() throws Exception {
        checkExportExcelResult("ReportsController/report_cascade_num.json", "Cascade x-num", "ReportsController/report_cascade_num.txt");
    }

    @Test
    public void order220createNewComboChartXStr() throws Exception {
        checkExportExcelResult("ReportsController/report_combo_str.json", "Combo x-str", "ReportsController/report_combo_str.txt");
    }

    @Test
    public void order230createNewComboChartXDate() throws Exception {
        checkExportExcelResult("ReportsController/report_combo_date.json", "Combo x-date", "ReportsController/report_combo_date.txt");
    }

    @Test
    public void order240createNewComboChartXNum() throws Exception {
        checkExportExcelResult("ReportsController/report_combo_num.json", "Combo x-num", "ReportsController/report_combo_num.txt");
    }

    private void checkExportExcelResult(String reportPath, String reportName, String reportExpectedResultPath) throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/reports/analytics/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(Util.readResource(reportPath)))
                .andExpect(status().isCreated()).andReturn();
        Map resultMap = new JacksonJsonParser().parseMap(result.getResponse().getContentAsString());
        Integer createdPieChartId = (Integer) resultMap.get("id");

        result = mockMvc.perform(MockMvcRequestBuilders.post("/reports/analytics/reports/" + createdPieChartId + "/_export")
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

    }

}
