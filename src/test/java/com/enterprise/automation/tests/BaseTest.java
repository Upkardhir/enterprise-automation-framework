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
        driver = webDriverManager.initializeDriver();
    }

    @AfterMethod
    public void tearDown() {
        WebDriverManager.quitDriver();
    }
}
