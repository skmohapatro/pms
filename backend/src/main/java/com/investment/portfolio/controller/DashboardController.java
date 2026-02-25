package com.investment.portfolio.controller;

import com.investment.portfolio.dto.DashboardInsightsDTO;
import com.investment.portfolio.dto.DashboardSummaryDTO;
import com.investment.portfolio.dto.NewsArticleDTO;
import com.investment.portfolio.dto.WatchlistAlertDTO;
import com.investment.portfolio.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary() {
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }

    @GetMapping("/news")
    public ResponseEntity<List<NewsArticleDTO>> getNews() {
        return ResponseEntity.ok(dashboardService.getNewsWithRelevance());
    }

    @GetMapping("/insights")
    public ResponseEntity<DashboardInsightsDTO> getInsights() {
        return ResponseEntity.ok(dashboardService.getAiInsights());
    }

    @GetMapping("/watchlist")
    public ResponseEntity<List<WatchlistAlertDTO>> getWatchlistAlerts() {
        return ResponseEntity.ok(dashboardService.getWatchlistAlerts());
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshCache() {
        dashboardService.clearCache();
        return ResponseEntity.ok().build();
    }
}
