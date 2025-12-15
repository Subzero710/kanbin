package fr.uha.ensisa.gl.kanbin.it;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import org.openqa.selenium.support.ui.Select;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BoardFeatureIT extends AbstractIT {

    @Test
    public void testDefaultBoardHasBacklogAndClosedColumns() {
        driver.get(getBaseUrl() + "board");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("board")));

        java.util.List<WebElement> columns =
                driver.findElements(By.cssSelector("#board section[data-col]"));

        assertTrue(columns.size() >= 2,
                "Le board doit contenir au moins deux colonnes système (Backlog et Closed).");

        int backlogIndex = -1;
        int closedIndex = -1;

        for (int i = 0; i < columns.size(); i++) {
            String key = columns.get(i).getAttribute("data-col");
            if ("backlog".equals(key)) {
                backlogIndex = i;
            } else if ("closed".equals(key)) {
                closedIndex = i;
            }
        }

        assertTrue(backlogIndex != -1, "La colonne Backlog doit être présente.");
        assertTrue(closedIndex != -1, "La colonne Closed doit être présente.");
        assertTrue(backlogIndex < closedIndex,
                "La colonne Backlog doit apparaître avant Closed.");
    }

    @Test
    public void testNewColumnsAreInsertedBeforeClosed() {
        driver.get(getBaseUrl() + "board");

        String newColumnTitle = "Inserted Before Closed " + System.currentTimeMillis();
        // Le champ HTML a maxlength="30", donc le titre sera tronqué côté navigateur.
        String expectedTitle = newColumnTitle.length() > 30
                ? newColumnTitle.substring(0, 30)
                : newColumnTitle;
        WebElement titleInput = driver.findElement(By.name("title"));
        WebElement typeSelectElement = driver.findElement(By.name("type"));
        Select typeSelect = new Select(typeSelectElement);
        typeSelect.selectByValue("simple");

        WebElement submitButton = driver.findElement(
                By.cssSelector("input[type='submit'][value='Ajouter Colonne']"));
        titleInput.sendKeys(newColumnTitle);
        submitButton.click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        // On attend que le texte de la nouvelle colonne soit présent dans le board
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.id("board"),
                expectedTitle
        ));
        java.util.List<WebElement> columns =
                driver.findElements(By.cssSelector("#board section[data-col]"));

        int newIndex = -1;
        int closedIndex = -1;

        for (int i = 0; i < columns.size(); i++) {
            WebElement col = columns.get(i);
            String key = col.getAttribute("data-col");
            String fullText = col.getText();

            if (fullText.contains(expectedTitle)) {
                newIndex = i;
            }
            if ("closed".equals(key)) {
                closedIndex = i;
            }
        }

        assertTrue(newIndex != -1, "La nouvelle colonne doit être présente.");
        assertTrue(closedIndex != -1, "La colonne Closed doit être présente.");
        assertTrue(newIndex < closedIndex,
                "Les colonnes créées manuellement doivent être insérées avant Closed.");
    }

    @Test
    public void testAddColumnFeature() {
        driver.get(getBaseUrl() + "board");
        String newColumnTitle = "Test Integration Colonne";
        String expectedKey = newColumnTitle.toLowerCase().replaceAll("\\s+", "-");

        WebElement titleInput = driver.findElement(By.name("title"));

        // --- AJOUT : Sélection du type "Simple" pour garder le comportement d'avant ---
        WebElement typeSelectElement = driver.findElement(By.name("type"));
        Select typeSelect = new Select(typeSelectElement);
        typeSelect.selectByValue("simple");
        // -----------------------------------------------------------------------------

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
    public void testAddColumnDoubleFeature() {
        driver.get(getBaseUrl() + "board");
        String newColumnTitle = "Test Colonne Double";
        String expectedKey = newColumnTitle.toLowerCase().replaceAll("\\s+", "-");

        WebElement titleInput = driver.findElement(By.name("title"));
        WebElement typeSelectElement = driver.findElement(By.name("type"));
        Select typeSelect = new Select(typeSelectElement);
        typeSelect.selectByValue("double");

        WebElement submitButton = driver.findElement(By.cssSelector("input[type='submit'][value='Ajouter Colonne']"));

        titleInput.sendKeys(newColumnTitle);
        submitButton.click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        WebElement newColumnHeader = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.xpath(
                        "//section[@data-col='" + expectedKey + "']//span[contains(text(), '" + newColumnTitle + "')]"
                ))
        );
        assertTrue(newColumnHeader.isDisplayed(), "Le titre de la colonne double doit s'afficher.");
        boolean hasTodo = !driver.findElements(By.xpath("//section[@data-col='" + expectedKey + "']//span[contains(text(), 'À Faire')]")).isEmpty();
        boolean hasWip = !driver.findElements(By.xpath("//section[@data-col='" + expectedKey + "']//span[contains(text(), 'En Cours')]")).isEmpty();

        assertTrue(hasTodo, "La sous-colonne 'À Faire' doit être présente.");
        assertTrue(hasWip, "La sous-colonne 'En Cours' doit être présente.");
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

    /**
     * Helper pour créer une colonne rapidement.
     * Remplit le formulaire et attend que la colonne apparaisse.
     */
    private void createColumnHelper(String title) {
        // 1. Remplir le titre
        WebElement titleInput = driver.findElement(By.name("title"));
        titleInput.clear();
        titleInput.sendKeys(title);

        // 2. Valider (On suppose que le type est "Simple" par défaut)
        driver.findElement(By.cssSelector("input[type='submit'][value='Ajouter Colonne']")).click();

        // 3. Attendre que la colonne soit créée pour ne pas aller trop vite
        // Cela évite que le test enchaîne sur le Drag & Drop alors que la colonne n'est pas encore dans le DOM
        new WebDriverWait(driver, Duration.ofSeconds(2))
                .until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//span[contains(text(), '" + title + "')]")));
    }
    @Test
    public void testReorderColumnFeature() {
        driver.get(getBaseUrl() + "board");

        // 1. Préparation : Création de 2 colonnes avec des noms uniques
        long timestamp = System.currentTimeMillis();
        String nameA = "DRAG-A-" + timestamp;
        String nameB = "DRAG-B-" + timestamp;

        createColumnHelper(nameA);
        createColumnHelper(nameB);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        // 2. Localisation des éléments
        // On a besoin de la SECTION (pour le drag) et du HEADER (pour le unlock du clic)
        WebElement sectionA = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//span[contains(text(), '" + nameA + "')]/ancestor::section")));

        WebElement sectionB = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//span[contains(text(), '" + nameB + "')]/ancestor::section")));

        WebElement headerB = sectionB.findElement(By.cssSelector(".kb-col-header"));

        // 3. Action : Drag B sur A (B doit passer devant A)
        // On passe headerB (pour le clic) et les sections (pour le mouvement)
        performDragAndDropJS(headerB, sectionB, sectionA);

        // 4. Vérification
        // Petit délai de stabilisation pour le DOM
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        var allTitles = driver.findElements(By.cssSelector(".kb-col-header span[id^='col-title-']"));

        int indexA = -1;
        int indexB = -1;

        for (int i = 0; i < allTitles.size(); i++) {
            String txt = allTitles.get(i).getText();
            if (txt.contains(nameA)) indexA = i;
            if (txt.contains(nameB)) indexB = i;
        }

        assertTrue(indexB != -1 && indexA != -1, "Les colonnes doivent être présentes dans le DOM");
        assertTrue(indexB < indexA, "La colonne B (" + indexB + ") doit être placée avant A (" + indexA + ")");
    }

    /**
     * Simule un Drag & Drop HTML5 complet.
     * Contourne le bug de Selenium Actions avec HTML5 draggable.
     * Simule d'abord un MOUSEDOWN sur le header pour valider la sécurité du project.js,
     * puis enchaîne les événements Drag & Drop standards sur les sections.
     */
    private void performDragAndDropJS(WebElement srcHeader, WebElement srcSection, WebElement tgtSection) {
        String script =
                "var srcHeader = arguments[0]; " +
                        "var srcSection = arguments[1]; " +
                        "var tgtSection = arguments[2]; " +

                        // 1. Scroll pour s'assurer que les éléments sont interactifs (viewport)
                        "srcSection.scrollIntoView({block: 'center', inline: 'center'}); " +

                        // 2. SIMULATION DU CLIC (MOUSEDOWN)
                        // Indispensable pour passer le check 'if (!isCursorInHeader)' de project.js
                        "var evtMouseDown = new MouseEvent('mousedown', { bubbles: true, cancelable: true, view: window }); " +
                        "srcHeader.dispatchEvent(evtMouseDown); " +

                        // 3. PRÉPARATION DU DRAG
                        "var dataTransfer = new DataTransfer(); " +

                        // 4. DRAG START (Sur la SECTION)
                        "var evtDragStart = new DragEvent('dragstart', { bubbles: true, cancelable: true, view: window }); " +
                        "Object.defineProperty(evtDragStart, 'dataTransfer', { value: dataTransfer }); " +
                        "srcSection.dispatchEvent(evtDragStart); " +

                        // 5. DROP (Sur la SECTION cible)
                        "var evtDrop = new DragEvent('drop', { bubbles: true, cancelable: true, view: window }); " +
                        "Object.defineProperty(evtDrop, 'dataTransfer', { value: dataTransfer }); " +
                        "tgtSection.dispatchEvent(evtDrop); " +

                        // 6. DRAG END
                        "var evtDragEnd = new DragEvent('dragend', { bubbles: true, cancelable: true, view: window }); " +
                        "Object.defineProperty(evtDragEnd, 'dataTransfer', { value: dataTransfer }); " +
                        "srcSection.dispatchEvent(evtDragEnd);";

        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(script, srcHeader, srcSection, tgtSection);
    }


}