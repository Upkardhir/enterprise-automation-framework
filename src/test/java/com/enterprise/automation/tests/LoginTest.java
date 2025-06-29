package com.enterprise.automation.tests;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.testng.annotations.Test;

@SpringBootTest
public class LoginTest extends BaseTest {
    @Test
    public void verifyLoginSuccess() {
        driver.get("https://practice.expandtesting.com/login");
        // your login code...
    }
}
