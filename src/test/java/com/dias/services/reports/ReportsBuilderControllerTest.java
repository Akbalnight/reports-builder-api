package com.dias.services.reports;

import com.dias.services.reports.subsystem.TablesService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ReportsBuilderControllerTest extends AbstractReportsModuleTest {

    private static final String HISTORY_DAY_DATA_TABLE_NAME_TITLE = "Данные по дневным ведомостям";
    private static final String HISTORY_DAY_DATA_ROW_TABLE_NAME = "history_day_data";
    private static final String ID_TITLE = "Идентификатор";

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TablesService tablesService;

    @Before
    public void setUp() throws IOException {
        super.setUp();
    }

    @Test
    public void getSubSystemsTest() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/analytics/subsystems"))
                .andExpect(status().isOk()).andReturn();
        JsonNode resultJson = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode historyDayData = resultJson.get("Коммерческий учет").get("Суточные ведомости").get(HISTORY_DAY_DATA_TABLE_NAME_TITLE);
        String rowTableName = tablesService.getTableNameByTitle(HISTORY_DAY_DATA_TABLE_NAME_TITLE);
        Assert.assertEquals(HISTORY_DAY_DATA_ROW_TABLE_NAME, rowTableName);
        Assert.assertTrue(historyDayData.size() > 0);
        JsonNode idNode = null;
        for (int i = 0; i < historyDayData.size(); i++) {
            JsonNode columnNode = historyDayData.get(i);
            if (columnNode.get("column").asText().equals("id")) {
                idNode = columnNode;
                break;
            }
        }
        Assert.assertNotNull(idNode);
        Assert.assertEquals(ID_TITLE, idNode.get("title").asText());


    }


}
