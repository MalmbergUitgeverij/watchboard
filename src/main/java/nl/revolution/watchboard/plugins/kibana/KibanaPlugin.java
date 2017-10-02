package nl.revolution.watchboard.plugins.kibana;

import nl.revolution.watchboard.Config;
import nl.revolution.watchboard.data.Graph;
import nl.revolution.watchboard.data.Plugin;
import nl.revolution.watchboard.plugins.WatchboardPlugin;
import nl.revolution.watchboard.utils.WebDriverUtils;
import nl.revolution.watchboard.utils.WebDriverWrapper;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.SECONDS;
import static nl.revolution.watchboard.utils.WebDriverWaitBuilder.let;
import static org.openqa.selenium.support.ui.ExpectedConditions.not;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfAllElementsLocatedBy;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfAllElementsLocatedBy;

public class KibanaPlugin implements WatchboardPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(KibanaPlugin.class);
    private final Graph.Type type;

    private boolean stop;

    private Plugin plugin;
    private WebDriverWrapper wrappedDriver;

    public KibanaPlugin(Graph.Type type) {
        LOG.info("Starting Kibana plugin.");
        this.type = type;
        plugin = Config.getInstance().getPlugin(this.type);
    }

    @Override
    public void performLogin() {
        LOG.info("Logging in to Kibana.");
        try {
            WebDriver driver = wrappedDriver.getDriver();
            driver.manage().window().setSize(new Dimension(2000, 1000));
            driver.get(plugin.getLoginUrl());
            WebDriverUtils.verifyTitle(driver, "Kibana", 10);
        } catch (Exception e) {
            LOG.error("Error while logging in to Kibana: ", e);
        }

        LOG.info("Logged in to Kibana.");
    }


    @Override
    public void performUpdate() {
        long start = System.currentTimeMillis();
        LOG.info("Performing update for Kibana graphs.");

        // Check for config file update.
        Config.getInstance().checkForConfigUpdate();

        // Generate reports for all graphs for all dashboards.
        LOG.info("Updating data from Kibana.");
        Config.getInstance().getGrapsForType(type).stream().forEach(graph -> {
                if (!stop) {
                    performSingleUpdate(graph);
                }
            }
        );

        long end = System.currentTimeMillis();
        LOG.info("Finished updating " + getName() + " graphs. Update took " + ((end-start)/1000) + " seconds.");
    }


    private void performSingleUpdate(Graph graph) {
        LOG.debug("Starting update of {}.", graph.getImagePath());
        WebDriver driver = wrappedDriver.getDriver();
        driver.manage().window().setSize(new Dimension(2000, 1000));
        WebDriverUtils.fetchDummyPage(driver);

        try {
            WebDriverUtils.disableTimeouts(driver);

            try {
                driver.get(graph.getUrl());
            } catch (TimeoutException ignored) {
                // Expected, do nothing.
            }

            let(driver).wait(15, SECONDS).on(currentUrlIs(graph.getUrl()));
            let(driver).wait(30, SECONDS).on(visibilityOfAllElementsLocatedBy(By.tagName("visualize")));
            let(driver).wait(10, SECONDS).on(visibilityOfAllElementsLocatedBy(By.className("visualize-chart")));
            let(driver).wait(30, SECONDS).on(not(presenceOfAllElementsLocatedBy(By.className("loading"))));
            let(driver).wait(5, SECONDS).on(nonTransparant(By.className("visualize-chart")));

            getKibanaScreenshot(graph.getBrowserWidth(), graph.getBrowserHeight(), graph.getImagePath());
            plugin.setTsLastUpdated(LocalDateTime.now());

        } catch (NoSuchElementException | TimeoutException e) {
            LOG.info("No Kibana visualizations found for graph {}; skipping screenshot. ({})", graph.getId(), e.getMessage());
            WebDriverUtils.takeDebugScreenshot(driver, graph);
        } finally {
            WebDriverUtils.enableTimeouts(driver);
        }
    }

    private ExpectedCondition<Boolean> nonTransparant(By locator) {
        return webDriver -> webDriver.findElements(locator).stream().allMatch(element -> element.getCssValue("opacity").equals(String.valueOf(1)));
    }

    private ExpectedCondition<Boolean> currentUrlIs(String url) {
        return webDriver -> webDriver.getCurrentUrl().equals(url);
    }


    private void getKibanaScreenshot(int width, int height, String filename) {
        WebDriver driver = wrappedDriver.getDriver();
        driver.manage().window().setSize(new Dimension(width, height));

        try {
            WebDriverUtils.takeScreenShot(driver, driver.findElement(By.tagName("dashboard-grid")), filename);
        } catch (IOException e) {
            LOG.error("Error while taking screenshot: ", e);
        }
    }

    @Override
    public void shutdown() {
        LOG.info("Shutting down.");
        this.stop = true;
    }

    @Override
    public void setDriver(WebDriverWrapper driver) {
        this.wrappedDriver = driver;
    }

    @Override
    public String getName() {
        return "Kibana";
    }

    @Override
    public int getUpdateInterval() {
        return plugin.getUpdateIntervalSeconds();
    }

}
