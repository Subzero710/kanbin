package fr.uha.ensisa.antidemo.it;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.*;
import fr.uha.ensisa.eco.metrologie.extension.EcoExtension;
import fr.uha.ensisa.eco.metrologie.extension.annotations.*;

import java.util.List;
import java.util.Random;

@EcoDocker(network = "antidemo-metrologie", clean = true)
@EcoDockerContainer(id = "anti-demo-proxy-1", port = 8082)
@EcoMonitor(containerId = "anti-demo-app-1")
@EcoMonitor(containerId = "anti-demo-db-1")
@EcoWebDriver(remote = true) // <- change this to use local browser
@EcoEnergyCounter(type = EcoEnergyCounterType.POWERSPY, name = "Main", endPoint = "$POWERSPY_HOST$")
@EcoGatling(userCount = 50, rampDuration = 10)
@ExtendWith(EcoExtension.class)
public class PremierTest {

    private int NOMBRE_ARTICLE_TEST = 5;
    //en secondes
    private int TEMPS_PAR_ARTICLE = 10;
    private int TEMPS_RECHERCHE_ARTICLE = 5;

    private Random rnd = new Random();
    @RepeatedTest(5)
    @EcoRunConfig(warmupRepetitions = 1)
    public void papillonnage(WebDriver wb){
        JavascriptExecutor js = (JavascriptExecutor) wb;
        wb.get("/");
        int nb;
        try {

            js.executeScript("window.scrollBy(-2000,0)", "");
            Thread.sleep(TEMPS_RECHERCHE_ARTICLE*1000);
            for(int i = 0; i< NOMBRE_ARTICLE_TEST; i++){

                List<WebElement> articles = wb.findElements(By.className("danger-btn"));
                nb=rnd.nextInt(articles.size());
                articles.get(nb).click();
                Thread.sleep(TEMPS_PAR_ARTICLE*300);
                js.executeScript("window.scrollTo(0,document.body.scrollHeight)", "");
                Thread.sleep(TEMPS_PAR_ARTICLE*500);
                js.executeScript("window.scrollTo(0,0)", "");
                Thread.sleep(TEMPS_PAR_ARTICLE*200);
                wb.get("/");
                js.executeScript("window.scrollBy(-2500,0)", "");
                Thread.sleep(TEMPS_RECHERCHE_ARTICLE*1000);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}