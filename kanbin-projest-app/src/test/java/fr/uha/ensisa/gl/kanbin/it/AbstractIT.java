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
    private static String host = "localhost";
    private static String port = "8080";

    @BeforeAll
    public static void setupWebDriver() throws MalformedURLException {
        if (driver != null) return;

        // Récupération des propriétés système injectées par Maven
        String remoteBrowser = System.getProperty("selenium.remote.browser");
        String envHost = System.getProperty("host");
        String envPort = System.getProperty("servlet.port");
        String remoteUrl = System.getProperty("selenium.remote.url");

        if (envHost != null) host = envHost;
        if (envPort != null) port = envPort;

        System.out.println(">>> DEBUG: Host=" + host + " Port=" + port + " RemoteMode=" + remoteBrowser);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--window-size=1920,1080");

        // Logique demandée par le prof : si remote.browser est défini, on passe en RemoteWebDriver
        if ("1".equals(remoteBrowser) && remoteUrl != null) {
            System.out.println(">>> MODE CI: Connexion au Selenium distant...");
            driver = new RemoteWebDriver(new URL(remoteUrl), options);
        } else {
            System.out.println(">>> MODE LOCAL: Démarrage Chrome...");
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