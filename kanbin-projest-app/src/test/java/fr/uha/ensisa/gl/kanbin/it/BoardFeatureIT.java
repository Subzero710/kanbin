package fr.uha.ensisa.gl.kanbin.it;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BoardFeatureIT extends AbstractIT {

    @Test
    public void testAddColumnFeature() {
        driver.get(getBaseUrl() + "board");
        String newColumnTitle = "Test Integration Colonne";
        String expectedKey = newColumnTitle.toLowerCase().replaceAll("\\s+", "-");

        WebElement titleInput = driver.findElement(By.name("title"));
        WebElement submitButton = driver.findElement(By.cssSelector("input[type='submit'][value='Ajouter Colonne']"));

        titleInput.sendKeys(newColumnTitle);
        submitButton.click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        WebElement newColumnHeader = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.xpath(
                        "//section[@data-col='" + expectedKey + "']//span[contains(text(), '" + newColumnTitle + "')]"
                ))
        );
        assertTrue(newColumnHeader.isDisplayed(), "La page devrait contenir le titre de la nouvelle colonne.");
    }

    @Test
    public void testRemoveColumnFeature() {
        driver.get(getBaseUrl() + "board");
        String colName = "To Delete";
        String colKey = "to-delete";

        driver.findElement(By.name("title")).sendKeys(colName);
        driver.findElement(By.cssSelector("input[value='Ajouter Colonne']")).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        WebElement colSection = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("section[data-col='" + colKey + "']")));

        WebElement removeBtn = colSection.findElement(
                By.cssSelector("form[action*='remove-column'] button[type='submit']")
        );
        removeBtn.click();

        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();

        boolean isGone = wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("section[data-col='" + colKey + "']")));

        assertTrue(isGone, "La colonne aurait dû disparaître de l'interface.");
    }

    @Test
    public void testNavigateToIssues() {
        driver.get(getBaseUrl() + "board");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        WebElement toIssuesBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("btn-to-issues")));
        toIssuesBtn.click();

        wait.until(ExpectedConditions.urlContains("/issues"));

        assertTrue(driver.getTitle().contains("Liste") || driver.getPageSource().contains("Stories"),
                "Devrait être arrivé sur la liste des stories");
    }

    @Test
    public void testRenameColumn() {
        driver.get(getBaseUrl() + "board");
        String originalTitle = "Col To Rename " + System.currentTimeMillis();
        driver.findElement(By.name("title")).sendKeys(originalTitle);
        driver.findElement(By.cssSelector("input[value='Ajouter Colonne']")).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        WebElement editBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//header[contains(., '" + originalTitle + "')]//a[contains(@title, 'Renommer')]")
        ));
        editBtn.click();

        wait.until(ExpectedConditions.urlContains("/edit"));
        WebElement input = driver.findElement(By.id("colTitle"));
        input.clear();
        String newTitle = "RENAMED " + System.currentTimeMillis();
        input.sendKeys(newTitle);
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        wait.until(ExpectedConditions.urlContains("/board"));

        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), newTitle));

        boolean newPresent = driver.getPageSource().contains(newTitle);
        assertTrue(newPresent, "Le nouveau titre de colonne devrait être affiché");
    }

    @Test
    public void testAddColumnDisplaysSubColumns() {
        driver.get(getBaseUrl() + "board");

        //  Ajouter une colonne "Integration"
        driver.findElement(By.name("title")).sendKeys("Integration");
        driver.findElement(By.cssSelector("input[type='submit'][value='Ajouter Colonne']")).click();

        // Vérifier que le titre principal est là
        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains("Integration"), "Le parent doit être affiché");

        // Vérifier que les sous-titres sont là
        assertTrue(pageSource.contains("À Faire"), "La sous-colonne 'À Faire' doit exister");
        assertTrue(pageSource.contains("En Cours"), "La sous-colonne 'En Cours' doit exister");
    }
}