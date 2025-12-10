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

        // 1. On récupère les propriétés comme demandé dans le doc
        String remoteBrowser = System.getProperty("selenium.remote.browser"); //
        String envHost = System.getProperty("host");
        String envPort = System.getProperty("servlet.port");
        String remoteUrl = System.getProperty("selenium.remote.url");

        if (envHost != null) host = envHost;
        if (envPort != null) port = envPort;

        System.out.println(">>> CONFIG: Host=" + host + " Port=" + port + " RemoteBrowser=" + remoteBrowser);

        ChromeOptions options = new ChromeOptions();
        // Options standard pour éviter les crashs dans les environnements Linux/Docker
        // options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        // 2. La logique du Prof : Si remote.browser est défini, on passe en RemoteWebDriver
        if ("1".equals(remoteBrowser)) { //
            System.out.println(">>> MODE CI (Remote): Connexion au service Selenium...");
            // Si remoteUrl n'est pas défini par Maven, on met l'URL par défaut du service
            if (remoteUrl == null) remoteUrl = "http://selenium-chrome:4444/wd/hub";
            driver = new RemoteWebDriver(new URL(remoteUrl), options);
        } else {
            // Sinon, on est en local (sur ton PC)
            System.out.println(">>> MODE LOCAL: Démarrage Chrome local...");
            WebDriverManager.chromedriver().setup(); //
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