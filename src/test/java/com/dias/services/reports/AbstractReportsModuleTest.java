package com.dias.services.reports;

import com.dias.services.reports.repository.ReportRepository;
import com.dias.services.reports.service.ReportBuilderService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(classes = {AbstractReportsModuleTest.TestConfig.class})
@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:test.properties")
@AutoConfigureMockMvc
public abstract class AbstractReportsModuleTest {

    private static boolean inited = false;

    @TestConfiguration
    @ComponentScan({"com.dias.services.reports"})
    static class TestConfig {

        @Autowired
        NamedParameterJdbcTemplate template;

        @Bean
        public ReportRepository reportRepository() {

            return new ReportRepository(template) {

                @Override
                protected String getInsertSql() {
                    String insertSql = super.getInsertSql();
                    //для работы в h2 базе нужно переделать генерацию id
                    return insertSql.replace("nextval('report_id_seq')","report_id_seq.NEXTVAL");
                }
            };
        }

    }

    protected MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private NamedParameterJdbcTemplate template;

    @Autowired
    private ReportRepository reportRepository;

    @Before
    public void setUp() throws IOException {

        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .build();

        if (!inited) {
            inited = true;
            //создаем тестовые данные
            reportRepository.executeSqlFromFile(getClass(), template, "/ReportsController/tablesAndData.sql");

            Map<String, Pair<Integer, Boolean>> typesMap = new HashMap<>();
            typesMap.put("-5", Pair.of(Types.NUMERIC, false));
            typesMap.put("3", Pair.of(Types.NUMERIC, false));
            ReflectionTestUtils.setField(ReportBuilderService.class, "typesMap", typesMap);

        }
    }
}
