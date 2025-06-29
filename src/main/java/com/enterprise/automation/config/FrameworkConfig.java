package com.enterprise.automation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.Valid;

import java.util.Map;

/**
 * Enterprise Automation Framework Configuration
 * 
 * This class maps the application.yml configuration to Java objects
 * using Spring Boot's ConfigurationProperties mechanism.
 * 
 * @author Enterprise Automation Team
 * @version 3.0
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "automation")
public class FrameworkConfig {

    @Valid
    @NotNull(message = "Web configuration is required")
    private Web web;
    
    @Valid
    private Api api;
    
    @Valid
    private Database database;
    
    @Valid
    private Mobile mobile;
    
    @Valid
    private Reporting reporting;
    
    @Valid
    private Remote remote;

    @Data
    public static class Web {
        @NotBlank(message = "Browser must be specified")
        private String browser;
        
        private boolean headless = false;
        
        @Min(value = 5, message = "Timeout must be at least 5 seconds")
        @Max(value = 300, message = "Timeout cannot exceed 300 seconds")
        private int timeout = 30;
        
        private String windowSize = "1920,1080";
        
        private Map<String, Object> capabilities;
        
        /**
         * Get browser capabilities for the specified browser
         * @param browserName Browser name (chrome, firefox, edge)
         * @return Browser-specific capabilities map
         */
        @SuppressWarnings("unchecked")
        public Map<String, Object> getBrowserCapabilities(String browserName) {
            if (capabilities != null && capabilities.containsKey(browserName)) {
                return (Map<String, Object>) capabilities.get(browserName);
            }
            return null;
        }
        
        /**
         * Check if a specific capability is enabled
         * @param capabilityName Name of the capability
         * @return true if capability exists and is true
         */
        public boolean isCapabilityEnabled(String capabilityName) {
            return capabilities != null && 
                   Boolean.TRUE.equals(capabilities.get(capabilityName));
        }
    }

    @Data
    public static class Api {
        @NotBlank(message = "Base URL must be specified for API testing")
        private String baseUrl;
        
        @Min(value = 1, message = "Timeout must be at least 1 second")
        private int timeout = 15;
        
        @Min(value = 0, message = "Retry count cannot be negative")
        @Max(value = 10, message = "Retry count cannot exceed 10")
        private int retryCount = 3;
        
        private Map<String, String> headers;
    }

    @Data
    public static class Database {
        @NotBlank(message = "Database URL must be specified")
        private String url;
        
        private String username;
        private String password;
        private String driver = "com.mysql.cj.jdbc.Driver";
        
        @Min(value = 1000, message = "Connection timeout must be at least 1000ms")
        private long connectionTimeout = 30000;
        
        @Min(value = 1, message = "Max pool size must be at least 1")
        @Max(value = 50, message = "Max pool size cannot exceed 50")
        private int maxPoolSize = 10;
    }

    @Data
    public static class Mobile {
        @NotBlank(message = "Mobile platform must be specified")
        private String platform;
        
        private String deviceName;
        private String appPath;
        private String automationName = "UiAutomator2";
        
        @Min(value = 60, message = "New command timeout must be at least 60 seconds")
        private int newCommandTimeout = 300;
    }

    @Data
    public static class Reporting {
        private String allureResults = "target/allure-results";
        private boolean screenshots = true;
        private boolean videoRecording = false;
        private boolean screenshotOnFailure = true;
        
        private ReportPortal reportPortal;
        
        @Data
        public static class ReportPortal {
            private boolean enabled = false;
            private String endpoint;
            private String project;
            private String apiKey;
        }
    }
    
    @Data
    public static class Remote {
        private String hubUrl = "http://localhost:4444";
        
        private Docker docker;
        private Cloud cloud;
        
        @Data
        public static class Docker {
            private boolean enabled = false;
            private String seleniumImage = "selenium/standalone-chrome:latest";
            private String networkName = "selenium-network";
        }
        
        @Data
        public static class Cloud {
            private String provider; // browserstack, saucelabs, lambdatest
            private Credentials credentials;
            
            @Data
            public static class Credentials {
                private String username;
                private String accessKey;
            }
        }
    }
    
    /**
     * Utility method to check if remote execution is configured
     * @return true if remote execution is properly configured
     */
    public boolean isRemoteExecutionConfigured() {
        return remote != null && remote.getHubUrl() != null && !remote.getHubUrl().isEmpty();
    }
    
    /**
     * Utility method to check if cloud execution is configured
     * @return true if cloud execution is properly configured
     */
    public boolean isCloudExecutionConfigured() {
        return remote != null && 
               remote.getCloud() != null && 
               remote.getCloud().getProvider() != null &&
               remote.getCloud().getCredentials() != null;
    }
    
    /**
     * Get environment-specific database URL
     * @param environment Environment name (local, dev, staging, prod)
     * @return Environment-specific database URL
     */
    public String getDatabaseUrlForEnvironment(String environment) {
        if (database == null || database.getUrl() == null) {
            return null;
        }
        
        String baseUrl = database.getUrl();
        if (environment != null && !environment.equals("prod")) {
            // Replace database name with environment-specific name
            return baseUrl.replaceAll("/testdb", "/testdb_" + environment);
        }
        return baseUrl;
    }
}