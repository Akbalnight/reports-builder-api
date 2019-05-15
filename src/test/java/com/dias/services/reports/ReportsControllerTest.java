package com.dias.services.reports;

import com.dias.services.reports.query.TotalValue;
import com.dias.services.reports.report.query.ResultSet;
import com.dias.services.reports.report.query.ResultSetWithTotal;
import com.dias.services.reports.subsystem.ColumnWithType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.*;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChart;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTChartsheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDrawing;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorksheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.ChartsheetDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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
    @Ignore
    public void order050exportToExcel() throws Exception {

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/reports/analytics/reports/" + createdReportId + "/_export?format=XLSX"))
                .andExpect(status().isOk()).andReturn();

        String expected = Util.readResource("ReportsController/excelRows.txt");
        String[] expextedRows = expected.split("\n");

        byte[] bytes = result.getResponse().getContentAsByteArray();
        XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes));
        XSSFSheet sheet = wb.getSheetAt(0);
        int index = 0;
        XSSFRow r;
        while ((r = sheet.getRow(index)) != null) {
            Iterator<Cell> cellIterator = r.cellIterator();
            List<Object> row = new ArrayList<>();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                if (cell.getCellType() == CellType.STRING) {
                    row.add(cell.getStringCellValue());
                } else if (cell.getCellType() == CellType.NUMERIC) {
                    row.add(cell.getNumericCellValue());
                }

            }
            String expextedRow = expextedRows[index];
            String[] expectedCells = !expextedRow.isEmpty() ? expextedRow.split(",") : new String[]{};
            Assert.assertEquals(expectedCells.length, row.size());
            if (expectedCells.length > 0) {
                for (int i = 0; i < expectedCells.length; i++) {
                    Assert.assertEquals(expectedCells[i].trim(), row.get(i).toString().trim());
                }
            }
            index++;
        }
        Assert.assertTrue(index > 0);
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
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/reports/analytics/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(Util.readResource("ReportsController/report_pie.json")))
                .andExpect(status().isCreated()).andReturn();
        Map resultMap = new JacksonJsonParser().parseMap(result.getResponse().getContentAsString());
        Integer createdPieChartId = (Integer) resultMap.get("id");

        result = mockMvc.perform(MockMvcRequestBuilders.post("/reports/analytics/reports/" + createdPieChartId + "/_export")
                .contentType(MediaType.APPLICATION_JSON)
                .content(Util.readResource("ReportsController/report_pie.json")))
                .andExpect(status().isOk()).andReturn();

        XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));
        XSSFSheet sheet = workbook.getSheet("Pie");
        XSSFDrawing drawing = sheet.getDrawingPatriarch();
        List<XSSFChart> charts = drawing.getCharts();

        Assert.assertEquals(1, charts.size());

        String actualChartData = charts.get(0).getCTChart().toString();
        Diff diff = XMLUnit.compareXML(Util.readResource("ReportsController/report_pie.txt"), actualChartData);
        Assert.assertTrue(diff.similar());

    }


}
