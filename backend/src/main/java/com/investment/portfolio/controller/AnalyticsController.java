package com.investment.portfolio.controller;

import com.investment.portfolio.dto.MonthlyInvestmentDTO;
import com.investment.portfolio.dto.MonthlyStockDetailDTO;
import com.investment.portfolio.dto.YearlyInvestmentDTO;
import com.investment.portfolio.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/monthly")
    public List<MonthlyInvestmentDTO> getMonthlyInvestment(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String company) {
        return analyticsService.getMonthlyInvestment(startDate, endDate, company);
    }

    @GetMapping("/yearly")
    public List<YearlyInvestmentDTO> getYearlyInvestment(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String company) {
        return analyticsService.getYearlyInvestment(startDate, endDate, company);
    }

    @GetMapping("/monthly/details")
    public List<MonthlyStockDetailDTO> getMonthlyStockDetails(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String company) {
        return analyticsService.getMonthlyStockDetails(year, month, company);
    }
}
