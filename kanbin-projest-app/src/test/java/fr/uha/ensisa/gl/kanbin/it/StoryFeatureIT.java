package fr.uha.ensisa.gl.kanbin.it;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StoryFeatureIT extends AbstractIT {

    @Test
    public void testCreateAndDeleteStory() {
        driver.get(getBaseUrl() + "issues/new");

        String fullTitle = "Story Test Selenium " + System.currentTimeMillis();
        String expectedTitle = fullTitle.length() > 30 ? fullTitle.substring(0, 30) : fullTitle;

        WebElement titleInput = driver.findElement(By.name("title"));
        WebElement submitBtn = driver.findElement(By.cssSelector("button[type='submit']"));

        titleInput.sendKeys(fullTitle);
        submitBtn.click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.urlContains("/issues"));

        // On vérifie que la carte est bien sur le board
        WebElement storyCard = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//article[contains(., '" + expectedTitle + "')]")
        ));
        assertTrue(storyCard.isDisplayed(), "La story doit être visible sur le board.");

        // 2. SUPPRESSION
        // On navigue vers la liste pour trouver facilement le bouton supprimer
        driver.get(getBaseUrl() + "issues");

        WebElement deleteBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//tr[td[contains(text(), '" + expectedTitle + "')]]//form//button")
        ));
        deleteBtn.click();

        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();
        wait.until(ExpectedConditions.invisibilityOf(deleteBtn));

        String pageSource = driver.getPageSource();
        assertNotNull(pageSource);
        assertFalse(pageSource.contains(expectedTitle), "La story devrait être supprimée.");
    }

    @Test
    public void testNavigateToBoard() {
        driver.get(getBaseUrl() + "issues");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        WebElement toBoardBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("btn-to-board")));
        toBoardBtn.click();
        wait.until(ExpectedConditions.urlContains("/board"));

        String title = driver.getTitle();
        String content = driver.getPageSource();

        boolean titleOk = title != null && title.contains("Board");
        boolean contentOk = content != null && content.contains("Kanbin");
        assertTrue(titleOk || contentOk, "Devrait être arrivé sur la page du Board");
    }

    @Test
    public void testRenameStory() {
        driver.get(getBaseUrl() + "issues/new");
        String originalTitle = "RenameTest " + System.currentTimeMillis();
        driver.findElement(By.name("title")).sendKeys(originalTitle);
        String expectedOriginal = originalTitle.length() > 30 ? originalTitle.substring(0, 30) : originalTitle;

        driver.findElement(By.name("title")).sendKeys(originalTitle);
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.urlContains("/board"));

        // 2. CLIC SUR LE CRAYON (Sur le board)
        // On cherche le lien 'edit' à l'intérieur de la carte qui a le bon titre
        WebElement editBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//article[contains(., '" + originalTitle + "')]//a[contains(@href, '/edit')]")
        ));
        editBtn.click();

        driver.findElement(
                By.xpath("//tr[td[contains(text(), '" + expectedOriginal + "')]]//a[contains(@href, '/edit')]")
        ).click();

        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("issueTitle")));
        input.clear();
        String newTitle = "Renamed Name " + System.currentTimeMillis();
        String expectedNew = newTitle.length() > 30 ? newTitle.substring(0, 30) : newTitle;
        input.sendKeys(newTitle);
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // 4. Vérif retour board
        wait.until(ExpectedConditions.urlContains("/board"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("board"), newTitle));
    }

    @Test
    public void testDragAndDropIssue() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        // 1. Préparation Board
        driver.get(getBaseUrl() + "board");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("board")));
        ensureColumnExists("Todo");

        // 2. Création Story
        createStory("DnD-" + System.currentTimeMillis());

        // 3. Retour Board
        driver.get(getBaseUrl() + "board");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("board")));

        // 4. Drag
        String storyTitle = "DnD-";
        WebElement issueCard = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//article[contains(@class, 'issue-draggable') and .//div[contains(text(), '" + storyTitle + "')]]")
        ));
        String fullTitle = issueCard.findElement(By.className("kb-card-title")).getText();

        java.util.List<WebElement> dropZones = driver.findElements(By.className("kb-col-body"));
        WebElement targetZone = dropZones.getLast();

        simulateDragAndDrop(issueCard, targetZone);

        wait.until((d) -> {
            // On récupère la liste fraîche des colonnes
            WebElement freshTarget = d.findElements(By.className("kb-col-body")).getLast();
            return freshTarget.getText().contains(fullTitle);
        });
        String finalStateText = driver.findElements(By.className("kb-col-body")).getLast().getText();
        assertTrue(finalStateText.contains(fullTitle));
    }

    @Test
    public void testDragAndDrop_blockedByWipLimit() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        try {
            // 1. Préparation PROPRE : On supprime la colonne si elle existe déjà (pollution)
            driver.get(getBaseUrl() + "board");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("board")));
            deleteColumnIfExists("Limited");

            // 2. Création fraîche de la colonne
            driver.findElement(By.name("title")).sendKeys("Limited");
            driver.findElement(By.cssSelector("input[type='submit'][value='Ajouter Colonne']")).click();
            wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("board"), "Limited"));

            // 3. Clic ÉDITER (Ciblage par classe btn-warning)
            WebElement editBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//section[contains(., 'Limited')]//header//a[contains(@class, 'btn-warning')]")
            ));
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", editBtn);
            editBtn.click();

            // 4. Config Limite à 1
            WebElement limitInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("wipLimit")));
            limitInput.clear();
            limitInput.sendKeys("1");
            driver.findElement(By.cssSelector("button[type='submit']")).click();

            // Attente retour board
            wait.until(ExpectedConditions.urlContains("/board"));

            // VERIFICATION CRITIQUE : On attend de voir le badge "/ 1"
            // Si cette ligne échoue, c'est que la limite n'est pas sauvegardée.
            wait.until(ExpectedConditions.textToBePresentInElementLocated(
                    By.xpath("//section[contains(., 'Limited')]//header"), "/ 1"
            ));

            // 5. Création Stories
            createStory("Story-Blocker");
            createStory("Story-Mover");

            // 6. Remplissage (1/1)
            driver.get(getBaseUrl() + "board");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("board")));

            WebElement blocker = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//article[contains(., 'Story-Blocker')]")
            ));
            WebElement limitedColBody = driver.findElement(
                    By.xpath("//section[contains(., 'Limited')]//div[contains(@class, 'kb-col-body')]")
            );

            simulateDragAndDrop(blocker, limitedColBody);
            wait.until(ExpectedConditions.textToBePresentInElement(limitedColBody, "Story-Blocker"));

            // 7. Tentative Dépassement
            WebElement mover = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//article[contains(., 'Story-Mover')]")
            ));
            simulateDragAndDrop(mover, limitedColBody);

            // 8. Vérif Alerte
            wait.until(ExpectedConditions.alertIsPresent());
            Alert alert = driver.switchTo().alert();
            String alertText = alert.getText();

            assertFalse(alertText.contains("supprimer"), "ERREUR: Le test a cliqué sur Supprimer au lieu d'Éditer !");
            assertTrue(alertText.contains("Limite atteinte"), "Message incorrect: " + alertText);
            alert.accept();

            // 9. Vérif Finale (Rollback)
            try { Thread.sleep(500); } catch (Exception ignored) {}
            assertFalse(limitedColBody.getText().contains("Story-Mover"), "La limite a été ignorée !");

        } finally {
            // NETTOYAGE : On supprime la colonne pour laisser le board propre
            deleteColumnIfExists("Limited");
        }
    }

    // --- HELPERS ---

    private void ensureColumnExists(String title) {
        if (driver.findElements(By.xpath("//span[contains(text(), '" + title + "')]")).isEmpty()) {
            driver.findElement(By.name("title")).sendKeys(title);
            driver.findElement(By.cssSelector("input[type='submit'][value='Ajouter Colonne']")).click();
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.textToBePresentInElementLocated(By.id("board"), title));
        }
    }

    private void deleteColumnIfExists(String title) {
        try {
            driver.get(getBaseUrl() + "board");
            // On cherche la section. Si elle existe, on cherche le bouton supprimer dedans.
            java.util.List<WebElement> cols = driver.findElements(By.xpath("//section[contains(., '" + title + "')]"));
            if (!cols.isEmpty()) {
                WebElement deleteBtn = cols.getFirst().findElement(
                        By.xpath(".//form[contains(@action, 'remove-column')]//button")
                );
                // Clic forcé JS pour éviter les obstructions
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", deleteBtn);

                // Gestion de l'alerte de confirmation de suppression
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
                wait.until(ExpectedConditions.alertIsPresent());
                driver.switchTo().alert().accept();

                // Attente disparition
                wait.until(ExpectedConditions.invisibilityOf(cols.getFirst()));
            }
        } catch (Exception e) {
            System.out.println("Info: Nettoyage de la colonne '" + title + "' ignoré ou échoué (pas grave).");
        }
    }

    private void createStory(String title) {
        driver.get(getBaseUrl() + "issues/new");
        try { driver.switchTo().alert().accept(); } catch (Exception ignored) {}

        driver.findElement(By.name("title")).sendKeys(title);
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Attente robuste : on attend que le titre soit visible dans la liste
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.urlContains("/board"));
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

    @Test
    public void testUpdateStoryDetail() {
        driver.get(getBaseUrl() + "issues/new");
        String title = "DetailTest " + System.currentTimeMillis();
        driver.findElement(By.name("title")).sendKeys(title);
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.urlContains("/board"));

        // 2. CLIC SUR LE CRAYON
        WebElement editBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//article[contains(., '" + title + "')]//a[contains(@href, '/edit')]")
        ));
        editBtn.click();

        String newDetail = "Ceci est une description détaillée mise à jour.";
        WebElement detailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("detail")));
        detailInput.clear();
        detailInput.sendKeys(newDetail);
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // 4. Vérif
        wait.until(ExpectedConditions.urlContains("/board"));
        WebElement card = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//article[contains(., '" + title + "')]")
        ));
        assertTrue(card.getText().contains(newDetail));
    }
}