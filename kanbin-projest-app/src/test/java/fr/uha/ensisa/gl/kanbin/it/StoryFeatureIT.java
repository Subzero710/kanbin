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

        // 1. On génère un titre long
        String fullTitle = "Story Test Selenium " + System.currentTimeMillis();

        // 2. On calcule ce qu'on s'attend à voir (tronqué à 30 chars)
        String expectedTitle = fullTitle.length() > 30 ? fullTitle.substring(0, 30) : fullTitle;

        WebElement titleInput = driver.findElement(By.name("title"));
        WebElement submitBtn = driver.findElement(By.cssSelector("button[type='submit']"));

        // On envoie le titre long
        titleInput.sendKeys(fullTitle);
        submitBtn.click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.urlContains("/issues"));

        // 3. On cherche le titre TRONQUÉ
        WebElement storyCell = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//td[contains(text(), '" + expectedTitle + "')]")
        ));
        assertTrue(storyCell.isDisplayed(), "La story créée (tronquée) devrait être visible.");

        // Suppression
        WebElement deleteBtn = driver.findElement(By.xpath("//tr[td[contains(text(), '" + expectedTitle + "')]]//form//button"));
        deleteBtn.click();

        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();
        wait.until(ExpectedConditions.invisibilityOf(storyCell));

        String pageSource = driver.getPageSource();
        assertFalse(pageSource.contains(expectedTitle), "La story devrait avoir disparu après suppression.");
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

    @Test
    public void testRenameStory() {
        driver.get(getBaseUrl() + "issues/new");
        String originalTitle = "Original Name " + System.currentTimeMillis();
        // Troncature préventive pour la recherche
        String expectedOriginal = originalTitle.length() > 30 ? originalTitle.substring(0, 30) : originalTitle;

        WebElement titleInput = driver.findElement(By.name("title"));
        titleInput.sendKeys(originalTitle);
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.urlContains("/issues"));

        By rowLocator = By.xpath("//tr[td[contains(text(), '" + expectedOriginal + "')]]");
        wait.until(ExpectedConditions.presenceOfElementLocated(rowLocator));

        WebElement editBtn = driver.findElement(
                By.xpath("//tr[td[contains(text(), '" + expectedOriginal + "')]]//a[contains(@href, '/edit')]")
        );
        editBtn.click();
        // ----------------------

        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("issueTitle")));
        input.clear();
        String newTitle = "Renamed Name " + System.currentTimeMillis();
        // Calcul du nouveau titre attendu (tronqué)
        String expectedNew = newTitle.length() > 30 ? newTitle.substring(0, 30) : newTitle;

        input.sendKeys(newTitle);
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        wait.until(ExpectedConditions.urlContains("/issues"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//td[contains(text(), '" + expectedNew + "')]")
        ));
        String pageSource = driver.getPageSource();
        assertFalse(pageSource.contains(expectedOriginal), "L'ancien titre ne devrait plus être visible");
        assertTrue(pageSource.contains(expectedNew), "Le nouveau titre (tronqué) devrait être affiché");
    }
}