package nl.revolution.watchboard.utils;

import nl.revolution.watchboard.WebDriverHttpParamsSetter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.revolution.watchboard.utils.WebDriverUtils.doSleep;

public class WebDriverWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(WebDriverWrapper.class);
    private static final int SOCKET_TIMEOUT_MS = 60 * 1000;

    private WebDriver driver;

    private WebDriverWrapper(WebDriver driver) {
        this.driver = driver;
    }

    public void start() {
        LOG.info("Initializing PhantomJS webDriver.");
        try {
            WebDriverHttpParamsSetter.setSoTimeout(SOCKET_TIMEOUT_MS);
            WebDriverUtils.enableTimeouts(driver);
        } catch (Exception e) {
            LOG.error("Error (re)initializing webDriver: ", e);
            LOG.info("Sleeping 10 seconds and trying again.");
            doSleep(10000);
            start();
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
        String[] args = new String[]{"--proxy-type=none", "--web-security=false", "--webdriver-logfile=/tmp/phantomjsdriver.log"};
        desiredCapabilities.setCapability("phantomjs.cli.args", args);
        desiredCapabilities.setCapability("phantomjs.ghostdriver.cli.args", args);
        desiredCapabilities.setCapability("phantomjs.page.settings.loadImages", false);
        return new WebDriverWrapper(new PhantomJSDriver(desiredCapabilities));
    }

    public static WebDriverWrapper chrome(boolean headless, boolean canary) {
        ChromeOptions chromeOptions = new ChromeOptions();

        if (canary) {
            chromeOptions.setBinary("/Applications/Google Chrome Canary.app/Contents/MacOS/Google Chrome Canary");
        }

        if (headless) {
            chromeOptions.addArguments("headless");
        }

        ChromeDriver driver = new ChromeDriver(chromeOptions);
        return new WebDriverWrapper(driver);
    }


    public static WebDriverWrapper firefox() {
        return new WebDriverWrapper(new FirefoxDriver());
    }



    public WebDriver getDriver() {
        return driver;
    }


}
