package com.enterprise.automation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "automation")
public class FrameworkConfig {

    private Web web;
    private Api api;
    private Database database;
    private Mobile mobile;
    private Reporting reporting;

    @Data
    public static class Web {
        private String browser;
        private boolean headless;
        private int timeout;
        private String windowSize;
    }

    @Data
    public static class Api {
        private String baseUrl;
        private int timeout;
        private int retryCount;
    }

    @Data
    public static class Database {
        private String url;
        private String username;
        private String password;
        private String driver;
    }

    @Data
    public static class Mobile {
        private String platform;
        private String deviceName;
        private String appPath;
    }

    @Data
    public static class Reporting {
        private String allureResults;
        private boolean screenshots;
        private boolean videoRecording;
    }
}
