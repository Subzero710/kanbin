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
    private static String host;
    private static String port;

    @BeforeAll
    public static void setupWebDriver() throws MalformedURLException {
        if (driver != null) return;

        host = System.getProperty("host", "localhost");
        port = System.getProperty("servlet.port", "8080");
        String remoteUrl = System.getProperty("selenium.remote.url");

        System.out.println("DEBUG: Config -> Host=" + host + " Port=" + port + " RemoteURL=" + remoteUrl);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        if (remoteUrl != null && !remoteUrl.isEmpty()) {
            System.out.println("DEBUG: Démarrage en mode REMOTE (GitLab CI)...");
            driver = new RemoteWebDriver(new URL(remoteUrl), options);
        } else {

            System.out.println("DEBUG: Démarrage en mode LOCAL...");
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