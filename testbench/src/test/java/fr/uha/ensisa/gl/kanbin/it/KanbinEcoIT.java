package fr.uha.ensisa.gl.kanbin.it;

import fr.uha.ensisa.eco.metrologie.extension.EcoExtension;
import fr.uha.ensisa.eco.metrologie.extension.annotations.EcoDocker;
import fr.uha.ensisa.eco.metrologie.extension.annotations.EcoDockerContainer;
import fr.uha.ensisa.eco.metrologie.extension.annotations.EcoRunConfig;
import fr.uha.ensisa.eco.metrologie.extension.annotations.EcoWebDriver;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EcoDocker(network = "kanbin-net", clean = true)
@EcoDockerContainer(id = "kanbin-app", port = 8080)
@EcoWebDriver(remote = true)
@EcoRunConfig(initIdle = 2_000, minIdle = 2_000)
@ExtendWith(EcoExtension.class)
class KanbinEcoIT {

    @RepeatedTest(5)
    void openHomePage(WebDriver driver) {
        driver.get("/");

        String pageSource = driver.getPageSource();
        assertFalse(pageSource == null || pageSource.isBlank(), "The home page should return HTML content");
    }

    @RepeatedTest(5)
    void openBoardPage(WebDriver driver) {
        driver.get("/board");

        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/board") || currentUrl.endsWith("/"),
                "The application should expose the board route or redirect from the root page");

        assertFalse(driver.findElements(By.tagName("body")).isEmpty(),
                "The board page should render a body element");
    }
}