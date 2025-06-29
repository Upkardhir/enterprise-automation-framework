package com.enterprise.automation.core;

import com.enterprise.automation.config.FrameworkConfig;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.events.EventFiringDecorator;
import org.openqa.selenium.support.events.WebDriverListener;
//Import the static methods for all supported browsers
import static io.github.bonigarcia.wdm.WebDriverManager.chromedriver;
import static io.github.bonigarcia.wdm.WebDriverManager.firefoxdriver;
import static io.github.bonigarcia.wdm.WebDriverManager.edgedriver;
//Safari is not managed by WDM, so no static import is needed for it
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.LocalFileDetector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring Boot integrated WebDriver Manager for Enterprise Automation Framework
 * 
 * Features:
 * - Spring Boot configuration integration
 * - Thread-safe driver management for parallel execution
 * - Automatic configuration from application.yml
 * - Multiple browser support with custom capabilities
 * - Remote execution support (Grid, Docker, Cloud)
 * - Event listening and monitoring
 * - Profile-based environment configuration
 * 
 * @author Enterprise Automation Team
 * @version 3.0 (Spring Boot Integration)
 */
@Component
@Scope("singleton")
public class WebDriverManager {
    
    private static final Logger logger = LoggerFactory.getLogger(WebDriverManager.class);
    
    @Autowired
    private FrameworkConfig frameworkConfig;
    
    // Thread-safe driver storage for parallel execution
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<String> browserThreadLocal = new ThreadLocal<>();
    
    // Driver instances tracking for cleanup
    private static final Map<Long, WebDriver> driverInstances = new ConcurrentHashMap<>();
    
    // Execution modes
    public enum ExecutionMode {
        LOCAL, REMOTE, DOCKER, CLOUD
    }
    
    /**
     * Initialize WebDriver using Spring Boot configuration
     * Reads settings from application.yml
     * 
     * @return WebDriver instance configured from application.yml
     */
    public WebDriver initializeDriver() {
        return initializeDriver(ExecutionMode.LOCAL, null);
    }
    
    /**
     * Initialize WebDriver with specific execution mode
     * 
     * @param executionMode Execution mode (local, remote, etc.)
     * @param hubUrl Remote hub URL (for remote execution)
     * @return WebDriver instance
     */
    public WebDriver initializeDriver(ExecutionMode executionMode, String hubUrl) {
        try {
            FrameworkConfig.Web webConfig = frameworkConfig.getWeb();
            String browserType = webConfig.getBrowser().toLowerCase();
            boolean headless = webConfig.isHeadless();
            
            logger.info("Initializing {} driver in {} mode (headless: {})", 
                       browserType, executionMode, headless);
            
            WebDriver driver = null;
            
            switch (executionMode) {
                case LOCAL:
                    driver = createLocalDriver(browserType, headless);
                    break;
                case REMOTE:
                    driver = createRemoteDriver(browserType, headless, hubUrl);
                    break;
                case DOCKER:
                    driver = createDockerDriver(browserType, headless, hubUrl);
                    break;
                case CLOUD:
                    driver = createCloudDriver(browserType, headless, hubUrl);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported execution mode: " + executionMode);
            }
            
            // Configure driver with settings from application.yml
            configureDriverFromConfig(driver);
            
            // Add event listener for monitoring
            driver = addEventListener(driver);
            
            // Store in thread local for parallel execution
            driverThreadLocal.set(driver);
            browserThreadLocal.set(browserType);
            
            // Track driver instance for cleanup
            driverInstances.put(Thread.currentThread().getId(), driver);
            
            logger.info("Driver initialized successfully: {}", browserType);
            return driver;
            
        } catch (Exception e) {
            logger.error("Failed to initialize driver: {}", e.getMessage(), e);
            throw new RuntimeException("WebDriver initialization failed", e);
        }
    }
    
