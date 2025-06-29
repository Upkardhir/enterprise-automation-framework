package com.enterprise.automation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "automation.web.capabilities.chrome")
public class ChromeConfig {
    private List<String> args;
    private Map<String, Object> prefs;
}
