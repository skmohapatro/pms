package com.fno;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class NseFuturesAutomationParallel2 {

    // ====== Constants / Selectors ======
    private static final String NSE_URL = "https://www.nseindia.com/get-quotes/derivatives?symbol=360ONE";
    private static final By ALL_SYMBOLS_DD_ANY = By.cssSelector("select[id^='equity-derivative-allSymbol']");
    private static final By ALL_CONTRACTS_TABLE = By.cssSelector("#gq-derivatives-all-contracts-table");
    private static final DateTimeFormatter DTF1 =
    	    DateTimeFormatter.ofPattern("dd-MMM-yyyy-HH-mm-ss", Locale.ENGLISH);

    // Trade Information – Stock Futures accordion
    private static final By TRADE_INFO_TOGGLE = By.cssSelector("[aria-label='Trade Information - Stock Futures']");
    private static final By TRADE_INFO_EXPANDED = By.cssSelector("[aria-label='Trade Information - Stock Futures'][aria-expanded='true']");
    private static final By UNDERLYING_VALUE_X = By.xpath("//td[@id='d-underlyingValueTrade']/following-sibling::td[1]");
    private static final By MARKET_LOT_X = By.xpath("//td[@id='marketLot']/following-sibling::td[1]");

    // Parsing & formatting
    private static final Locale LOCALE_EN = Locale.ENGLISH;
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter TS_FMT =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("dd-MMM-uuuu-HH:mm:ss")
                    .toFormatter(LOCALE_EN)
                    .withResolverStyle(ResolverStyle.SMART);
    private static final DateTimeFormatter EXP_FMT =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("dd-MMM-uuuu")
                    .toFormatter(LOCALE_EN)
                    .withResolverStyle(ResolverStyle.SMART);

    // Finance assumptions (same as your original logic)
    private static final double FUTURES_MARGIN_FRACTION = 0.20;  // (Last * Lot) / 5
    private static final double FIXED_COST = 2000.0;             // ₹2000

    // Candidate values to recognize "Stock Futures" in dropdowns
    private static final String FUTURES_TEXT = "Stock Futures";
    private static final String FUTURES_VALUE = "FUTSTK";

    public static void main(String[] args) {
        long t0 = System.nanoTime();

        WebDriverManager.chromedriver().setup();

        ChromeOptions baseOpts = new ChromeOptions();
        baseOpts.addArguments("--start-maximized");
        baseOpts.addArguments("--disable-gpu");
        baseOpts.addArguments("--no-sandbox");
        baseOpts.addArguments("--disable-dev-shm-usage");
        // Uncomment to speed up on servers/CI:
        // baseOpts.addArguments("--headless=new");

        // 1) Use a seed driver to fetch the full list of symbols
        List<String> symbols = new ArrayList<>();
        WebDriver seed = null;
        try {
            seed = new ChromeDriver(baseOpts);
            WebDriverWait wait = new WebDriverWait(seed, Duration.ofSeconds(20));
            seed.get(NSE_URL);
            waitForDocumentReady(seed);

            WebElement dd = wait.until(ExpectedConditions.presenceOfElementLocated(ALL_SYMBOLS_DD_ANY));
            Select sel = new Select(dd);
            for (WebElement op : sel.getOptions()) {
                String v = (op.getAttribute("value") == null) ? "" : op.getAttribute("value").trim();
                if (v.isEmpty()) continue;
                if ("-1".equals(v)) continue; // placeholder/header
                symbols.add(v);
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch symbol list: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (seed != null) try { seed.quit(); } catch (Exception ignore) {}
        }

        if (symbols.isEmpty()) {
            System.err.println("No symbols found. Exiting.");
            return;
        }

        // Optional: test subset
        // symbols = symbols.subList(0, Math.min(25, symbols.size()));

        // 2) Parallel scrape (1 driver per task)
        int threads = Math.min(Math.max(2, Runtime.getRuntime().availableProcessors()), 8);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        System.out.printf("Scraping %d symbols using %d threads...%n", symbols.size(), threads);

        List<Callable<List<FutureData>>> tasks = symbols.stream()
                .map(sym -> (Callable<List<FutureData>>) () -> scrapeOneSymbol(sym, baseOpts))
                .collect(Collectors.toList());

        List<Future<List<FutureData>>> futures;
        try {
            futures = pool.invokeAll(tasks);
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while scheduling tasks", e);
        }

        // 3) Consolidate results
        List<FutureData> allRows = new ArrayList<>(symbols.size() * 4); // rough cap
        int symbolsOk = 0, symbolsFailed = 0;
        for (int i = 0; i < futures.size(); i++) {
            String sym = symbols.get(i);
            try {
                List<FutureData> rows = futures.get(i).get();
                allRows.addAll(rows);
                symbolsOk++;
            } catch (Exception ex) {
                symbolsFailed++;
                System.err.printf("Symbol task failed (%s): %s%n", sym, ex.getMessage());
            }
        }

        pool.shutdown();

        // 4) Write one Excel from merged rows
        writeToExcel(allRows);

        // 5) Execution time + stats
        long t1 = System.nanoTime();
        double seconds = (t1 - t0) / 1_000_000_000.0;
        System.out.println("=====================================");
        System.out.printf("Symbols attempted : %d%n", symbols.size());
        System.out.printf("Symbols succeeded : %d%n", symbolsOk);
        System.out.printf("Symbols failed    : %d%n", symbolsFailed);
        System.out.printf("Rows collected    : %d%n", allRows.size());
        System.out.printf("Total time        : %.2f seconds (%.2f minutes)%n",
                seconds, seconds / 60.0);
        System.out.println("=====================================");
    }

    // ========= Per-symbol scrape with a light retry =========
    private static List<FutureData> scrapeOneSymbol(String symbol, ChromeOptions baseOpts) {
        int attempts = 0;
        RuntimeException lastErr = null;
        while (attempts < 2) { // 1 retry
            attempts++;
            try {
                return scrapeOneSymbolOnce(symbol, baseOpts);
            } catch (RuntimeException e) {
                lastErr = e;
                // small jitter to avoid thundering herd on retry
                try { Thread.sleep(500L + ThreadLocalRandom.current().nextInt(500)); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        throw lastErr != null ? lastErr : new RuntimeException("Unknown scrape failure for " + symbol);
    }

    private static List<FutureData> scrapeOneSymbolOnce(String symbol, ChromeOptions baseOpts) {
        WebDriver driver = null;
        List<FutureData> result = new ArrayList<>();
        try {
            driver = new ChromeDriver(baseOpts);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            String url = "https://www.nseindia.com/get-quotes/derivatives?symbol=" + urlEncode(symbol);
            driver.get(url);
            waitForDocumentReady(driver);

            // Ensure instrument dropdown exists and is hydrated with Stock Futures/FUTSTK
            Select instrumentSel = selectInstrumentDropdown(driver);
            ensureInstrumentStockFutures(instrumentSel);

            // Open Trade Info – read Spot & Lot
            openTradeInfo(driver);
            double spot = getDouble(driver, wait, UNDERLYING_VALUE_X);
            double lot = getDouble(driver, wait, MARKET_LOT_X);
            collapseTradeInfo(driver);

            // Wait table and build header map
            wait.until(ExpectedConditions.presenceOfElementLocated(ALL_CONTRACTS_TABLE));
            WebElement table = driver.findElement(ALL_CONTRACTS_TABLE);
            Map<String, Integer> hIdx = headerIndexMap(table);

            int expiryIdx = colOrFail(hIdx, "expiry date");
            int closeIdx  = colOrFail(hIdx, "close");
            int lastIdx   = colOrFail(hIdx, "last");

            // Current timestamp in IST, uppercase as in your original
            String nowIst = LocalDateTime.now(IST).format(TS_FMT).toUpperCase(LOCALE_EN);

            // Rows
            WebElement tbody = table.findElement(By.tagName("tbody"));
            List<WebElement> rows = tbody.findElements(By.tagName("tr"));
            for (WebElement tr : rows) {
                String expiryStr = cellText(tr, expiryIdx);
                String lastStr = cellText(tr, lastIdx);
                String closeStr = cellText(tr, closeIdx);

                double last = parseNumber(lastStr);
                double close = parseNumber(closeStr);

                FutureData fd = new FutureData();
                fd.companyCode = symbol;
                fd.currentDateTime = nowIst;
                fd.selectedExpiry = expiryStr;
                fd.featurePriceL = last;
                fd.featurePriceC = close;
                fd.spotPrice = spot;
                fd.lotSize = lot;

                result.add(fd);
            }

            return result;
        } catch (Throwable t) {
            throw new RuntimeException("Scrape failed for symbol=" + symbol + " -> " + t.getMessage(), t);
        } finally {
            if (driver != null) try { driver.quit(); } catch (Exception ignore) {}
        }
    }

    // ========= Robust waits & dropdown helpers =========
    private static void waitForDocumentReady(WebDriver driver) {
        new WebDriverWait(driver, Duration.ofSeconds(25)).until(d ->
                "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState"))
        );
    }

    private static Select selectInstrumentDropdown(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(25));
        // Any instrument dropdown (id may be ...0 or ...1)
        By anyInstrument = By.cssSelector("select[id^='equity-derivative-instrumentType']");
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(anyInstrument));
        return new Select(el);
    }

    // Wait until the select has an option with given visible text or value
    private static void waitSelectHasOption(Select sel, Duration timeout, String... candidates) {
        long end = System.nanoTime() + timeout.toMillis() * 1_000_000L;
        while (System.nanoTime() < end) {
            List<WebElement> opts = sel.getOptions();
            for (WebElement op : opts) {
                String txt = op.getText() == null ? "" : op.getText().trim();
                String val = op.getAttribute("value");
                val = val == null ? "" : val.trim();
                for (String want : candidates) {
                    if (want.equalsIgnoreCase(txt) || want.equalsIgnoreCase(val)) {
                        return;
                    }
                }
            }
            try { Thread.sleep(200); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        throw new TimeoutException("Select never got expected option(s): " + String.join(", ", candidates));
    }

    // Ensure instrument is set to Stock Futures (by visible text or value)
    private static void ensureInstrumentStockFutures(Select sel) {
        // If already on a futures-like selection, skip
        try {
            String curText = sel.getFirstSelectedOption().getText();
            if (curText != null && curText.toLowerCase(Locale.ROOT).contains("futur")) return;
        } catch (Exception ignore) {}

        // Wait for option hydration
        waitSelectHasOption(sel, Duration.ofSeconds(20), FUTURES_TEXT, FUTURES_VALUE);

        // Prefer visible text; fall back to value; then heuristic
        try { sel.selectByVisibleText(FUTURES_TEXT); return; }
        catch (NoSuchElementException ignore) { /* fall through */ }

        try { sel.selectByValue(FUTURES_VALUE); return; }
        catch (NoSuchElementException ignore) { /* fall through */ }

        for (WebElement op : sel.getOptions()) {
            String t = op.getText() == null ? "" : op.getText().toLowerCase(Locale.ROOT);
            if (t.contains("futur")) { op.click(); return; }
        }
        throw new NoSuchElementException("Cannot locate a futures option by text/value.");
    }

    // ========= Trade Info accordion helpers =========
    private static void openTradeInfo(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        WebElement toggle = wait.until(ExpectedConditions.presenceOfElementLocated(TRADE_INFO_TOGGLE));
        scrollIntoView(driver, toggle);
        String expanded = toggle.getAttribute("aria-expanded");
        if (!"true".equalsIgnoreCase(expanded)) {
            safeClick(driver, toggle);
            wait.until(ExpectedConditions.presenceOfElementLocated(TRADE_INFO_EXPANDED));
        }
    }

    private static void collapseTradeInfo(WebDriver driver) {
        try {
            WebElement toggle = driver.findElement(TRADE_INFO_TOGGLE);
            String expanded = toggle.getAttribute("aria-expanded");
            if ("true".equalsIgnoreCase(expanded)) {
                safeClick(driver, toggle);
            }
        } catch (Exception ignore) {}
    }

    // ========= Table parsing helpers =========
    private static Map<String, Integer> headerIndexMap(WebElement table) {
        Map<String, Integer> idx = new HashMap<>();
        WebElement thead = table.findElement(By.tagName("thead"));
        List<WebElement> ths = thead.findElements(By.tagName("th"));
        for (int i = 0; i < ths.size(); i++) {
            String key = normalize(ths.get(i).getText());
            idx.put(key, i + 1); // 1-based
        }
        return idx;
    }

    private static int colOrFail(Map<String, Integer> map, String name) {
        Integer v = map.get(normalize(name));
        if (v == null) throw new NoSuchElementException("Column not found: " + name);
        return v;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static String cellText(WebElement tr, int oneBasedIdx) {
        try {
            WebElement td = tr.findElement(By.cssSelector("td:nth-child(" + oneBasedIdx + ")"));
            return td.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    // ========= Generic element utils =========
    private static double getDouble(WebDriver driver, WebDriverWait wait, By locator) {
        String s = wait.until(ExpectedConditions.presenceOfElementLocated(locator)).getText();
        return parseNumber(s);
    }

    private static double parseNumber(String s) {
        if (s == null) return 0.0;
        s = s.trim().replaceAll("[^0-9.,-]", "");
        if (s.isEmpty()) return 0.0;
        try {
            return NumberFormat.getNumberInstance(Locale.US).parse(s).doubleValue();
        } catch (Exception ignore) {
            try {
                return Double.parseDouble(s.replace(",", ""));
            } catch (Exception e) {
                return 0.0;
            }
        }
    }

    private static void scrollIntoView(WebDriver driver, WebElement el) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center',inline:'nearest'});", el);
    }

    private static void safeClick(WebDriver driver, WebElement el) {
        try {
            el.click();
        } catch (ElementClickInterceptedException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        }
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    // ========= Excel writer (same structure & formulas as your original) =========
    private static void writeToExcel(List<FutureData> data) {
        String[] headers = new String[] {
                "Company Code", "Date Time", "Expiry Date", "Feature Price Last Traded",
                "Feature Price Closed", "Spot Price", "Lot Size", "Holding(DAYS)",
                "Price Difference", "% Price Difference",
                "Feature Investment Amount", "Spot Investment Amount", "Total Investment",
                "Total Profit", "Perday Return", "Per Annum Return %"
        };

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("FuturesData");

            // Create a numeric cell style with 2 decimal places
            CellStyle twoDecimalStyle = wb.createCellStyle();
            DataFormat df = wb.createDataFormat();
            twoDecimalStyle.setDataFormat(df.getFormat("0.00"));

            // Header row
            Row h = sh.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                h.createCell(i, CellType.STRING).setCellValue(headers[i]);
            }

            int r = 1;
            for (FutureData fd : data) {
                int holdingDays = holdingDays(fd.currentDateTime, fd.selectedExpiry);

                double priceDiff = fd.featurePriceL - fd.spotPrice;
                if (priceDiff <= 0) continue; // keep same filter

                double pctPriceDiff = (fd.spotPrice == 0) ? 0.0 : (priceDiff / (fd.spotPrice / 100.0));
                double futInv = fd.featurePriceL * fd.lotSize;
                double spotInv = fd.spotPrice * fd.lotSize;
                double totalInv = spotInv + futInv * FUTURES_MARGIN_FRACTION;
                double totalProfit = (priceDiff * fd.lotSize) - FIXED_COST;
                double perDay = (holdingDays > 0) ? (totalProfit / holdingDays) : 0.0;
                double perAnnumPct = (totalInv == 0) ? 0.0 : ((perDay * 365.0) / (totalInv / 100.0));

                if (perAnnumPct>=7) {
					Row row = sh.createRow(r++);
					int c = 0;
					row.createCell(c++).setCellValue(fd.companyCode);
					row.createCell(c++).setCellValue(fd.currentDateTime);
					row.createCell(c++).setCellValue(fd.selectedExpiry);
					setNumeric(row, c++, fd.featurePriceL, twoDecimalStyle);
					setNumeric(row, c++, fd.featurePriceC, twoDecimalStyle);
					setNumeric(row, c++, fd.spotPrice, twoDecimalStyle);
					setNumeric(row, c++, fd.lotSize, twoDecimalStyle);
					row.createCell(c++).setCellValue(holdingDays); // integer, no style
					setNumeric(row, c++, priceDiff, twoDecimalStyle);
					setNumeric(row, c++, pctPriceDiff, twoDecimalStyle);
					setNumeric(row, c++, futInv, twoDecimalStyle);
					setNumeric(row, c++, spotInv, twoDecimalStyle);
					setNumeric(row, c++, totalInv, twoDecimalStyle);
					setNumeric(row, c++, totalProfit, twoDecimalStyle);
					setNumeric(row, c++, perDay, twoDecimalStyle);
					setNumeric(row, c++, perAnnumPct, twoDecimalStyle);
				}
            }

            for (int i = 0; i < headers.length; i++) sh.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream("FuturesData-"+ZonedDateTime.now(IST).format(DTF1).toUpperCase(Locale.ENGLISH)+".xlsx")) {
                wb.write(fos);
            }
            System.out.println("Excel file written successfully: FuturesData.xlsx");
        } catch (IOException e) {
            throw new RuntimeException("Failed to write Excel: " + e.getMessage(), e);
        }
    }

    // Helper method to round and apply style
    private static void setNumeric(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col, CellType.NUMERIC);
        cell.setCellValue(Math.round(value * 100.0) / 100.0); // round to 2 decimals
        cell.setCellStyle(style);
    }

    
    private static int holdingDays(String nowIstStr, String expiryStr) {
        try {
            LocalDate start = LocalDate.parse(nowIstStr, TS_FMT);
            LocalDate end = LocalDate.parse(expiryStr, EXP_FMT);
            return (int) Duration.between(start.atStartOfDay(), end.atStartOfDay()).toDays();
        } catch (Exception e) {
            return 0;
        }
    }

    // ========= Data POJO =========
    static class FutureData {
        String companyCode;
        String currentDateTime;   // "dd-MMM-uuuu-HH:mm:ss" IST, uppercased
        String selectedExpiry;    // "dd-MMM-uuuu"
        double featurePriceL;     // Last
        double featurePriceC;     // Close
        double spotPrice;         // Underlying value
        double lotSize;           // Market lot
    }
}

