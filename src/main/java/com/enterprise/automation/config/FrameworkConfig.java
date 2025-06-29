package com.enterprise.automation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import java.util.Map; // Import Map for the new fields

@Data
@Component // Added @Component to ensure it's picked up by component scanning
@Configuration // You can keep this annotation for configuration purposes
@Validated // New: Added validation annotation
@ConfigurationProperties(prefix = "automation")
public class FrameworkConfig {

    private Web web;
    private Api api;
    private Database database;
    private Mobile mobile;
    private Reporting reporting;

    @Data
    public static class Web {
        @NotBlank(message = "Browser must be specified in the configuration.") // New: Validation
        private String browser;
        private boolean headless;
        private int timeout;
        private String windowSize;
        private Map<String, Object> capabilities; // New: To map browser capabilities like args
    }

    @Data
    public static class Api {
        @NotBlank(message = "Base URL must be specified for API testing.") // New: Validation
        private String baseUrl;
        private int timeout;
        private int retryCount;
        private Map<String, String> headers; // New: To map default headers
    }

    @Data
    public static class Database {
        @NotBlank(message = "Database URL must be specified.") // New: Validation
        private String url;
        private String username;
        private String password;
        private String driver;
    }

    @Data
    public static class Mobile {
        @NotBlank(message = "Mobile platform must be specified.") // New: Validation
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