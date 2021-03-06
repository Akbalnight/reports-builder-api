package com.dias.services.reports.config;

import com.dias.services.notifications.NotificationsServiceImpl;
import com.dias.services.notifications.database.NotificationsDatabaseDao;
import com.dias.services.notifications.interfaces.INotificationsDao;
import com.dias.services.notifications.interfaces.INotificationsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.sql.DataSource;
import java.util.List;

@Configuration
@EnableAsync
@PropertySource(value="file:${config}/application.properties", ignoreResourceNotFound = true)
public class ServiceConfig {

    @Value("${notifications.enable:false}")
    Boolean enableNotifications;

    @Value("${scheme.name:public}")
    private String schemeName;

    @Autowired
    NamedParameterJdbcTemplate template;

    @Autowired
    ObjectMapper objectMapper;

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setCacheSeconds(10); //reload messages every 10 seconds
        return messageSource;
    }

    @Bean
    public INotificationsService notificationsService() {
        if (!enableNotifications) {
            return new INotificationsService() {
                @Override
                public void sendNotification(int typeId, String[] objects, List<Integer> receivers, String targetId, Integer initiatorId) {
                    // ???????????? ???? ????????????
                }
                @Override
                public void stopNotifications(int typeId, List<Integer> receivers, String targetId) {
                    // ???????????? ???? ????????????
                }
            };
        } else {
            return new NotificationsServiceImpl(new NotificationsDatabaseDao(objectMapper, schemeName, template));
        }
    }
}
