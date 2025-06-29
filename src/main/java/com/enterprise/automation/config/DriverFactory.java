/*
 * package com.enterprise.automation.config;
 * 
 * import com.enterprise.automation.config.FrameworkConfig.Web; import
 * io.github.bonigarcia.wdm.WebDriverManager; import lombok.extern.slf4j.Slf4j;
 * import org.openqa.selenium.WebDriver; import
 * org.openqa.selenium.chrome.ChromeOptions; import
 * org.openqa.selenium.chrome.ChromeDriver; import
 * org.openqa.selenium.firefox.FirefoxOptions; import
 * org.openqa.selenium.firefox.FirefoxDriver; import
 * org.openqa.selenium.edge.EdgeOptions; import
 * org.openqa.selenium.edge.EdgeDriver; import
 * org.springframework.beans.factory.annotation.Autowired; import
 * org.springframework.stereotype.Component;
 * 
 * @Slf4j
 * 
 * @Component public class DriverFactory {
 * 
 * private static final ThreadLocal<WebDriver> driverThread = new
 * ThreadLocal<>();
 * 
 * @Autowired private FrameworkConfig config;
 * 
 * public WebDriver getDriver() { if (driverThread.get() == null) { Web
 * webConfig = config.getWeb(); String browser =
 * webConfig.getBrowser().toLowerCase(); boolean headless =
 * webConfig.isHeadless();
 * 
 * switch (browser) { case "chrome" -> {
 * WebDriverManager.chromedriver().setup(); ChromeOptions chromeOptions = new
 * ChromeOptions(); if (headless) chromeOptions.addArguments("--headless=new");
 * chromeOptions.addArguments("--window-size=" + webConfig.getWindowSize());
 * driverThread.set(new ChromeDriver(chromeOptions)); } case "firefox" -> {
 * WebDriverManager.firefoxdriver().setup(); FirefoxOptions firefoxOptions = new
 * FirefoxOptions(); if (headless) firefoxOptions.addArguments("--headless");
 * driverThread.set(new FirefoxDriver(firefoxOptions)); } case "edge" -> {
 * WebDriverManager.edgedriver().setup(); EdgeOptions edgeOptions = new
 * EdgeOptions(); if (headless) edgeOptions.addArguments("--headless=new");
 * driverThread.set(new EdgeDriver(edgeOptions)); } default -> throw new
 * IllegalArgumentException("Unsupported browser: " + browser); }
 * 
 * log.info("Initialized {} WebDriver in {} mode", browser, headless ?
 * "headless" : "headed"); }
 * 
 * return driverThread.get(); }
 * 
 * public void quitDriver() { WebDriver driver = driverThread.get(); if (driver
 * != null) { driver.quit(); driverThread.remove();
 * log.info("WebDriver instance closed and removed from thread"); } } }
 * 
 */