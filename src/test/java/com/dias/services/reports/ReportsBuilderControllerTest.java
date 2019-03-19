package com.dias.services.reports;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ReportsBuilderControllerTest extends AbstractReportsModuleTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() throws IOException {
        super.setUp();
    }

    @Test
    public void getSubSystemsTest() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/reports/analytics/subsystems"))
                .andExpect(status().isOk()).andReturn();
        JsonNode resultJson = objectMapper.readTree(result.getResponse().getContentAsString());
        Assert.assertTrue(resultJson.isArray());


    }


}
