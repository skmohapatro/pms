package com.investment.portfolio.service;

import com.investment.portfolio.dto.ArbitrageOpportunity;
import com.investment.portfolio.entity.Instrument;
import com.investment.portfolio.repository.InstrumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ArbitrageService {

    private static final Logger logger = LoggerFactory.getLogger(ArbitrageService.class);
    
    // Financial constants from FNO code
    private static final double FUTURES_MARGIN_FRACTION = 0.20;  // 20% margin for futures
    private static final double FIXED_COST = 2000.0;             // ₹2000 fixed cost
    
    // Batch configuration
    private static final int BATCH_SIZE = 50;  // Max symbols per batch LTP call
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BASE_DELAY_MS = 500;
    
    // Date formatters
    private static final DateTimeFormatter TS_FMT = 
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("dd-MMM-uuuu-HH:mm:ss")
                    .toFormatter(Locale.ENGLISH)
                    .withResolverStyle(ResolverStyle.SMART);
    
    private static final DateTimeFormatter EXP_FMT = 
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("dd-MMM-uuuu")
                    .toFormatter(Locale.ENGLISH)
                    .withResolverStyle(ResolverStyle.SMART);
    
    // Pattern to extract base symbol and year/month from futures contract
    private static final Pattern FUTURES_PATTERN = Pattern.compile("^([A-Z&]+?)(\\d{2})([A-Z]{3})FUT$");
    
    // Month abbreviation to month number mapping
    private static final Map<String, Integer> MONTH_MAP = new HashMap<>();
    static {
        MONTH_MAP.put("JAN", 1);  MONTH_MAP.put("FEB", 2);  MONTH_MAP.put("MAR", 3);
        MONTH_MAP.put("APR", 4);  MONTH_MAP.put("MAY", 5);  MONTH_MAP.put("JUN", 6);
        MONTH_MAP.put("JUL", 7);  MONTH_MAP.put("AUG", 8);  MONTH_MAP.put("SEP", 9);
        MONTH_MAP.put("OCT", 10); MONTH_MAP.put("NOV", 11); MONTH_MAP.put("DEC", 12);
    }
    
    @Autowired
    private InstrumentRepository instrumentRepository;
    
    @Autowired
    private RestTemplate restTemplate;
    
    private static final String CHAT_BACKEND_URL = "http://localhost:5000";
    
    // Processing metadata (accessible after calculation)
    private int lastTotalCompanies = 0;
    private int lastSuccessfulCompanies = 0;
    private int lastFailedCompanies = 0;
    private int lastTotalFuturesProcessed = 0;
    private int lastFuturesSkippedNoMatch = 0;
    private int lastFuturesSkippedApiError = 0;
    
    public int getLastTotalCompanies() { return lastTotalCompanies; }
    public int getLastSuccessfulCompanies() { return lastSuccessfulCompanies; }
    public int getLastFailedCompanies() { return lastFailedCompanies; }
    public int getLastTotalFuturesProcessed() { return lastTotalFuturesProcessed; }
    public int getLastFuturesSkippedNoMatch() { return lastFuturesSkippedNoMatch; }
    public int getLastFuturesSkippedApiError() { return lastFuturesSkippedApiError; }
    
    /**
     * Calculate arbitrage opportunities using batch parallel price fetching.
     * 
     * Architecture:
     * 1. Query DB for all EQ and FUT instruments
     * 2. Build symbol lists for batch LTP calls
     * 3. Fetch ALL spot prices in parallel batches (few batch calls instead of ~200 individual)
     * 4. Fetch ALL futures prices in parallel batches (few batch calls instead of ~600 individual)
     * 5. Calculate arbitrage opportunities in parallel using pre-fetched prices
     */
    public List<ArbitrageOpportunity> calculateArbitrageOpportunities(double minAnnualReturn) {
        logger.info("Starting arbitrage opportunity calculation with min return: {}%", minAnnualReturn);
        
        // Step 1: Get all NSE instruments (EQ and FUT)
        List<Instrument> allInstruments = instrumentRepository.findByExchange("NSE");
        
        Map<String, Instrument> equityMap = new HashMap<>();
        List<Instrument> futuresList = new ArrayList<>();
        
        for (Instrument inst : allInstruments) {
            if ("EQ".equals(inst.getInstrumentType())) {
                equityMap.put(inst.getTradingSymbol(), inst);
            } else if ("FUT".equals(inst.getInstrumentType())) {
                futuresList.add(inst);
            }
        }
        
        logger.info("Found {} equity instruments and {} futures contracts", equityMap.size(), futuresList.size());
        
        // Step 2: Build matched pairs (futures -> base symbol) and collect symbols needed
        Map<String, String> futToBase = new LinkedHashMap<>();  // SBIN26MAYFUT -> SBIN
        Set<String> uniqueBaseSymbols = new LinkedHashSet<>();
        Set<String> unmatchedFutures = new HashSet<>();
        
        for (Instrument futInst : futuresList) {
            String base = extractBaseSymbol(futInst.getTradingSymbol());
            if (base == null || !equityMap.containsKey(base)) {
                unmatchedFutures.add(futInst.getTradingSymbol());
                continue;
            }
            Double lotSize = futInst.getLotSize() != null ? futInst.getLotSize().doubleValue() : 0.0;
            if (lotSize <= 0) {
                logger.warn("Skipping {} - lot size is null or zero", futInst.getTradingSymbol());
                unmatchedFutures.add(futInst.getTradingSymbol());
                continue;
            }
            futToBase.put(futInst.getTradingSymbol(), base);
            uniqueBaseSymbols.add(base);
        }
        
        lastTotalCompanies = uniqueBaseSymbols.size();
        lastTotalFuturesProcessed = futuresList.size();
        lastFuturesSkippedNoMatch = unmatchedFutures.size();
        
        logger.info("Matched {} futures contracts across {} companies ({} unmatched)",
                futToBase.size(), uniqueBaseSymbols.size(), unmatchedFutures.size());
        
        // Step 3: Batch fetch ALL prices in parallel
        List<String> cashSymbols = new ArrayList<>(uniqueBaseSymbols);
        List<String> fnoSymbols = new ArrayList<>(futToBase.keySet());
        
        logger.info("Fetching prices: {} spot symbols, {} futures symbols in parallel batches of {}",
                cashSymbols.size(), fnoSymbols.size(), BATCH_SIZE);
        
        // Use CompletableFuture to fetch spot and futures prices concurrently
        ExecutorService batchExecutor = Executors.newFixedThreadPool(
                Math.min(4, Runtime.getRuntime().availableProcessors()));
        
        CompletableFuture<Map<String, Double>> spotFuture = CompletableFuture.supplyAsync(
                () -> fetchBatchPricesWithRetry(cashSymbols, "cash"), batchExecutor);
        
        CompletableFuture<Map<String, Double>> fnoFuture = CompletableFuture.supplyAsync(
                () -> fetchBatchPricesWithRetry(fnoSymbols, "fno"), batchExecutor);
        
        Map<String, Double> spotPrices;
        Map<String, Double> futuresPrices;
        
        try {
            // Both fetches run in parallel, wait for both
            spotPrices = spotFuture.get(120, TimeUnit.SECONDS);
            futuresPrices = fnoFuture.get(120, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Failed to fetch batch prices: {}", e.getMessage());
            spotPrices = new HashMap<>();
            futuresPrices = new HashMap<>();
        } finally {
            batchExecutor.shutdown();
        }
        
        logger.info("Fetched {} spot prices and {} futures prices",
                spotPrices.size(), futuresPrices.size());
        
        // Step 4: Calculate arbitrage opportunities in parallel using pre-fetched prices
        String currentDateTime = LocalDateTime.now().format(TS_FMT).toUpperCase(Locale.ENGLISH);
        List<ArbitrageOpportunity> opportunities = Collections.synchronizedList(new ArrayList<>());
        Set<String> successfulSymbols = ConcurrentHashMap.newKeySet();
        Set<String> failedSymbols = ConcurrentHashMap.newKeySet();
        int apiErrorCount = 0;
        
        // Build instrument lookup
        Map<String, Instrument> futInstMap = new HashMap<>();
        for (Instrument futInst : futuresList) {
            futInstMap.put(futInst.getTradingSymbol(), futInst);
        }
        
        // Use parallel stream for CPU-bound calculations (no more network calls)
        final Map<String, Double> finalSpotPrices = spotPrices;
        final Map<String, Double> finalFuturesPrices = futuresPrices;
        
        futToBase.entrySet().parallelStream().forEach(entry -> {
            String futSymbol = entry.getKey();
            String baseSymbol = entry.getValue();
            
            try {
                Double spotPrice = finalSpotPrices.get(baseSymbol);
                Double futPriceLast = finalFuturesPrices.get(futSymbol);
                
                if (spotPrice == null || futPriceLast == null) {
                    failedSymbols.add(baseSymbol);
                    return;
                }
                
                Instrument futInst = futInstMap.get(futSymbol);
                if (futInst == null) return;
                
                double lotSize = futInst.getLotSize().doubleValue();
                String expiryDate = extractExpiryDate(futSymbol);
                int holdingDays = calculateHoldingDays(currentDateTime, expiryDate);
                
                // Apply calculation logic from FNO code
                double priceDiff = futPriceLast - spotPrice;
                if (priceDiff <= 0) {
                    successfulSymbols.add(baseSymbol);
                    return; // No arbitrage (negative spread)
                }
                
                double pctPriceDiff = (spotPrice == 0) ? 0.0 : (priceDiff / (spotPrice / 100.0));
                double futInv = futPriceLast * lotSize;
                double spotInv = spotPrice * lotSize;
                double totalInv = spotInv + futInv * FUTURES_MARGIN_FRACTION;
                double totalProfit = (priceDiff * lotSize) - FIXED_COST;
                double perDay = (holdingDays > 0) ? (totalProfit / holdingDays) : 0.0;
                double perAnnumPct = (totalInv == 0) ? 0.0 : ((perDay * 365.0) / (totalInv / 100.0));
                
                successfulSymbols.add(baseSymbol);
                
                if (perAnnumPct >= minAnnualReturn) {
                    ArbitrageOpportunity opp = new ArbitrageOpportunity(
                        baseSymbol, currentDateTime, expiryDate,
                        round(futPriceLast, 2), 0.0, round(spotPrice, 2),
                        round(lotSize, 2), holdingDays,
                        round(priceDiff, 2), round(pctPriceDiff, 2),
                        round(futInv, 2), round(spotInv, 2), round(totalInv, 2),
                        round(totalProfit, 2), round(perDay, 2), round(perAnnumPct, 2)
                    );
                    opportunities.add(opp);
                }
            } catch (Exception e) {
                failedSymbols.add(baseSymbol);
                logger.error("Error calculating arbitrage for {}: {}", futSymbol, e.getMessage());
            }
        });
        
        // Count symbols that had no prices at all
        for (String base : uniqueBaseSymbols) {
            if (!successfulSymbols.contains(base) && !failedSymbols.contains(base)) {
                failedSymbols.add(base);
            }
        }
        apiErrorCount = failedSymbols.size();
        
        // Update metadata
        lastSuccessfulCompanies = successfulSymbols.size();
        lastFailedCompanies = failedSymbols.size();
        lastFuturesSkippedApiError = apiErrorCount;
        
        logger.info("Found {} arbitrage opportunities from {}/{} companies ({} failed)",
                opportunities.size(), lastSuccessfulCompanies, lastTotalCompanies, lastFailedCompanies);
        
        // Sort by annual return descending
        opportunities.sort((a, b) -> Double.compare(b.getPerAnnumReturn(), a.getPerAnnumReturn()));
        
        return opportunities;
    }
    
    /**
     * Fetch prices for a list of symbols using batch LTP API with retry.
     * Splits into batches of BATCH_SIZE and fetches each batch via the chat-backend.
     * Returns a map of symbol -> last_price.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Double> fetchBatchPricesWithRetry(List<String> symbols, String segment) {
        Map<String, Double> allPrices = new ConcurrentHashMap<>();
        
        if (symbols.isEmpty()) return allPrices;
        
        // Split into batches
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < symbols.size(); i += BATCH_SIZE) {
            batches.add(symbols.subList(i, Math.min(i + BATCH_SIZE, symbols.size())));
        }
        
        logger.info("Fetching {} {} symbols in {} batches", symbols.size(), segment, batches.size());
        
        // Process batches in parallel using CompletableFuture
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(batches.size(), 4));
        
        List<CompletableFuture<Map<String, Double>>> futures = batches.stream()
                .map(batch -> CompletableFuture.supplyAsync(() -> {
                    return fetchOneBatchWithRetry(batch, segment);
                }, executor))
                .collect(Collectors.toList());
        
        // Collect all results
        for (CompletableFuture<Map<String, Double>> future : futures) {
            try {
                Map<String, Double> batchResult = future.get(60, TimeUnit.SECONDS);
                allPrices.putAll(batchResult);
            } catch (Exception e) {
                logger.error("Batch fetch failed for {}: {}", segment, e.getMessage());
            }
        }
        
        executor.shutdown();
        
        logger.info("Successfully fetched {}/{} {} prices", allPrices.size(), symbols.size(), segment);
        return allPrices;
    }
    
    /**
     * Fetch one batch of prices with retry logic.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Double> fetchOneBatchWithRetry(List<String> batch, String segment) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Map<String, Double> result = fetchOneBatch(batch, segment);
                if (!result.isEmpty()) {
                    return result;
                }
            } catch (Exception e) {
                logger.debug("Batch {} retry {}/{}: {}", segment, attempt, MAX_RETRIES, e.getMessage());
            }
            
            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_BASE_DELAY_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return new HashMap<>();
                }
            }
        }
        
        // Fallback: try individual fetches for this batch
        logger.warn("Batch {} fetch failed after {} retries, falling back to individual fetches for {} symbols",
                segment, MAX_RETRIES, batch.size());
        return fetchIndividualFallback(batch, segment);
    }
    
    /**
     * Fetch one batch of prices via the batch LTP endpoint.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Double> fetchOneBatch(List<String> symbols, String segment) {
        Map<String, Double> prices = new HashMap<>();
        
        String url = CHAT_BACKEND_URL + "/api/stock/batch-ltp";
        
        Map<String, Object> requestBody = new HashMap<>();
        if ("cash".equals(segment)) {
            requestBody.put("cash", symbols);
            requestBody.put("fno", Collections.emptyList());
        } else {
            requestBody.put("cash", Collections.emptyList());
            requestBody.put("fno", symbols);
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            Map<String, Object> segmentData = (Map<String, Object>) body.get(segment);
            
            if (segmentData != null) {
                for (Map.Entry<String, Object> entry : segmentData.entrySet()) {
                    if (entry.getValue() instanceof Number) {
                        prices.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                    }
                }
            }
        }
        
        return prices;
    }
    
    /**
     * Fallback: fetch prices individually when batch fails.
     */
    private Map<String, Double> fetchIndividualFallback(List<String> symbols, String segment) {
        Map<String, Double> prices = new ConcurrentHashMap<>();
        
        symbols.parallelStream().forEach(symbol -> {
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    String url = CHAT_BACKEND_URL + "/api/stock/search?q=" + symbol;
                    ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                    
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        Object lastPrice = response.getBody().get("last_price");
                        if (lastPrice != null) {
                            prices.put(symbol, ((Number) lastPrice).doubleValue());
                            return;
                        }
                    }
                } catch (Exception e) {
                    if (attempt < MAX_RETRIES) {
                        try { Thread.sleep(RETRY_BASE_DELAY_MS * attempt); }
                        catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
                    }
                }
            }
        });
        
        return prices;
    }
    
    /**
     * Extract base symbol from futures contract name
     * Example: SBIN26MAYFUT -> SBIN, M&M26MAYFUT -> M&M
     */
    private String extractBaseSymbol(String futuresSymbol) {
        Matcher matcher = FUTURES_PATTERN.matcher(futuresSymbol);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        
        // Fallback: try to extract before the year+month pattern
        Pattern fallback = Pattern.compile("^(.+?)(\\d{2}[A-Z]{3}FUT)$");
        Matcher fbMatcher = fallback.matcher(futuresSymbol);
        if (fbMatcher.matches()) {
            return fbMatcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Extract expiry date from futures symbol by computing the last Thursday of the expiry month.
     * NSE convention: SYMBOL + YY + MMM + FUT (e.g., SBIN26MAYFUT = SBIN, year 2026, May)
     * The actual expiry is the LAST THURSDAY of the expiry month.
     */
    private String extractExpiryDate(String futuresSymbol) {
        try {
            Pattern pattern = Pattern.compile("(\\d{2})([A-Z]{3})FUT$");
            Matcher matcher = pattern.matcher(futuresSymbol);
            
            if (matcher.find()) {
                String yearStr = matcher.group(1);
                String monthStr = matcher.group(2);
                
                int fullYear = 2000 + Integer.parseInt(yearStr);
                Integer monthNum = MONTH_MAP.get(monthStr);
                
                if (monthNum == null) {
                    logger.debug("Unknown month abbreviation: {} in {}", monthStr, futuresSymbol);
                    return LocalDate.now().plusDays(30).format(EXP_FMT).toUpperCase(Locale.ENGLISH);
                }
                
                LocalDate lastThursday = LocalDate.of(fullYear, monthNum, 1)
                        .with(TemporalAdjusters.lastInMonth(DayOfWeek.THURSDAY));
                
                return lastThursday.format(EXP_FMT).toUpperCase(Locale.ENGLISH);
            }
        } catch (Exception e) {
            logger.debug("Could not extract expiry from {}: {}", futuresSymbol, e.getMessage());
        }
        
        return LocalDate.now().plusDays(30).format(EXP_FMT).toUpperCase(Locale.ENGLISH);
    }
    
    /**
     * Calculate holding days between current date and expiry
     */
    private int calculateHoldingDays(String nowIstStr, String expiryStr) {
        try {
            LocalDate start = LocalDate.parse(nowIstStr, TS_FMT);
            LocalDate end = LocalDate.parse(expiryStr, EXP_FMT);
            return (int) Duration.between(start.atStartOfDay(), end.atStartOfDay()).toDays();
        } catch (Exception e) {
            logger.debug("Could not calculate holding days: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Round to specified decimal places
     */
    private double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}
