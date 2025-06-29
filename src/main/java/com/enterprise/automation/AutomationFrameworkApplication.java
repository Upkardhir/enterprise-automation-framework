package com.enterprise.automation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.enterprise.automation.config.FrameworkConfig;

@SpringBootApplication
@EnableConfigurationProperties(FrameworkConfig.class)
public class AutomationFrameworkApplication {
    public static void main(String[] args) {
        SpringApplication.run(AutomationFrameworkApplication.class, args);
    }
}

