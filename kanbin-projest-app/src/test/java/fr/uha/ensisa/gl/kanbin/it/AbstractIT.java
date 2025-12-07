package fr.uha.ensisa.gl.kanbin.it;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

public abstract class AbstractIT {

    protected static WebDriver driver;
    // Par défaut localhost (pour ton PC), mais surchargé par le CI
    private static String host = "localhost";
    private static String port = "8080";

    @BeforeAll
    public static void setupWebDriver() throws MalformedURLException {
        if (driver != null) return;

        // 1. Récupération des propriétés (Définies dans le pom.xml ou la ligne de commande)
        String remoteBrowser = System.getProperty("selenium.remote.browser");
        String envHost = System.getProperty("host");
        String envPort = System.getProperty("servlet.port");
        String remoteUrl = System.getProperty("selenium.remote.url");

        if (envHost != null) host = envHost;
        if (envPort != null) port = envPort;

        System.out.println(">>> DEBUG SETUP: Host=" + host + " Port=" + port + " RemoteMode=" + remoteBrowser);

        ChromeOptions options = new ChromeOptions();
        // Options indispensables pour que Chrome ne crash pas dans Docker
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        if ("1".equals(remoteBrowser) && remoteUrl != null) {
            // --- CAS GITLAB CI (Selon le document du prof) ---
            System.out.println(">>> MODE CI DETECTÉ: Connexion au Selenium distant...");
            driver = new RemoteWebDriver(new URL(remoteUrl), options);
        } else {
            // --- CAS LOCAL (Ton PC) ---
            System.out.println(">>> MODE LOCAL: Démarrage Chrome local...");
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver(options);
        }

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
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
}