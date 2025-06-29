package com.enterprise.automation.tests;

import com.enterprise.automation.core.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public abstract class BaseTest {

    @Autowired
    protected WebDriverManager webDriverManager;

    protected WebDriver driver;

    @BeforeEach
    public void setUp() {
        driver = webDriverManager.initializeDriver();
    }

    @AfterEach
    public void tearDown() {
        WebDriverManager.quitDriver();  // or webDriverManager.quitDriver()
    }
}