    /**
     * Get current thread's WebDriver instance
     * 
     * @return WebDriver instance
     */
    public static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            throw new IllegalStateException("WebDriver not initialized. Call initializeDriver() first.");
        }
        return driver;
    }
    
    /**
     * Get current browser type
     * 
     * @return Browser type string
     */
    public static String getCurrentBrowser() {
        return browserThreadLocal.get();
    }
    
    /**
     * Create local WebDriver instance based on Spring Boot configuration
     */
    private WebDriver createLocalDriver(String browserType, boolean headless) {
        WebDriver driver;
        
        switch (browserType) {
            case "chrome":
                chromedriver().setup();
                driver = new ChromeDriver(getChromeOptionsFromConfig(headless));
                break;
                
            case "firefox":
                firefoxdriver().setup();
                driver = new FirefoxDriver(getFirefoxOptionsFromConfig(headless));
                break;
                
            case "edge":
                edgedriver().setup();
                driver = new EdgeDriver(getEdgeOptionsFromConfig());
                break;
                
            case "safari":
                driver = new SafariDriver();
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported browser: " + browserType);
        }
        
        return driver;
    }
    
    /**
     * Modern approach for creating remote WebDriver (Selenium 4+)
     */
    private WebDriver createRemoteDriver(String browserType, boolean headless, String hubUrl) {
        try {
            AbstractDriverOptions<?> options;
            
            switch (browserType.toLowerCase()) {
                case "chrome":
                    options = getChromeOptionsFromConfig(headless);
                    break;
                case "firefox":
                    options = getFirefoxOptionsFromConfig(headless);
                    break;
                case "edge":
                    options = getEdgeOptionsFromConfig();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported remote browser: " + browserType);
            }
            
            logger.info("Creating remote WebDriver for {} at {}", browserType, hubUrl);
            RemoteWebDriver remoteDriver = new RemoteWebDriver(new URL(hubUrl), options);
            
            // Set additional RemoteWebDriver specific configurations
            remoteDriver.setFileDetector(new LocalFileDetector());
            
            return remoteDriver;
            
        } catch (Exception e) {
            logger.error("Failed to create remote driver for {}: {}", browserType, e.getMessage(), e);
            throw new RuntimeException("Remote WebDriver creation failed", e);
        }
    }

    
    /**
     * Create Docker WebDriver instance
     */
    private WebDriver createDockerDriver(String browserType, boolean headless, String dockerUrl) {
        return createRemoteDriver(browserType, headless, dockerUrl);
    }
    
    /**
     * Create Cloud WebDriver instance
     */
    private WebDriver createCloudDriver(String browserType, boolean headless, String cloudUrl) {
        return createRemoteDriver(browserType, headless, cloudUrl);
    }
    
    /**
     * Get Chrome options from Spring Boot configuration
     */
    /**
     * Get Chrome options from Spring Boot configuration
     * This enhanced version reads all 'args' from the YAML capabilities map.
     */
    private ChromeOptions getChromeOptionsFromConfig(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        FrameworkConfig.Web webConfig = frameworkConfig.getWeb();

        // Apply headless mode
        if (headless) {
            options.addArguments("--headless=new");
        }

        // Get Chrome-specific capabilities from YAML
        Map<String, Object> chromeConfig = webConfig.getBrowserCapabilities("chrome");
        if (chromeConfig != null) {
            // Handle Chrome arguments
            if (chromeConfig.containsKey("args")) {
                @SuppressWarnings("unchecked")
                List<String> args = (List<String>) chromeConfig.get("args");
                if (args != null && !args.isEmpty()) {
                    options.addArguments(args);
                    logger.debug("Added Chrome arguments: {}", args);
                }
            }
            
            // Handle Chrome preferences
            if (chromeConfig.containsKey("prefs")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> prefs = (Map<String, Object>) chromeConfig.get("prefs");
                if (prefs != null && !prefs.isEmpty()) {
                    options.setExperimentalOption("prefs", prefs);
                    logger.debug("Added Chrome preferences: {}", prefs);
                }
            }
            
            // Handle experimental options
            if (chromeConfig.containsKey("experimentalOptions")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> expOptions = (Map<String, Object>) chromeConfig.get("experimentalOptions");
                expOptions.forEach(options::setExperimentalOption);
            }
        }

        // Apply global capabilities
        Map<String, Object> capabilities = webConfig.getCapabilities();
        if (capabilities != null) {
            if (capabilities.containsKey("acceptInsecureCerts")) {
                options.setAcceptInsecureCerts((Boolean) capabilities.get("acceptInsecureCerts"));
            }
            
            if (capabilities.containsKey("pageLoadStrategy")) {
                options.setPageLoadStrategy(PageLoadStrategy.fromString((String) capabilities.get("pageLoadStrategy")));
            }
            
            if (capabilities.containsKey("unhandledPromptBehavior")) {
                options.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.fromString((String) capabilities.get("unhandledPromptBehavior")));
            }
        }

        // Default download preferences if not specified in config
        if (chromeConfig == null || !chromeConfig.containsKey("prefs")) {
            Map<String, Object> defaultPrefs = new HashMap<>();
            defaultPrefs.put("download.default_directory", System.getProperty("user.dir") + "/downloads");
            defaultPrefs.put("download.prompt_for_download", false);
            defaultPrefs.put("download.directory_upgrade", true);
            defaultPrefs.put("safeBrowse.enabled", true);
            options.setExperimentalOption("prefs", defaultPrefs);
        }

        logger.debug("Chrome options configured successfully");
        return options;
    }

    /**
     * Get Firefox options from Spring Boot configuration
     */
    private FirefoxOptions getFirefoxOptionsFromConfig(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        FrameworkConfig.Web webConfig = frameworkConfig.getWeb();

        // Apply headless mode
        if (headless) {
            options.addArguments("--headless");
        }

        // Get Firefox-specific capabilities from YAML
        Map<String, Object> firefoxConfig = webConfig.getBrowserCapabilities("firefox");
        if (firefoxConfig != null) {
            // Handle Firefox arguments
            if (firefoxConfig.containsKey("args")) {
                @SuppressWarnings("unchecked")
                List<String> args = (List<String>) firefoxConfig.get("args");
                if (args != null && !args.isEmpty()) {
                    options.addArguments(args);
                    logger.debug("Added Firefox arguments: {}", args);
                }
            }
            
            // Handle Firefox preferences
            if (firefoxConfig.containsKey("prefs")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> prefs = (Map<String, Object>) firefoxConfig.get("prefs");
                if (prefs != null) {
                    prefs.forEach((key, value) -> {
                        if (value instanceof Boolean) {
                            options.addPreference(key, (Boolean) value);
                        } else if (value instanceof Integer) {
                            options.addPreference(key, (Integer) value);
                        } else {
                            options.addPreference(key, value.toString());
                        }
                    });
                    logger.debug("Added Firefox preferences: {}", prefs);
                }
            }
        }

        // Apply global capabilities
        Map<String, Object> capabilities = webConfig.getCapabilities();
        if (capabilities != null) {
            if (capabilities.containsKey("acceptInsecureCerts")) {
                options.setAcceptInsecureCerts((Boolean) capabilities.get("acceptInsecureCerts"));
            }
            
            if (capabilities.containsKey("pageLoadStrategy")) {
                options.setPageLoadStrategy(PageLoadStrategy.fromString((String) capabilities.get("pageLoadStrategy")));
            }
        }

        // Default preferences if not specified in config
        if (firefoxConfig == null || !firefoxConfig.containsKey("prefs")) {
            options.addPreference("dom.webnotifications.enabled", false);
            options.addPreference("media.volume_scale", "0.0");
            options.addPreference("browser.download.folderList", 2);
            options.addPreference("browser.download.dir", System.getProperty("user.dir") + "/downloads");
        }

        logger.debug("Firefox options configured successfully");
        return options;
    }
    
    /**
     * Get Edge options from Spring Boot configuration
     */
    private EdgeOptions getEdgeOptionsFromConfig() {
        EdgeOptions options = new EdgeOptions();
        FrameworkConfig.Web webConfig = frameworkConfig.getWeb();

        // Get Edge-specific capabilities from YAML
        Map<String, Object> edgeConfig = webConfig.getBrowserCapabilities("edge");
        if (edgeConfig != null) {
            // Handle Edge arguments
            if (edgeConfig.containsKey("args")) {
                @SuppressWarnings("unchecked")
                List<String> args = (List<String>) edgeConfig.get("args");
                if (args != null && !args.isEmpty()) {
                    options.addArguments(args);
                    logger.debug("Added Edge arguments: {}", args);
                }
            }
            
            // Handle experimental options
            if (edgeConfig.containsKey("experimentalOptions")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> expOptions = (Map<String, Object>) edgeConfig.get("experimentalOptions");
                expOptions.forEach(options::setExperimentalOption);
            }
        }

        // Apply global capabilities
        Map<String, Object> capabilities = webConfig.getCapabilities();
        if (capabilities != null) {
            if (capabilities.containsKey("acceptInsecureCerts")) {
                options.setAcceptInsecureCerts((Boolean) capabilities.get("acceptInsecureCerts"));
            }
            
            if (capabilities.containsKey("pageLoadStrategy")) {
                options.setPageLoadStrategy(PageLoadStrategy.fromString((String) capabilities.get("pageLoadStrategy")));
            }
        }

        // Default arguments if not specified in config
        if (edgeConfig == null || !edgeConfig.containsKey("args")) {
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
        }

        logger.debug("Edge options configured successfully");
        return options;
    }
    
    /**
     * Enhanced driver configuration with better error handling
     */
    private void configureDriverFromConfig(WebDriver driver) {
        try {
            FrameworkConfig.Web webConfig = frameworkConfig.getWeb();
            
            // Configure timeouts
            int timeout = webConfig.getTimeout();
            WebDriver.Timeouts timeouts = driver.manage().timeouts();
            timeouts.implicitlyWait(Duration.ofSeconds(timeout));
            timeouts.pageLoadTimeout(Duration.ofSeconds(timeout * 2));
            timeouts.scriptTimeout(Duration.ofSeconds(timeout));
            
            // Window management
            String windowSize = webConfig.getWindowSize();
            if (windowSize != null && windowSize.contains(",")) {
                String[] dimensions = windowSize.split(",");
                if (dimensions.length == 2) {
                    try {
                        int width = Integer.parseInt(dimensions[0].trim());
                        int height = Integer.parseInt(dimensions[1].trim());
                        driver.manage().window().setSize(new Dimension(width, height));
                        logger.info("Window size set to: {}x{}", width, height);
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid window size format: {}. Maximizing window instead.", windowSize);
                        driver.manage().window().maximize();
                    }
                }
            } else {
                driver.manage().window().maximize();
            }
            
            // Clear cookies and storage
            driver.manage().deleteAllCookies();
            
            // Clear local storage and session storage (if supported)
            try {
                ((JavascriptExecutor) driver).executeScript("window.localStorage.clear();");
                ((JavascriptExecutor) driver).executeScript("window.sessionStorage.clear();");
            } catch (Exception e) {
                logger.debug("Could not clear browser storage: {}", e.getMessage());
            }
            
            logger.info("Driver configured successfully with timeout: {}s", timeout);
            
        } catch (Exception e) {
            logger.error("Failed to configure driver: {}", e.getMessage(), e);
            throw new RuntimeException("Driver configuration failed", e);
        }
    }
    
    /**
     * Add event listener for monitoring and logging
     */
    private WebDriver addEventListener(WebDriver driver) {
        WebDriverListener listener = new WebDriverListener() {
            @Override
            public void beforeGet(WebDriver driver, String url) {
                logger.info("Navigating to: {}", url);
            }
            
            @Override
            public void afterGet(WebDriver driver, String url) {
                logger.info("Successfully navigated to: {}", url);
            }
            
            @Override
            public void onError(Object target, Method method, Object[] args, InvocationTargetException e) {
                logger.error("WebDriver error occurred: {}", e.getTargetException().getMessage());
            }
        };
        
        return new EventFiringDecorator<>(listener).decorate(driver);
    }
    
    /**
     * Quit current thread's driver
     */
    public static void quitDriver() {
        try {
            WebDriver driver = driverThreadLocal.get();
            if (driver != null) {
                logger.info("Quitting driver for thread: {}", Thread.currentThread().getId());
                driver.quit();
                driverThreadLocal.remove();
                browserThreadLocal.remove();
                driverInstances.remove(Thread.currentThread().getId());
                logger.info("Driver quit successfully");
            }
        } catch (Exception e) {
            logger.error("Error while quitting driver: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Quit all driver instances (cleanup method)
     */
    public static void quitAllDrivers() {
        logger.info("Quitting all driver instances...");
        
        driverInstances.values().parallelStream().forEach(driver -> {
            try {
                if (driver != null) {
                    driver.quit();
                }
            } catch (Exception e) {
                logger.warn("Error quitting driver: {}", e.getMessage());
            }
        });
        
        driverInstances.clear();
        logger.info("All drivers quit successfully");
    }
    
    /**
     * Get framework configuration
     */
    public FrameworkConfig getFrameworkConfig() {
        return frameworkConfig;
    }
    
    /**
     * Get active driver count
     */
    public static int getActiveDriverCount() {
        return driverInstances.size();
    }
    
    /**
     * Check if driver is initialized for current thread
     */
    public static boolean isDriverInitialized() {
        return driverThreadLocal.get() != null;
    }
}