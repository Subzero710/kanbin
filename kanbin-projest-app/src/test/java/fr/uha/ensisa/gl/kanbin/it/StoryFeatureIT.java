package fr.uha.ensisa.gl.kanbin.it;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StoryFeatureIT {

    public static WebDriver driver;
    private static String host;
    private static String port;

    @BeforeAll
    public static void setupWebDriver() {
        if (driver != null) return;

        // Récupération du port dynamique ou utilisation de 8080 par défaut
        host = System.getProperty("host", "localhost");
        port = System.getProperty("servlet.port", "8080");

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        // Mode headless pour éviter d'ouvrir une fenêtre pendant les tests automatisés
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
    }

    @AfterAll
    public static void shutdownWebDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    public static String getBaseUrl() {
        return "http://" + host + ":" + port + "/";
    }

    @Test
    public void testCreateAndDeleteStory() {
        // 1. Aller sur la page de création
        driver.get(getBaseUrl() + "issues/new");

        // 2. Créer une Story unique pour le test
        String storyTitle = "Story Test Selenium " + System.currentTimeMillis();

        // Localisation des éléments
        WebElement titleInput = driver.findElement(By.name("title")); // ou By.id("issueTitle") selon votre HTML
        WebElement submitBtn = driver.findElement(By.cssSelector("button[type='submit']"));

        // Action
        titleInput.sendKeys(storyTitle);
        submitBtn.click();

        // 3. Attendre la redirection vers la liste
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.urlContains("/issues"));

        // Vérifier que la story est bien visible
        // Note: Utilisation de xpath pour être sûr que le texte est dans la page
        WebElement storyCell = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//td[contains(text(), '" + storyTitle + "')]")
        ));
        assertTrue(storyCell.isDisplayed(), "La story créée devrait être visible.");

        // 4. Tester la Suppression
        // Trouver le bouton 'Supprimer' situé dans la même ligne (tr) que notre story
        WebElement deleteBtn = driver.findElement(By.xpath("//tr[td[contains(text(), '" + storyTitle + "')]]//button"));
        deleteBtn.click();

        // 5. Gérer la confirmation JavaScript (Alert)
        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept(); // Cliquer sur "OK"

        // 6. Vérifier que la story a disparu
        // On attend que l'élément devienne "stale" (obsolète/supprimé du DOM) ou invisible
        wait.until(ExpectedConditions.invisibilityOf(storyCell));

        String pageSource = driver.getPageSource();
        assertFalse(pageSource.contains(storyTitle), "La story devrait avoir disparu après suppression.");
    }
}