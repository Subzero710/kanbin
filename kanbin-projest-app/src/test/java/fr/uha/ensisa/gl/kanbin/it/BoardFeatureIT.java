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

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BoardFeatureIT {

    public static WebDriver driver;
    private static String host;
    private static String port;

    @BeforeAll
    public static void setupWebDriver() {
        if (driver != null) return;

        host = System.getProperty("host", "localhost");
        port = System.getProperty("servlet.port", "8080");

        WebDriverManager.chromedriver().setup(); // Configure le driver Chrome


        ChromeOptions options = new ChromeOptions();
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

    // LE TEST POUR L'ISSUE #29
    @Test
    public void testAddColumnFeature() {

        driver.get(getBaseUrl() + "board");


        String newColumnName = "Test Integration Colonne";


        WebElement titleInput = driver.findElement(By.name("title"));
        WebElement submitButton = driver.findElement(By.cssSelector("input[type='submit'][value='Ajouter Colonne']"));


        titleInput.sendKeys(newColumnName);
        submitButton.click();

        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains(newColumnName),
                "La page devrait contenir le titre de la nouvelle colonne après l'ajout.");
    }
}