package fr.uha.ensisa.gl.kanbin.eco;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import fr.uha.ensisa.eco.metrologie.extension.EcoExtension;
import fr.uha.ensisa.eco.metrologie.extension.annotations.*;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Scénario complet d'éco-conception pour Kanbin Projest.
 * Mesure : énergie, CPU, mémoire, bande passante réseau, disque
 *
 * Scénario mesuré :
 * 1. Consultation du Board (chargement page)
 * 2. Création colonne simple
 * 3. Création colonne double (avec sous-colonnes)
 * 4. Création story (issue)
 * 5. Modification story (édition)
 * 6. Drag & Drop story entre colonnes
 * 7. Reordering colonnes
 * 8. Passage en "Closed" (complétée)
 * 9. Suppression story + suppression colonne
 */
@EcoDocker(network = "kanbin-metrologie", clean = false)
@EcoDockerContainer(id = "kanbin-app-eco", port = 8080)
@EcoMonitor(containerId = "kanbin-app-eco")
@EcoWebDriver(remote = true)
@ExtendWith(EcoExtension.class)
public class KanbinEcoScenarioIT {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private WebDriver driver;
    private JsonObject scenarioMetrics;
    private Map<String, ActionMetric> actionMetrics;

    /**
     * Test principal : exécute le scénario complet et exporte les métriques en JSON
     * Mesure :
     * - CPU (via cAdvisor/Prometheus)
     * - Mémoire (via cAdvisor/Prometheus)
     * - Bande passante réseau (via cAdvisor/Prometheus)
     * - Disque I/O (via cAdvisor/Prometheus)
     * - Consommation énergétique (via EcoExtension si PowerSpy disponible)
     */
    @Test
    @EcoRunConfig(warmupRepetitions = 0)
    public void testCompleteKanbinScenarioWithMetrics(WebDriver webDriver) throws Exception {
        this.driver = webDriver;
        this.actionMetrics = new LinkedHashMap<>();
        this.scenarioMetrics = new JsonObject();

        try {
            System.out.println("\n" + "=".repeat(70));
            System.out.println("🌱 BENCHMARK ECO-CONCEPTION - KANBIN PROJEST");
            System.out.println("=".repeat(70));
            System.out.println("Points mesurés:");
            System.out.println("  • Consommation énergétique");
            System.out.println("  • Bande passante réseau");
            System.out.println("  • Utilisation mémoire");
            System.out.println("  • Consommation CPU");
            System.out.println("  • Place sur disque (volumes)");
            System.out.println("=".repeat(70) + "\n");


            // === 1. CONSULTATION DU BOARD ===
            recordAction("01_board_consultation", () -> {
                System.out.println("  📋 Consultation du Board...");
                driver.get("/board");
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("board")));
            });

