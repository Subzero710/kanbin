package fr.uha.ensisa.gl.kanbin.it;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StoryFeatureIT extends AbstractIT {

    @Test
    public void testCreateAndDeleteStory() {
        driver.get(getBaseUrl() + "issues/new");
        String storyTitle = "Story Test Selenium " + System.currentTimeMillis();
        WebElement titleInput = driver.findElement(By.name("title"));
        WebElement submitBtn = driver.findElement(By.cssSelector("button[type='submit']"));
        titleInput.sendKeys(storyTitle);
        submitBtn.click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.urlContains("/issues"));

        WebElement storyCell = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//td[contains(text(), '" + storyTitle + "')]")
        ));
        assertTrue(storyCell.isDisplayed(), "La story créée devrait être visible.");

        WebElement deleteBtn = driver.findElement(By.xpath("//tr[td[contains(text(), '" + storyTitle + "')]]//button"));
        deleteBtn.click();

        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();
        wait.until(ExpectedConditions.invisibilityOf(storyCell));

        String pageSource = driver.getPageSource();
        assertFalse(pageSource.contains(storyTitle), "La story devrait avoir disparu après suppression.");
    }

    @Test
    public void testNavigateToBoard() {
        driver.get(getBaseUrl() + "issues");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        WebElement toBoardBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("btn-to-board")));
        toBoardBtn.click();
        wait.until(ExpectedConditions.urlContains("/board"));
        assertTrue(driver.getTitle().contains("Board") || driver.getPageSource().contains("Kanbin"),
                "Devrait être arrivé sur la page du Board");
    }
}