package com.enterprise.automation.core;

import com.enterprise.automation.config.FrameworkConfig;
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
     * Create remote WebDriver instance
     */
    private WebDriver createRemoteDriver(String browserType, boolean headless, String hubUrl) {
        try {
            // DesiredCapabilities is now initialized from the Options object
            DesiredCapabilities capabilities = new DesiredCapabilities();
            
            switch (browserType) {
                case "chrome":
                    // Correct syntax for Selenium 4.x+
                    ChromeOptions chromeOptions = getChromeOptionsFromConfig(headless);
                    capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
                    break;
                case "firefox":
                    // Correct syntax for Selenium 4.x+
                    FirefoxOptions firefoxOptions = getFirefoxOptionsFromConfig(headless);
                    capabilities.setCapability(FirefoxOptions.FIREFOX_OPTIONS, firefoxOptions);
                    break;
                case "edge":
                    // Correct syntax for Selenium 4.x+
                    EdgeOptions edgeOptions = getEdgeOptionsFromConfig();
                    capabilities.setCapability(EdgeOptions.CAPABILITY, edgeOptions);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported remote browser: " + browserType);
            }
            
            // Now, create the RemoteWebDriver with the updated capabilities
            return new RemoteWebDriver(new URL(hubUrl), capabilities);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create remote driver", e);
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

        // Apply headless mode from config
        if (headless) {
            options.addArguments("--headless=new");
        }

        // Apply capabilities from application.yml
        Map<String, Object> capabilities = webConfig.getCapabilities();
        if (capabilities != null) {
            // Handle acceptInsecureCerts
            if (capabilities.containsKey("acceptInsecureCerts")) {
                options.setAcceptInsecureCerts((Boolean) capabilities.get("acceptInsecureCerts"));
            }

            // New: Handle all browser-specific arguments
            if (capabilities.containsKey("chrome")) {
                Map<String, Object> chromeConfig = (Map<String, Object>) capabilities.get("chrome");
                if (chromeConfig != null && chromeConfig.containsKey("args")) {
                    // This directly reads the list of args from the YAML
                    List<String> args = (List<String>) chromeConfig.get("args");
                    options.addArguments(args);
                }
            }
        }

        // Window size logic can be moved to configureDriverFromConfig

        // Download preferences (can also be moved to YAML if needed)
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", System.getProperty("user.dir") + "/downloads");
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("safeBrowse.enabled", true);
        options.setExperimentalOption("prefs", prefs);

        // Replace the problematic line with this:
     // Just use asMap() to show all options
        Map<String, Object> optionsMap = options.asMap();
        logger.debug("Chrome options configured: {}", optionsMap);
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
        
        // Apply capabilities from application.yml
        Map<String, Object> capabilities = webConfig.getCapabilities();
        if (capabilities != null) {
            // Handle Firefox-specific options
            if (capabilities.containsKey("moz:firefoxOptions")) {
                Map<String, Object> firefoxConfig = (Map<String, Object>) capabilities.get("moz:firefoxOptions");
                if (firefoxConfig.containsKey("args")) {
                    List<String> args = (List<String>) firefoxConfig.get("args");
                    options.addArguments(args);
                }
            }
        }
        
        // Performance settings
        options.addPreference("dom.webnotifications.enabled", false);
        options.addPreference("media.volume_scale", "0.0");
        
        logger.debug("Firefox options configured");
        return options;
    }
    
    /**
     * Get Edge options from Spring Boot configuration
     */
    private EdgeOptions getEdgeOptionsFromConfig() {
        EdgeOptions options = new EdgeOptions();
        FrameworkConfig.Web webConfig = frameworkConfig.getWeb();
        
        // Apply window size from config
        if (webConfig.getWindowSize() != null) {
            options.addArguments("--window-size=" + webConfig.getWindowSize());
        }
        
        // Apply capabilities from application.yml
        Map<String, Object> capabilities = webConfig.getCapabilities();
        if (capabilities != null) {
            // Handle Edge-specific options
            if (capabilities.containsKey("ms:edgeOptions")) {
                Map<String, Object> edgeConfig = (Map<String, Object>) capabilities.get("ms:edgeOptions");
                if (edgeConfig.containsKey("args")) {
                    List<String> args = (List<String>) edgeConfig.get("args");
                    options.addArguments(args);
                }
            }
        }
        
        // Default settings
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        
        logger.debug("Edge options configured");
        return options;
    }
    
    /**
     * Configure driver with settings from application.yml
     */
    private void configureDriverFromConfig(WebDriver driver) {
        FrameworkConfig.Web webConfig = frameworkConfig.getWeb();
        
        // Configure timeouts from application.yml
        int timeout = webConfig.getTimeout();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(timeout));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(timeout * 2));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(timeout));
        
        // Window management
        driver.manage().window().maximize();
        
        // Delete all cookies
        driver.manage().deleteAllCookies();
        
        logger.info("Driver configured with timeout: {}s, window-size: {}", 
                   timeout, webConfig.getWindowSize());
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