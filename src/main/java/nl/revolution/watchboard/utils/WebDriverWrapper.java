package nl.revolution.watchboard.utils;

import nl.revolution.watchboard.WebDriverHttpParamsSetter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

import static nl.revolution.watchboard.utils.WebDriverUtils.doSleep;

public class WebDriverWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(WebDriverWrapper.class);
    private static final int SOCKET_TIMEOUT_MS = 60 * 1000;

    private Supplier<WebDriver> driverSupplier;
    private WebDriver driver;

    private WebDriverWrapper(Supplier<WebDriver> driverSupplier) {
        this.driverSupplier = driverSupplier;
    }

    public void start() {
        LOG.info("Initializing webDriver.");
        try {
            driver = driverSupplier.get();
            WebDriverHttpParamsSetter.setSoTimeout(SOCKET_TIMEOUT_MS);
            WebDriverUtils.enableTimeouts(driver);
        } catch (Exception e) {
            LOG.error("Error (re)initializing webDriver: ", e);
            LOG.info("Sleeping 10 seconds and trying again.");
            doSleep(10000);
            restart();
            return;
        }
        doSleep(100);
    }

    public void shutdown() {
        try {
            if (driver != null) {
                driver.quit();
                driver = null;
            }
        } catch (Exception e) {
            LOG.error("Error while shutting down webDriver: ", e);
        }
        doSleep(500);
    }

    public void restart() {
        shutdown();
        start();
    }

    public static WebDriverWrapper phantomJs() {
        DesiredCapabilities desiredCapabilities = DesiredCapabilities.phantomjs();
        String[] args = new String[]{"--proxy-type=none", "--web-security=false"};
        desiredCapabilities.setCapability("phantomjs.cli.args", args);
        desiredCapabilities.setCapability("phantomjs.ghostdriver.cli.args", args);
        desiredCapabilities.setCapability("phantomjs.page.settings.loadImages", false);
        return new WebDriverWrapper(() -> new PhantomJSDriver(desiredCapabilities));
    }

    public WebDriver getDriver() {
        return driver;
    }
}
