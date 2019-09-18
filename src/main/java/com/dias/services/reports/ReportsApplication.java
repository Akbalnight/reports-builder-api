package com.dias.services.reports;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(scanBasePackages = {"com.dias.services", "com.common.services"})
public class ReportsApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ReportsApplication.class, args);
    }
}
