package com.investment.portfolio.service;

import com.investment.portfolio.dto.ArbitrageOpportunity;
import com.investment.portfolio.entity.Instrument;
import com.investment.portfolio.repository.InstrumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
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
    
    // Pattern to extract base symbol from futures contract
    // Example: SBIN26MAYFUT -> SBIN
    private static final Pattern FUTURES_PATTERN = Pattern.compile("^([A-Z0-9]+?)\\d{2}[A-Z]{3}\\d{0,2}FUT$");
    
    @Autowired
    private InstrumentRepository instrumentRepository;
    
    @Autowired
    private RestTemplate restTemplate;
    
    private static final String CHAT_BACKEND_URL = "http://localhost:5000";
    
    /**
     * Calculate arbitrage opportunities for all available instruments
     */
    public List<ArbitrageOpportunity> calculateArbitrageOpportunities(double minAnnualReturn) {
        logger.info("Starting arbitrage opportunity calculation with min return: {}%", minAnnualReturn);
        
        // Get all NSE instruments (EQ and FUT)
        List<Instrument> allInstruments = instrumentRepository.findByExchange("NSE");
        
        // Separate EQ and FUT instruments
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
        
        // Process futures in parallel
        List<ArbitrageOpportunity> opportunities = Collections.synchronizedList(new ArrayList<>());
        int threads = Math.min(Math.max(2, Runtime.getRuntime().availableProcessors()), 8);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        
        List<Callable<Void>> tasks = futuresList.stream()
                .map(futInst -> (Callable<Void>) () -> {
                    try {
                        processOneFuture(futInst, equityMap, opportunities, minAnnualReturn);
                    } catch (Exception e) {
                        logger.error("Error processing future {}: {}", futInst.getTradingSymbol(), e.getMessage());
                    }
                    return null;
                })
                .collect(Collectors.toList());
        
        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while processing futures", e);
        } finally {
            executor.shutdown();
        }
        
        logger.info("Found {} arbitrage opportunities", opportunities.size());
        
        // Sort by annual return descending
        opportunities.sort((a, b) -> Double.compare(b.getPerAnnumReturn(), a.getPerAnnumReturn()));
        
        return opportunities;
    }
    
    /**
     * Process one futures contract and calculate arbitrage opportunity
     */
    private void processOneFuture(Instrument futInst, Map<String, Instrument> equityMap, 
                                   List<ArbitrageOpportunity> opportunities, double minAnnualReturn) {
        
        // Extract base symbol from futures contract
        String baseSymbol = extractBaseSymbol(futInst.getTradingSymbol());
        if (baseSymbol == null) {
            return;
        }
        
        // Find matching equity instrument
        Instrument eqInst = equityMap.get(baseSymbol);
        if (eqInst == null) {
            return;
        }
        
        // Fetch prices from Groww API
        Double spotPrice = fetchSpotPrice(eqInst.getTradingSymbol());
        Map<String, Double> futPrices = fetchFuturesPrices(futInst.getTradingSymbol());
        
        if (spotPrice == null || futPrices == null || futPrices.get("last") == null) {
            return;
        }
        
        Double futPriceLast = futPrices.get("last");
        Double futPriceClose = futPrices.get("close");
        Double lotSize = futInst.getLotSize() != null ? futInst.getLotSize().doubleValue() : 0.0;
        
        // Extract expiry date from futures symbol
        String expiryDate = extractExpiryDate(futInst.getTradingSymbol());
        
        // Calculate holding days
        String currentDateTime = LocalDateTime.now().format(TS_FMT).toUpperCase(Locale.ENGLISH);
        int holdingDays = calculateHoldingDays(currentDateTime, expiryDate);
        
        // Apply calculation logic from FNO code
        double priceDiff = futPriceLast - spotPrice;
        if (priceDiff <= 0) {
            return; // Only positive differences
        }
        
        double pctPriceDiff = (spotPrice == 0) ? 0.0 : (priceDiff / (spotPrice / 100.0));
        double futInv = futPriceLast * lotSize;
        double spotInv = spotPrice * lotSize;
        double totalInv = spotInv + futInv * FUTURES_MARGIN_FRACTION;
        double totalProfit = (priceDiff * lotSize) - FIXED_COST;
        double perDay = (holdingDays > 0) ? (totalProfit / holdingDays) : 0.0;
        double perAnnumPct = (totalInv == 0) ? 0.0 : ((perDay * 365.0) / (totalInv / 100.0));
        
        // Filter by minimum annual return
        if (perAnnumPct >= minAnnualReturn) {
            ArbitrageOpportunity opp = new ArbitrageOpportunity(
                baseSymbol,
                currentDateTime,
                expiryDate,
                round(futPriceLast, 2),
                round(futPriceClose, 2),
                round(spotPrice, 2),
                round(lotSize, 2),
                holdingDays,
                round(priceDiff, 2),
                round(pctPriceDiff, 2),
                round(futInv, 2),
                round(spotInv, 2),
                round(totalInv, 2),
                round(totalProfit, 2),
                round(perDay, 2),
                round(perAnnumPct, 2)
            );
            opportunities.add(opp);
        }
    }
    
    /**
     * Extract base symbol from futures contract name
     * Example: SBIN26MAYFUT -> SBIN
     */
    private String extractBaseSymbol(String futuresSymbol) {
        Matcher matcher = FUTURES_PATTERN.matcher(futuresSymbol);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        
        // Fallback: try to extract before first digit
        for (int i = 0; i < futuresSymbol.length(); i++) {
            if (Character.isDigit(futuresSymbol.charAt(i))) {
                return futuresSymbol.substring(0, i);
            }
        }
        
        return null;
    }
    
    /**
     * Extract expiry date from futures symbol
     * Example: SBIN26MAYFUT -> 26-MAY-2026
     */
    private String extractExpiryDate(String futuresSymbol) {
        try {
            // Pattern: SYMBOL + DD + MMM + YY + FUT
            Pattern pattern = Pattern.compile("(\\d{2})([A-Z]{3})(\\d{0,2})FUT$");
            Matcher matcher = pattern.matcher(futuresSymbol);
            
            if (matcher.find()) {
                String day = matcher.group(1);
                String month = matcher.group(2);
                String year = matcher.group(3);
                
                // If year is empty, assume current year
                if (year.isEmpty()) {
                    year = String.valueOf(LocalDate.now().getYear()).substring(2);
                }
                
                // Convert 2-digit year to 4-digit
                int fullYear = 2000 + Integer.parseInt(year);
                
                return String.format("%s-%s-%d", day, month, fullYear);
            }
        } catch (Exception e) {
            logger.debug("Could not extract expiry from {}: {}", futuresSymbol, e.getMessage());
        }
        
        // Default to 30 days from now
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
     * Fetch spot price from Groww API
     */
    private Double fetchSpotPrice(String symbol) {
        try {
            String url = CHAT_BACKEND_URL + "/api/stock/search?q=" + symbol;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object lastPrice = response.getBody().get("last_price");
                if (lastPrice != null) {
                    return ((Number) lastPrice).doubleValue();
                }
            }
        } catch (Exception e) {
            logger.debug("Could not fetch spot price for {}: {}", symbol, e.getMessage());
        }
        return null;
    }
    
    /**
     * Fetch futures prices from Groww API
     */
    private Map<String, Double> fetchFuturesPrices(String symbol) {
        try {
            String url = CHAT_BACKEND_URL + "/api/stock/search?q=" + symbol;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Map<String, Double> prices = new HashMap<>();
                
                Object lastPrice = body.get("last_price");
                if (lastPrice != null) {
                    prices.put("last", ((Number) lastPrice).doubleValue());
                }
                
                // Get close price from OHLC
                Object ohlc = body.get("ohlc");
                if (ohlc instanceof Map) {
                    Object close = ((Map<?, ?>) ohlc).get("close");
                    if (close != null) {
                        prices.put("close", ((Number) close).doubleValue());
                    }
                }
                
                return prices;
            }
        } catch (Exception e) {
            logger.debug("Could not fetch futures prices for {}: {}", symbol, e.getMessage());
        }
        return null;
    }
    
    /**
     * Round to specified decimal places
     */
    private double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}
