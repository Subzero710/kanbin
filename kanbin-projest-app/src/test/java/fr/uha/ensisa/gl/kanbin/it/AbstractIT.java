package fr.uha.ensisa.gl.kanbin.it;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

public abstract class AbstractIT {

    protected static WebDriver driver;
    // Par défaut localhost (fonctionne sur ton PC ET sur le CI en mode local)
    private static String host = "localhost";
    private static String port = "8080";

    @BeforeAll
    public static void setupWebDriver() throws MalformedURLException {
        if (driver != null) return;

        // Récupération des propriétés
        String remoteBrowser = System.getProperty("selenium.remote.browser");
        String envHost = System.getProperty("host");
        String envPort = System.getProperty("servlet.port");
        String remoteUrl = System.getProperty("selenium.remote.url");
        // Choix du navigateur (par défaut chrome)
        String browser = System.getProperty("browser", "chrome").toLowerCase();

        if (envHost != null) host = envHost;
        if (envPort != null) port = envPort;

        System.out.println(">>> DEBUG CONFIG: Host=" + host + ":" + port + " | RemoteMode=" + remoteBrowser + " | Browser=" + browser);

        // --- OPTIONS CHROME (CRITIQUES POUR DOCKER) ---
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless=new"); // Obligatoire CI
        chromeOptions.addArguments("--no-sandbox"); // ANTI-CRASH DOCKER
        chromeOptions.addArguments("--disable-dev-shm-usage"); // ANTI-CRASH DOCKER
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--remote-allow-origins=*");
        chromeOptions.addArguments("--window-size=1920,1080");

        // --- OPTIONS FIREFOX ---
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        firefoxOptions.addArguments("-headless");
        firefoxOptions.addArguments("--width=1920");
        firefoxOptions.addArguments("--height=1080");

        // Logique de sélection
        if ("1".equals(remoteBrowser) && remoteUrl != null) {
            // Mode Remote (si on voulait utiliser un service selenium externe)
            System.out.println(">>> MODE: REMOTE GRID");
            if (browser.contains("firefox")) {
                driver = new RemoteWebDriver(new URL(remoteUrl), firefoxOptions);
            } else {
                driver = new RemoteWebDriver(new URL(remoteUrl), chromeOptions);
            }
        } else {
            // Mode Local (Ton PC + CI méthode installation locale)
            System.out.println(">>> MODE: LOCAL DRIVER");
            if (browser.contains("firefox")) {
                WebDriverManager.firefoxdriver().setup();
                driver = new FirefoxDriver(firefoxOptions);
            } else {
                WebDriverManager.chromedriver().setup();
                // On passe bien les options anti-crash ici !
                driver = new ChromeDriver(chromeOptions);
            }
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