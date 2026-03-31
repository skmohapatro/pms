package com.investment.portfolio.controller;

import com.investment.portfolio.dto.ArbitrageOpportunity;
import com.investment.portfolio.service.ArbitrageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/arbitrage")
@CrossOrigin(origins = "*")
public class ArbitrageController {

    @Autowired
    private ArbitrageService arbitrageService;

    /**
     * Get all arbitrage opportunities
     * @param minAnnualReturn Minimum annual return percentage (default: 5.0)
     * @return List of arbitrage opportunities
     */
    @GetMapping("/opportunities")
    public ResponseEntity<List<ArbitrageOpportunity>> getOpportunities(
            @RequestParam(defaultValue = "5.0") double minAnnualReturn) {
        
        List<ArbitrageOpportunity> opportunities = 
                arbitrageService.calculateArbitrageOpportunities(minAnnualReturn);
        
        return ResponseEntity.ok(opportunities);
    }

    /**
     * Refresh and get latest arbitrage opportunities
     * @param minAnnualReturn Minimum annual return percentage (default: 5.0)
     * @return List of arbitrage opportunities with metadata
     */
    @GetMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshOpportunities(
            @RequestParam(defaultValue = "5.0") double minAnnualReturn) {
        
        long startTime = System.currentTimeMillis();
        
        List<ArbitrageOpportunity> opportunities = 
                arbitrageService.calculateArbitrageOpportunities(minAnnualReturn);
        
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("opportunities", opportunities);
        response.put("count", opportunities.size());
        response.put("minAnnualReturn", minAnnualReturn);
        response.put("durationMs", duration);
        response.put("timestamp", System.currentTimeMillis());
        response.put("totalCompanies", arbitrageService.getLastTotalCompanies());
        response.put("successfulCompanies", arbitrageService.getLastSuccessfulCompanies());
        response.put("failedCompanies", arbitrageService.getLastFailedCompanies());
        response.put("totalFuturesProcessed", arbitrageService.getLastTotalFuturesProcessed());
        response.put("futuresSkippedNoMatch", arbitrageService.getLastFuturesSkippedNoMatch());
        response.put("futuresSkippedApiError", arbitrageService.getLastFuturesSkippedApiError());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get arbitrage opportunities filtered by symbol
     * @param symbol Base symbol to filter (e.g., SBIN)
     * @param minAnnualReturn Minimum annual return percentage (default: 5.0)
     * @return List of arbitrage opportunities for the symbol
     */
    @GetMapping("/opportunities/{symbol}")
    public ResponseEntity<List<ArbitrageOpportunity>> getOpportunitiesBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "5.0") double minAnnualReturn) {
        
        List<ArbitrageOpportunity> allOpportunities = 
                arbitrageService.calculateArbitrageOpportunities(minAnnualReturn);
        
        List<ArbitrageOpportunity> filtered = allOpportunities.stream()
                .filter(opp -> opp.getCompanyCode().equalsIgnoreCase(symbol))
                .toList();
        
        return ResponseEntity.ok(filtered);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Arbitrage Opportunity Service");
        return ResponseEntity.ok(health);
    }
}
