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

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BoardFeatureIT extends AbstractIT {
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

        // Attendre que l'élément soit rendu dans la vue mise à jour (5 secondes max)
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

    @Test
    public void testRemoveColumnFeature() {
        driver.get(getBaseUrl() + "board");

        // 1. Créer une colonne spécifique pour être sûr de ce qu'on supprime
        String colName = "To Delete";
        String colKey = "to-delete";

        // Ajout rapide via le DOM ou réutilisation de la méthode de test si refactorisée
        // Ici on refait l'action manuellement pour être autonome
        driver.findElement(By.name("title")).sendKeys(colName);
        driver.findElement(By.cssSelector("input[value='Ajouter Colonne']")).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        // Attendre qu'elle soit là
        WebElement colSection = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("section[data-col='" + colKey + "']")));

        // 2. Trouver le bouton avec le bon sélecteur
        // On cherche le bouton submit DANS le formulaire dont l'action contient 'remove-column'
        WebElement removeBtn = colSection.findElement(
                By.cssSelector("form[action*='remove-column'] button[type='submit']")
        );

        // 3. Action : Cliquer
        removeBtn.click();

        // 4. IMPORTANT : Gérer la pop-up de confirmation JS (confirm())
        // On attend que l'alerte soit présente et on l'accepte (clic sur OK)
        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();

        // 5. Vérification : La section doit disparaître
        boolean isGone = wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("section[data-col='" + colKey + "']")));

        assertTrue(isGone, "La colonne aurait dû disparaître de l'interface.");
    }
}