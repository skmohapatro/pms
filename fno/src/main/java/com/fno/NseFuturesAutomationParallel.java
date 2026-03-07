package com.fno;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Arrays;

/**
 * Full, Docker/Cloud Run–ready Selenium scraper skeleton for NSE pages.
 * - Assumes Chromium at /usr/bin/chromium and Chromedriver at /usr/bin/chromedriver (Dockerfile installs these)
 * - Uses strict headless flags, stable UA, and robust waits suitable for Akamai-protected pages
 */
public class NseFuturesAutomationParallel {

    private static final Duration PAGE_LOAD_TIMEOUT = Duration.ofSeconds(90);
    private static final Duration SCRIPT_TIMEOUT    = Duration.ofSeconds(30);
    private static final Duration WAIT_TIMEOUT      = Duration.ofSeconds(45);

    /**
     * Create a headless Chrome driver with explicit binary/driver paths for Docker/Cloud Run.
     */
    private WebDriver newDriver() {
        // Point Selenium at the chromedriver binary
        String driverPath = System.getenv().getOrDefault("CHROMEDRIVER", "/usr/bin/chromedriver");
        System.setProperty("webdriver.chrome.driver", driverPath);

        ChromeOptions options = new ChromeOptions();

        // Point Chrome to the Chromium binary installed by the Dockerfile
        String chromeBin = System.getenv().getOrDefault("CHROME_BIN", "/usr/bin/chromium");
        options.setBinary(chromeBin);

        // Critical flags for containers
        options.addArguments(
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--disable-software-rasterizer",
                "--window-size=1366,768",
                "--lang=en-GB",
                // Stable UA helps with anti-bot gates that treat default headless UA differently
                "--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36",
                "--remote-allow-origins=*"
        );
        options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        ChromeDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(PAGE_LOAD_TIMEOUT);
        driver.manage().timeouts().scriptTimeout(SCRIPT_TIMEOUT);
        return driver;
    }

    private void waitForDocumentReady(WebDriver driver) {
        new WebDriverWait(driver, WAIT_TIMEOUT).until(
                wd -> "complete".equals(((JavascriptExecutor) wd).executeScript("return document.readyState"))
        );
    }

    private WebElement waitPresent(WebDriver driver, By locator, Duration timeout) {
        return new WebDriverWait(driver, timeout)
                .until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    private WebElement waitVisible(WebDriver driver, By locator, Duration timeout) {
        return new WebDriverWait(driver, timeout)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Entry point for a single-symbol scrape demo. Replace with your parallelized logic.
     */
    public void startTheScraper() {
        WebDriver driver = newDriver();
        try {
            // 1) Hit base domain first so cookies/akamai tokens are issued
            driver.get("https://www.nseindia.com/");
            waitForDocumentReady(driver);
            sleep(1200);

            // 2) Navigate to the derivatives quote page for a sample symbol
            final String symbol = System.getenv().getOrDefault("NSE_SYMBOL", "SBIN");
            final String url = "https://www.nseindia.com/get-quotes/derivatives?symbol=" + symbol;
            driver.navigate().to(url);
            waitForDocumentReady(driver);
            sleep(800);

            // 3) Robustly wait for the dynamic dropdown (id varies, so use prefix selector)
            By symbolDropBy = By.cssSelector("select[id^='equity-derivative-allSymbol']");

            WebElement symbolDropdown;
            try {
                symbolDropdown = waitVisible(driver, symbolDropBy, WAIT_TIMEOUT);
            } catch (TimeoutException first) {
                // One soft refresh to allow late Akamai/JS render
                driver.navigate().refresh();
                waitForDocumentReady(driver);
                symbolDropdown = waitVisible(driver, symbolDropBy, WAIT_TIMEOUT);
            }

            System.out.println("[OK] Found symbol dropdown: tag=" + symbolDropdown.getTagName());

            // 4) Example: read options (prove page rendered)
            Select select = new Select(symbolDropdown);
            System.out.println("[INFO] Available symbol options: " + select.getOptions().size());
            // If you want to select a particular symbol
            // select.selectByVisibleText(symbol);

            // TODO: Continue with your actual scraping flow (other dropdowns, tables, etc.)

        } finally {
            try { driver.quit(); } catch (Exception ignored) {}
        }
    }

    private static void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ignored) {}
    }

    public static void main(String[] args) {
        new NseFuturesAutomationParallel().startTheScraper();
    }
}

