package fr.uha.ensisa.gl.kanbin.eco;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.uha.ensisa.eco.metrologie.extension.EcoExtension;
import fr.uha.ensisa.eco.metrologie.extension.annotations.EcoDocker;
import fr.uha.ensisa.eco.metrologie.extension.annotations.EcoDockerContainer;
import fr.uha.ensisa.eco.metrologie.extension.annotations.EcoGatling;
import fr.uha.ensisa.eco.metrologie.extension.annotations.EcoRunConfig;
import fr.uha.ensisa.eco.metrologie.extension.annotations.EcoWebDriver;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@EcoDocker(network = "kanbin-metrologie", clean = true)
@EcoDockerContainer(id = "kanbin-app-eco", port = 8080)
@EcoWebDriver(remote = true)
@EcoGatling(userCount = 20, rampDuration = 10)
@ExtendWith(EcoExtension.class)
public class KanbinEcoScenarioIT {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private WebDriver driver;
    private Map<String, ActionMetric> actionMetrics;
    private final PrometheusMetricsClient prometheusMetricsClient = new PrometheusMetricsClient();

    @RepeatedTest(3)
    @EcoRunConfig(initIdle = 5000, minIdle = 2000)
    public void testCompleteKanbinScenarioWithMetrics(WebDriver webDriver) throws Exception {
        this.driver = webDriver;
        this.actionMetrics = new LinkedHashMap<>();

        System.out.println("\n" + "=".repeat(70));
        System.out.println("🌱 BENCHMARK ECO-CONCEPTION - KANBIN PROJEST");
        System.out.println("=".repeat(70));
        System.out.println("Mode benchmark :");
        System.out.println("  • Selenium piloté par EcoExtension");
        System.out.println("  • Mesures conteneur via Prometheus/cAdvisor");
        System.out.println("  • Montée en charge via EcoGatling");
        System.out.println("=".repeat(70) + "\n");

        String simpleColName = "Simple-Col-" + System.currentTimeMillis();
        String doubleColName = "Double-Col-" + (System.currentTimeMillis() + 1);
        String storyTitle = "Eco-Story-" + (System.currentTimeMillis() + 2);
        String updatedDetail = "Description détaillée mise à jour pour éco-conception";
        String reorderColName = "Reorder-Test-" + (System.currentTimeMillis() + 3);

        recordAction("01_board_consultation", () -> {
            driver.get("/board");
            waitFor(By.id("board"));
        });

        recordAction("02_create_simple_column", () -> {
            WebElement titleInput = driver.findElement(By.name("title"));
            WebElement typeSelect = driver.findElement(By.name("type"));
            new Select(typeSelect).selectByValue("simple");

            titleInput.clear();
            titleInput.sendKeys(simpleColName);
            driver.findElement(By.cssSelector("input[type='submit'][value='Ajouter Colonne']")).click();

            waitUntilTextInBoard(simpleColName);
            sleep(300);
        });

        recordAction("03_create_double_column", () -> {
            WebElement titleInput = driver.findElement(By.name("title"));
            WebElement typeSelect = driver.findElement(By.name("type"));
            new Select(typeSelect).selectByValue("double");

            titleInput.clear();
            titleInput.sendKeys(doubleColName);
            driver.findElement(By.cssSelector("input[type='submit'][value='Ajouter Colonne']")).click();

            waitUntilTextInBoard(doubleColName);
            sleep(300);
        });

        recordAction("04_create_story", () -> {
            driver.get("/issues/new");
            driver.findElement(By.name("title")).sendKeys(storyTitle);
            driver.findElement(By.cssSelector("button[type='submit']")).click();

            waitForUrlContains("/board");
            sleep(300);
        });

        recordAction("05_edit_story", () -> {
            driver.get("/board");
            waitFor(By.id("board"));

            WebElement editBtn = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//article[contains(., '" + storyTitle + "')]//a[contains(@href, '/edit')]")
                    ));
            editBtn.click();

            WebElement detailInput = driver.findElement(By.name("detail"));
            detailInput.clear();
            detailInput.sendKeys(updatedDetail);
            driver.findElement(By.cssSelector("button[type='submit']")).click();

            waitForUrlContains("/board");
            sleep(300);
        });

        recordAction("06_drag_and_drop_story", () -> {
            driver.get("/board");
            waitFor(By.id("board"));

            WebElement issueCard = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//article[contains(., '" + storyTitle + "')]")
                    ));

            List<WebElement> dropZones = driver.findElements(By.className("kb-col-body"));
            if (dropZones.size() > 1) {
                simulateDragAndDrop(issueCard, dropZones.get(1));
                sleep(500);
            }
        });

        recordAction("07_reorder_columns", () -> {
            driver.get("/board");
            waitFor(By.id("board"));

            WebElement titleInput = driver.findElement(By.name("title"));
            titleInput.clear();
            titleInput.sendKeys(reorderColName);
            driver.findElement(By.cssSelector("input[type='submit'][value='Ajouter Colonne']")).click();

            waitUntilTextInBoard(reorderColName);
            sleep(300);
        });

        recordAction("08_move_to_closed", () -> {
            driver.get("/board");
            waitFor(By.id("board"));

            List<WebElement> issueCards = driver.findElements(By.className("issue-draggable"));
            if (!issueCards.isEmpty()) {
                WebElement issueCard = issueCards.get(0);
                WebElement closedZone = driver.findElement(
                        By.xpath("//section[@data-col='closed']//div[contains(@class,'kb-col-body')]")
                );
                simulateDragAndDrop(issueCard, closedZone);
                sleep(500);
            }
        });

        recordAction("09_delete_story_and_column", () -> {
            driver.get("/issues");
            waitFor(By.tagName("table"));

            List<WebElement> deleteButtons = driver.findElements(
                    By.xpath("//table//tr//form//button[contains(text(), 'Supprimer')]")
            );
            if (!deleteButtons.isEmpty()) {
                deleteButtons.get(0).click();
                try {
                    new WebDriverWait(driver, Duration.ofSeconds(2))
                            .until(ExpectedConditions.alertIsPresent());
                    driver.switchTo().alert().accept();
                    sleep(300);
                } catch (Exception ignored) {
                }
            }
        });

        exportMetricsToJson();
    }

    private void recordAction(String actionName, ActionRunnable action) throws Exception {
        Instant startedAt = Instant.now();
        long startMemory = usedJvmMemoryBytes();

        try {
            action.run();
        } catch (Exception e) {
            System.err.println("⚠️ Action " + actionName + " échouée: " + e.getMessage());
            System.err.println("🔍 URL ACTUELLE : " + driver.getCurrentUrl());
            System.err.println("📄 CONTENU DE LA PAGE : \n" + driver.getPageSource());
            throw e;
        }

        Instant endedAt = Instant.now();
        long endMemory = usedJvmMemoryBytes();

        PrometheusMetricsClient.ActionContainerMetrics containerMetrics =
                prometheusMetricsClient.collectForWindow(startedAt, endedAt);

        ActionMetric metric = new ActionMetric(
                actionName,
                startedAt.toEpochMilli(),
                endedAt.toEpochMilli(),
                Duration.between(startedAt, endedAt).toMillis(),
                (endMemory - startMemory) / 1024,
                endMemory / (1024 * 1024),
                containerMetrics.cpuSeconds,
                containerMetrics.memoryAvgMb,
                containerMetrics.memoryPeakMb,
                containerMetrics.networkRxBytes,
                containerMetrics.networkTxBytes
        );

        actionMetrics.put(actionName, metric);
        System.out.printf(
                "✓ %s | %d ms | ΔJVM %d KB | JVM %d MB | CPU %.4f s | Mem avg %.2f MB | Mem peak %.2f MB | RX %.0f B | TX %.0f B%n",
                metric.name,
                metric.durationMs,
                metric.jvmMemoryDeltaKb,
                metric.jvmTotalMemoryMb,
                metric.cpuSeconds,
                metric.containerMemoryAvgMb,
                metric.containerMemoryPeakMb,
                metric.networkRxBytes,
                metric.networkTxBytes
        );
    }

    private void exportMetricsToJson() throws IOException {
        JsonObject root = new JsonObject();

        root.addProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        root.addProperty("scenario", "Kanbin Eco-Conception - Scénario Complet");
        root.addProperty("version", "2.0");

        JsonObject globalMetrics = new JsonObject();
        long totalDurationMs = actionMetrics.values().stream().mapToLong(m -> m.durationMs).sum();
        long maxJvmMemoryMb = actionMetrics.values().stream().mapToLong(m -> m.jvmTotalMemoryMb).max().orElse(0L);
        long totalJvmMemoryDeltaKb = actionMetrics.values().stream().mapToLong(m -> m.jvmMemoryDeltaKb).sum();
        double totalCpuSeconds = actionMetrics.values().stream().mapToDouble(m -> Math.max(0d, m.cpuSeconds)).sum();
        double maxContainerMemoryPeakMb = actionMetrics.values().stream().mapToDouble(m -> Math.max(0d, m.containerMemoryPeakMb)).max().orElse(0d);
        double totalNetworkRxBytes = actionMetrics.values().stream().mapToDouble(m -> Math.max(0d, m.networkRxBytes)).sum();
        double totalNetworkTxBytes = actionMetrics.values().stream().mapToDouble(m -> Math.max(0d, m.networkTxBytes)).sum();

        globalMetrics.addProperty("total_duration_ms", totalDurationMs);
        globalMetrics.addProperty("max_jvm_memory_mb", maxJvmMemoryMb);
        globalMetrics.addProperty("total_jvm_memory_delta_kb", totalJvmMemoryDeltaKb);
        globalMetrics.addProperty("total_cpu_seconds", totalCpuSeconds);
        globalMetrics.addProperty("max_container_memory_peak_mb", maxContainerMemoryPeakMb);
        globalMetrics.addProperty("total_network_rx_bytes", totalNetworkRxBytes);
        globalMetrics.addProperty("total_network_tx_bytes", totalNetworkTxBytes);
        globalMetrics.addProperty("actions_count", actionMetrics.size());
        root.add("global_metrics", globalMetrics);

        JsonArray actionsArray = new JsonArray();
        actionMetrics.values().forEach(metric -> {
            JsonObject actionObj = new JsonObject();
            actionObj.addProperty("name", metric.name);
            actionObj.addProperty("started_at_epoch_ms", metric.startedAtEpochMs);
            actionObj.addProperty("ended_at_epoch_ms", metric.endedAtEpochMs);
            actionObj.addProperty("duration_ms", metric.durationMs);
            actionObj.addProperty("jvm_memory_delta_kb", metric.jvmMemoryDeltaKb);
            actionObj.addProperty("jvm_total_memory_mb", metric.jvmTotalMemoryMb);
            actionObj.addProperty("cpu_seconds", metric.cpuSeconds);
            actionObj.addProperty("container_memory_avg_mb", metric.containerMemoryAvgMb);
            actionObj.addProperty("container_memory_peak_mb", metric.containerMemoryPeakMb);
            actionObj.addProperty("network_rx_bytes", metric.networkRxBytes);
            actionObj.addProperty("network_tx_bytes", metric.networkTxBytes);
            actionsArray.add(actionObj);
        });
        root.add("actions", actionsArray);

        JsonObject benchmarkMode = new JsonObject();
        benchmarkMode.addProperty("selenium_orchestration", "EcoExtension");
        benchmarkMode.addProperty("container_metrics_source", "Prometheus/cAdvisor");
        benchmarkMode.addProperty("load_testing", "EcoGatling");
        root.add("benchmark_mode", benchmarkMode);

        String filename = "target/eco-benchmark-" + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + ".json";
        Files.createDirectories(Paths.get("target"));

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(root, writer);
        }
    }

    private void waitFor(By locator) {
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    private void waitForUrlContains(String pathFragment) {
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.urlContains(pathFragment));
    }

    private void waitUntilTextInBoard(String text) {
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.textToBePresentInElementLocated(By.id("board"), text));
    }

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

    private long usedJvmMemoryBytes() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    private void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    static class ActionMetric {
        final String name;
        final long startedAtEpochMs;
        final long endedAtEpochMs;
        final long durationMs;
        final long jvmMemoryDeltaKb;
        final long jvmTotalMemoryMb;
        final double cpuSeconds;
        final double containerMemoryAvgMb;
        final double containerMemoryPeakMb;
        final double networkRxBytes;
        final double networkTxBytes;

        ActionMetric(String name,
                     long startedAtEpochMs,
                     long endedAtEpochMs,
                     long durationMs,
                     long jvmMemoryDeltaKb,
                     long jvmTotalMemoryMb,
                     double cpuSeconds,
                     double containerMemoryAvgMb,
                     double containerMemoryPeakMb,
                     double networkRxBytes,
                     double networkTxBytes) {
            this.name = name;
            this.startedAtEpochMs = startedAtEpochMs;
            this.endedAtEpochMs = endedAtEpochMs;
            this.durationMs = durationMs;
            this.jvmMemoryDeltaKb = jvmMemoryDeltaKb;
            this.jvmTotalMemoryMb = jvmTotalMemoryMb;
            this.cpuSeconds = cpuSeconds;
            this.containerMemoryAvgMb = containerMemoryAvgMb;
            this.containerMemoryPeakMb = containerMemoryPeakMb;
            this.networkRxBytes = networkRxBytes;
            this.networkTxBytes = networkTxBytes;
        }
    }

    interface ActionRunnable {
        void run() throws Exception;
    }
}
