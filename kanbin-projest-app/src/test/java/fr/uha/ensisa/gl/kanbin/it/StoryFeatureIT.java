package fr.uha.ensisa.gl.kanbin.it;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

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

        WebElement deleteBtn = driver.findElement(By.xpath("//tr[td[contains(text(), '" + storyTitle + "')]]//form//button"));
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

    @Test
    public void testRenameStory() {
        driver.get(getBaseUrl() + "issues/new");
        String originalTitle = "Original Name " + System.currentTimeMillis();
        WebElement titleInput = driver.findElement(By.name("title"));
        titleInput.sendKeys(originalTitle);
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.urlContains("/issues"));

        By rowLocator = By.xpath("//tr[td[contains(text(), '" + originalTitle + "')]]");
        wait.until(ExpectedConditions.presenceOfElementLocated(rowLocator));

        WebElement editBtn = driver.findElement(
                By.xpath("//tr[td[contains(text(), '" + originalTitle + "')]]//a[contains(@href, '/edit')]")
        );
        editBtn.click();
        // ----------------------

        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("issueTitle")));
        input.clear();
        String newTitle = "Renamed Name " + System.currentTimeMillis();
        input.sendKeys(newTitle);
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        wait.until(ExpectedConditions.urlContains("/issues"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//td[contains(text(), '" + newTitle + "')]")
        ));
        String pageSource = driver.getPageSource();
        assertFalse(pageSource.contains(originalTitle), "L'ancien titre ne devrait plus être visible");
        assertTrue(pageSource.contains(newTitle), "Le nouveau titre devrait être affiché");
    }

    @Test
    public void testDragAndDropIssue() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        // 1. Préparation des colonnes (si nécessaire)
        driver.get(getBaseUrl() + "board");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("board")));

        int dropZonesCount = driver.findElements(By.className("kb-col-body")).size();
        if (dropZonesCount < 2) {
            driver.findElement(By.name("title")).sendKeys("Todo");
            driver.findElement(By.cssSelector("input[type='submit'][value='Ajouter Colonne']")).click();
            wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("board"), "Todo"));
        }

        // 2. Création de la Story
        driver.get(getBaseUrl() + "issues/new");
        String storyTitle = "DnD-" + System.currentTimeMillis();
        driver.findElement(By.name("title")).sendKeys(storyTitle);
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        wait.until(ExpectedConditions.urlContains("/issues"));

        // 3. Retour au Board via le bouton
        WebElement btnToBoard = wait.until(ExpectedConditions.elementToBeClickable(By.id("btn-to-board")));
        btnToBoard.click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("board")));

        // 4. Identification des éléments
        WebElement issueCard = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//article[contains(@class, 'issue-draggable') and .//div[contains(text(), '" + storyTitle + "')]]")
        ));

        List<WebElement> dropZones = driver.findElements(By.className("kb-col-body"));
        WebElement targetZone = dropZones.get(dropZones.size() - 1);

        // 5. Exécution du Drag & Drop
        simulateDragAndDrop(issueCard, targetZone);

        // 6. Vérification
        wait.until(ExpectedConditions.textToBePresentInElement(targetZone, storyTitle));
        assertTrue(targetZone.getText().contains(storyTitle), "La story doit avoir changé de colonne");
    }

    /**
     * Simule le Drag & Drop HTML5 en injectant directement du JavaScript.
     * L'API standard de Selenium (Actions.dragAndDrop) est historiquement instable avec les implémentations HTML5 modernes.
     * Elle échoue souvent à déclencher la chaîne complète d'événements (dragstart -> dragover -> drop).
     * Ce script garantit que tous les événements sont levés correctement pour que le navigateur réagisse.
     */

    private void simulateDragAndDrop(WebElement source, WebElement target) {
        String script =
                "var src = arguments[0], tgt = arguments[1];" +
                        "var dataTransfer = {" +
                        "   dropEffect: '', effectAllowed: 'all', files: [], items: {}, types: []," +
                        "   setData: function (format, data) { this.items[format] = data; this.types.push(format); }," +
                        "   getData: function (format) { return this.items[format]; }," +
                        "   clearData: function (format) { }" +
                        "};" +
                        "var emit = function (event, target) {" +
                        "   var evt = document.createEvent('CustomEvent');" +
                        "   evt.initCustomEvent(event, true, true, null);" +
                        "   evt.dataTransfer = dataTransfer;" +
                        "   target.dispatchEvent(evt);" +
                        "};" +
                        "emit('dragstart', src);" +
                        "emit('dragenter', tgt);" +
                        "emit('dragover', tgt);" +
                        "emit('drop', tgt);" +
                        "emit('dragend', src);";

        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(script, source, target);
    }
}