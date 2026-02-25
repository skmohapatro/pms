package com.investment.portfolio.controller;

import com.investment.portfolio.repository.*;
import com.investment.portfolio.service.AggregationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = "*")
public class DataManagementController {

    private final PurchaseDateWiseRepository purchaseRepo;
    private final CompanyWiseAggregatedDataRepository companyWiseRepo;
    private final InvestmentGroupRepository groupRepo;
    private final DividendRepository dividendRepo;
    private final RealizedPnLRepository realizedPnLRepo;
    private final AggregationService aggregationService;

    public DataManagementController(
            PurchaseDateWiseRepository purchaseRepo,
            CompanyWiseAggregatedDataRepository companyWiseRepo,
            InvestmentGroupRepository groupRepo,
            DividendRepository dividendRepo,
            RealizedPnLRepository realizedPnLRepo,
            AggregationService aggregationService) {
        this.purchaseRepo = purchaseRepo;
        this.companyWiseRepo = companyWiseRepo;
        this.groupRepo = groupRepo;
        this.dividendRepo = dividendRepo;
        this.realizedPnLRepo = realizedPnLRepo;
        this.aggregationService = aggregationService;
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncAllTables() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Re-aggregate company wise data from purchases
            aggregationService.rebuildAggregation();
            
            long purchaseCount = purchaseRepo.count();
            long companyWiseCount = companyWiseRepo.count();
            
            result.put("success", true);
            result.put("message", "Data synchronized successfully");
            result.put("purchaseRecords", purchaseCount);
            result.put("companyWiseRecords", companyWiseCount);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Sync failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @GetMapping("/tables")
    public ResponseEntity<List<Map<String, Object>>> getTableInfo() {
        List<Map<String, Object>> tables = List.of(
            createTableInfo("purchases", "Purchase Date Wise", purchaseRepo.count()),
            createTableInfo("companyWise", "Company Wise Aggregated", companyWiseRepo.count()),
            createTableInfo("groups", "Groups", groupRepo.count()),
            createTableInfo("dividends", "Dividends", dividendRepo.count()),
            createTableInfo("realizedPnl", "Realized P&L", realizedPnLRepo.count())
        );
        return ResponseEntity.ok(tables);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteSelectedTables(@RequestBody List<String> tableNames) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Long> deletedCounts = new HashMap<>();
        
        try {
            // Delete in correct order due to FK constraints:
            // 1. Groups (references companyWise)
            // 2. CompanyWise
            // 3. Others (no dependencies)
            
            if (tableNames.contains("groups")) {
                long count = groupRepo.count();
                groupRepo.deleteAll();
                deletedCounts.put("groups", count);
            }
            
            if (tableNames.contains("companyWise")) {
                // Must delete groups first if not already deleted
                if (!tableNames.contains("groups")) {
                    groupRepo.deleteAll();
                }
                long count = companyWiseRepo.count();
                companyWiseRepo.deleteAll();
                deletedCounts.put("companyWise", count);
            }
            
            if (tableNames.contains("purchases")) {
                long count = purchaseRepo.count();
                purchaseRepo.deleteAll();
                deletedCounts.put("purchases", count);
            }
            
            if (tableNames.contains("dividends")) {
                long count = dividendRepo.count();
                dividendRepo.deleteAll();
                deletedCounts.put("dividends", count);
            }
            
            if (tableNames.contains("realizedPnl")) {
                long count = realizedPnLRepo.count();
                realizedPnLRepo.deleteAll();
                deletedCounts.put("realizedPnl", count);
            }
            
            result.put("success", true);
            result.put("message", "Selected tables deleted successfully");
            result.put("deletedCounts", deletedCounts);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Delete failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    private Map<String, Object> createTableInfo(String key, String displayName, long count) {
        Map<String, Object> info = new HashMap<>();
        info.put("key", key);
        info.put("displayName", displayName);
        info.put("recordCount", count);
        return info;
    }
}
