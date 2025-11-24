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
import org.openqa.selenium.support.ui.ExpectedConditions; // ⬅️ NOUVEL IMPORT
import org.openqa.selenium.support.ui.WebDriverWait;     // ⬅️ NOUVEL IMPORT
import java.time.Duration;                             // ⬅️ NOUVEL IMPORT

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

    @Test
    public void testAddColumnFeature() {

        driver.get(getBaseUrl() + "board");

        String newColumnTitle = "Test Integration Colonne";

        // La clé générée dans le BoardController est utilisée pour la recherche (ex: test-integration-colonne)
        String expectedKey = newColumnTitle.toLowerCase().replaceAll("\\s+", "-");


        WebElement titleInput = driver.findElement(By.name("title"));
        WebElement submitButton = driver.findElement(By.cssSelector("input[type='submit'][value='Ajouter Colonne']"));


        titleInput.sendKeys(newColumnTitle);
        submitButton.click();

        // ⬇️ CORRECTION CLÉ : Attendre que l'élément soit rendu dans la vue mise à jour (5 secondes max)
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        // Attendre que la section (section.kb-col) contenant l'attribut data-col='test-integration-colonne'
        // et le titre 'Test Integration Colonne' soit présente et visible.
        WebElement newColumnHeader = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.xpath(
                        "//section[@data-col='" + expectedKey + "']//span[contains(text(), '" + newColumnTitle + "')]"
                ))
        );

        // Assertion vérifiant que l'élément a été trouvé et est affiché
        assertTrue(newColumnHeader.isDisplayed(),
                "La page devrait contenir le titre de la nouvelle colonne après l'ajout.");
    }
}