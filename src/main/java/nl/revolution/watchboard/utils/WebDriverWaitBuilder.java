package nl.revolution.watchboard.utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class WebDriverWaitBuilder {

    private WebDriver driver;
    private long seconds;

    public static WebDriverWaitBuilder let(WebDriver driver) {
        WebDriverWaitBuilder waitForBuilder = new WebDriverWaitBuilder();
        waitForBuilder.driver = driver;
        return waitForBuilder;
    }

    public WebDriverWaitBuilder wait(long seconds, ChronoUnit unit) {
        this.seconds = Duration.of(seconds, unit).getSeconds();
        return this;
    }

    public <T> T on(ExpectedCondition<T> condition) {
        WebDriverWait loadwait = new WebDriverWait(driver, seconds);
        return loadwait.until(condition);
    }

}
