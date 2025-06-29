package com.enterprise.automation.tests;

import com.enterprise.automation.core.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;

@SpringBootTest
public abstract class BaseTest extends AbstractTestNGSpringContextTests {

    @Autowired
    protected WebDriverManager webDriverManager;

    protected WebDriver driver;

    @BeforeMethod
    public void setUp() {
        try {
            driver = webDriverManager.initializeDriver();
        } catch (Exception e) {
            e.printStackTrace();  // print full details in console
            throw new RuntimeException("WebDriver initialization failed: " + e.getMessage(), e);
        }
    }

    @AfterMethod
    public void tearDown() {
        try {
            WebDriverManager.quitDriver();
        } catch (Exception e) {
            System.err.println("Error during WebDriver teardown: " + e.getMessage());
        }
    }
}