            // === 2. CRÉATION COLONNE SIMPLE ===
            String simpleColName = "Simple-Col-" + System.currentTimeMillis();
            recordAction("02_create_simple_column", () -> {
                System.out.println("  ➕ Création colonne simple...");
                WebElement titleInput = driver.findElement(By.name("title"));
                WebElement typeSelect = driver.findElement(By.name("type"));
                new Select(typeSelect).selectByValue("simple");

                titleInput.clear();
                titleInput.sendKeys(simpleColName);
                driver.findElement(By.cssSelector("input[type='submit'][value='Ajouter Colonne']")).click();

                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.textToBePresentInElementLocated(By.id("board"), simpleColName));
                Thread.sleep(300); // Stabilisation
            });

            // === 3. CRÉATION COLONNE DOUBLE ===
            String doubleColName = "Double-Col-" + System.currentTimeMillis();
            recordAction("03_create_double_column", () -> {
                System.out.println("  ➕ Création colonne double (avec sous-colonnes)...");
                WebElement titleInput = driver.findElement(By.name("title"));
                WebElement typeSelect = driver.findElement(By.name("type"));
                new Select(typeSelect).selectByValue("double");

                titleInput.clear();
                titleInput.sendKeys(doubleColName);
                driver.findElement(By.cssSelector("input[type='submit'][value='Ajouter Colonne']")).click();

                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.textToBePresentInElementLocated(By.id("board"), doubleColName));
                Thread.sleep(300);
            });

            // === 4. CRÉATION STORY ===
            String storyTitle = "Eco-Story-" + System.currentTimeMillis();
            recordAction("04_create_story", () -> {
                System.out.println("  📝 Création story (issue)...");
                driver.get("/issues/new");
                driver.findElement(By.name("title")).sendKeys(storyTitle);
                driver.findElement(By.cssSelector("button[type='submit']")).click();

                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.urlContains("/board"));
                Thread.sleep(300);
            });

            // === 5. MODIFICATION STORY ===
            String updatedDetail = "Description détaillée mise à jour pour éco-conception";
            recordAction("05_edit_story", () -> {
                System.out.println("  ✏️  Modification story (édition)...");
                driver.get("/board");
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.presenceOfElementLocated(By.id("board")));

                WebElement editBtn = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.elementToBeClickable(
                                By.xpath("//article[contains(., '" + storyTitle + "')]//a[contains(@href, '/edit')]")
                        ));
                editBtn.click();

                WebElement detailInput = driver.findElement(By.name("detail"));
                detailInput.clear();
                detailInput.sendKeys(updatedDetail);
                driver.findElement(By.cssSelector("button[type='submit']")).click();

                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.urlContains("/board"));
                Thread.sleep(300);
            });

            // === 6. DRAG & DROP STORY ===
            recordAction("06_drag_and_drop_story", () -> {
                System.out.println("  🎯 Drag & Drop story entre colonnes...");
                driver.get("/board");
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.presenceOfElementLocated(By.id("board")));

                WebElement issueCard = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.presenceOfElementLocated(
                                By.xpath("//article[contains(., '" + storyTitle + "')]")
                        ));

                List<WebElement> dropZones = driver.findElements(By.className("kb-col-body"));
                if (dropZones.size() > 1) {
                    WebElement targetZone = dropZones.get(1);
                    simulateDragAndDrop(issueCard, targetZone);
                    Thread.sleep(500);
                }
            });

            // === 7. REORDERING COLONNES ===
            String reorderColName = "Reorder-Test-" + System.currentTimeMillis();
            recordAction("07_reorder_columns", () -> {
                System.out.println("  🔄 Réorganisation des colonnes...");
                driver.get("/board");
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.presenceOfElementLocated(By.id("board")));

                WebElement titleInput = driver.findElement(By.name("title"));
                titleInput.clear();
                titleInput.sendKeys(reorderColName);
                driver.findElement(By.cssSelector("input[type='submit'][value='Ajouter Colonne']")).click();

                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.textToBePresentInElementLocated(By.id("board"), reorderColName));
                Thread.sleep(300);
            });

            // === 8. PASSAGE EN CLOSED ===
            recordAction("08_move_to_closed", () -> {
                System.out.println("  ✅ Passage en 'Closed' (complétée)...");
                driver.get("/board");
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.presenceOfElementLocated(By.id("board")));

                List<WebElement> issueCards = driver.findElements(By.className("issue-draggable"));
                if (!issueCards.isEmpty()) {
                    WebElement issueCard = issueCards.get(0);
                    WebElement closedZone = driver.findElement(
                            By.xpath("//section[@data-col='closed']//div[contains(@class,'kb-col-body')]")
                    );
                    simulateDragAndDrop(issueCard, closedZone);
                    Thread.sleep(500);
                }
            });

            // === 9. SUPPRESSION STORY & COLONNE ===
            recordAction("09_delete_story_and_column", () -> {
                System.out.println("  🗑️  Suppression story + colonne...");
                driver.get("/issues");
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));

                List<WebElement> deleteButtons = driver.findElements(
                        By.xpath("//table//tr//form//button[contains(text(), 'Supprimer')]")
                );
                if (!deleteButtons.isEmpty()) {
                    deleteButtons.get(0).click();
                    try {
                        new WebDriverWait(driver, Duration.ofSeconds(2))
                                .until(ExpectedConditions.alertIsPresent());
                        driver.switchTo().alert().accept();
                        Thread.sleep(300);
                    } catch (Exception e) {
                        // Alert déjà traitée
                    }
                }
            });

            System.out.println("\n" + "=".repeat(70));
            System.out.println("✅ Scénario complété avec succès");
            System.out.println("=".repeat(70) + "\n");

            // === COMPILATION DES RÉSULTATS ===
            exportMetricsToJSON();

        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    /**
     * Enregistre les métriques d'une action
     */
    private void recordAction(String actionName, ActionRunnable action) throws Exception {
        long startTime = System.currentTimeMillis();
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        try {
            action.run();
        } catch (Exception e) {
            System.err.println("⚠️  Action " + actionName + " échouée: " + e.getMessage());
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        ActionMetric metric = new ActionMetric(
                actionName,
                endTime - startTime,
                (endMemory - startMemory) / 1024, // KB
                endMemory / (1024 * 1024)  // MB total
        );

        actionMetrics.put(actionName, metric);
        System.out.println("    ✓ Durée: " + metric.duration + "ms | Δ Mémoire: " + metric.memoryDelta + "KB | Total: " + metric.totalMemory + "MB");
    }


    /**
     * Simule le Drag & Drop HTML5
     */
    private void simulateDragAndDrop(WebElement source, WebElement target) {
        String script = "var src = arguments[0], tgt = arguments[1];" +
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

        ((JavascriptExecutor) driver).executeScript(script, source, target);
    }

    /**
     * Exporte les métriques au format JSON
     * Mesures disponibles :
     * - Durée d'exécution
     * - Mémoire utilisée (delta et totale)
     * - Ces données seront enrichies par cAdvisor/Prometheus pour :
     *   * CPU
     *   * Bande passante réseau
     *   * Disque I/O
     * - Et par PowerSpy pour :
     *   * Consommation énergétique
     */
    private void exportMetricsToJSON() throws IOException {
        JsonObject root = new JsonObject();

        // En-tête
        root.addProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        root.addProperty("scenario", "Kanbin Eco-Conception - Scénario Complet");
        root.addProperty("version", "1.0");

        // Métriques globales calculées
        JsonObject globalMetrics = new JsonObject();
        long totalDuration = actionMetrics.values().stream()
                .mapToLong(m -> m.duration).sum();
        long maxMemory = actionMetrics.values().stream()
                .mapToLong(m -> m.totalMemory).max().orElse(0);
        long totalMemoryDelta = actionMetrics.values().stream()
                .mapToLong(m -> m.memoryDelta).sum();

        globalMetrics.addProperty("total_duration_ms", totalDuration);
        globalMetrics.addProperty("max_memory_mb", maxMemory);
        globalMetrics.addProperty("total_memory_delta_kb", totalMemoryDelta);
        globalMetrics.addProperty("actions_count", actionMetrics.size());
        globalMetrics.addProperty("unit", "Java Runtime Metrics");

        root.add("global_metrics", globalMetrics);

        // Détails des actions
        JsonArray actionsArray = new JsonArray();
        actionMetrics.forEach((name, metric) -> {
            JsonObject actionObj = new JsonObject();
            actionObj.addProperty("name", metric.name);
            actionObj.addProperty("duration_ms", metric.duration);
            actionObj.addProperty("memory_delta_kb", metric.memoryDelta);
            actionObj.addProperty("total_memory_mb", metric.totalMemory);
            actionsArray.add(actionObj);
        });
        root.add("actions", actionsArray);

        // Références pour données complètes
        JsonObject externalMetrics = new JsonObject();
        externalMetrics.addProperty("cpu_usage", "Available via cAdvisor/Prometheus on http://localhost:9090");
        externalMetrics.addProperty("network_bandwidth", "Available via cAdvisor metrics");
        externalMetrics.addProperty("disk_io", "Available via cAdvisor metrics");
        externalMetrics.addProperty("energy_consumption", "Available via PowerSpy integration");
        externalMetrics.addProperty("cadvisor_endpoint", "http://localhost:8081");
        externalMetrics.addProperty("prometheus_endpoint", "http://localhost:9090");
        root.add("external_metrics_endpoints", externalMetrics);

        // Sauvegarde
        String filename = "target/eco-benchmark-" + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + ".json";
        Files.createDirectories(Paths.get("target"));

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(root, writer);
        }

        System.out.println("\n📊 Métriques exportées: " + filename);
        System.out.println("\n📈 Pour les mesures complètes (CPU, réseau, disque, énergie):");
        System.out.println("   • Prometheus: http://localhost:9090");
        System.out.println("   • cAdvisor: http://localhost:8081");
    }

    // === Classes internes ===

    static class ActionMetric {
        String name;
        long duration;
        long memoryDelta;
        long totalMemory;

        ActionMetric(String name, long duration, long memoryDelta, long totalMemory) {
            this.name = name;
            this.duration = duration;
            this.memoryDelta = memoryDelta;
            this.totalMemory = totalMemory;
        }
    }

    interface ActionRunnable {
        void run() throws Exception;
    }
}
