package com.enterprise.automation.tests;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {
    
	@Test
	public void verifyLoginSuccess() {
	    driver.get("https://practice.expandtesting.com/login");

	    driver.findElement(By.id("username")).sendKeys("practice");
	    driver.findElement(By.id("password")).sendKeys("SuperSecretPassword!");
	    driver.findElement(By.cssSelector("button[type='submit']")).click();

	    String successMsg = driver.findElement(By.cssSelector(".flash.success")).getText();
	    Assert.assertTrue(successMsg.contains("You logged into a secure area!"));
	}

}
