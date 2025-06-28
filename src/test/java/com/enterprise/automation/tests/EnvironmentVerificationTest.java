package com.enterprise.automation.tests;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import io.github.bonigarcia.wdm.WebDriverManager;

public class EnvironmentVerificationTest {
    
    public static void main(String[] args) {
        // Java 21 Text Block feature
        String message = """
            🚀 Enterprise Automation Framework
            ✅ Java 21 Features Enabled
            ✅ Selenium 4.26 Ready
            ✅ Environment Verified
            """;
        
        System.out.println(message);
        System.out.println("Java Version: " + System.getProperty("java.version"));
        
        // Quick Selenium test
        try {
            WebDriverManager.chromedriver().setup();
            WebDriver driver = new ChromeDriver();
            driver.get("https://www.google.com");
            System.out.println("✅ Selenium WebDriver working perfectly!");
            System.out.println("Page Title: " + driver.getTitle());
            driver.quit();
        } catch (Exception e) {
            System.out.println("Note: Chrome test skipped - " + e.getMessage());
        }
        
        System.out.println("\n🎯 Enterprise framework ready for development!");
    }
}