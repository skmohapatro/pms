package com.investment.portfolio.service;

import com.investment.portfolio.dto.MonthlyInvestmentDTO;
import com.investment.portfolio.dto.MonthlyStockDetailDTO;
import com.investment.portfolio.dto.YearlyInvestmentDTO;
import com.investment.portfolio.entity.PurchaseDateWise;
import com.investment.portfolio.repository.PurchaseDateWiseRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final PurchaseDateWiseRepository purchaseRepo;

    public AnalyticsService(PurchaseDateWiseRepository purchaseRepo) {
        this.purchaseRepo = purchaseRepo;
    }

    public List<MonthlyInvestmentDTO> getMonthlyInvestment(LocalDate startDate, LocalDate endDate, String company) {
        List<PurchaseDateWise> data = getFilteredData(startDate, endDate, company);

        Map<String, Double> monthlyMap = new TreeMap<>();

        for (PurchaseDateWise p : data) {
            String key = p.getDate().getYear() + "-" + String.format("%02d", p.getDate().getMonthValue());
            monthlyMap.merge(key, p.getInvestment(), Double::sum);
        }

        List<MonthlyInvestmentDTO> result = new ArrayList<>();
        for (Map.Entry<String, Double> entry : monthlyMap.entrySet()) {
            String[] parts = entry.getKey().split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            String monthName = Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            result.add(new MonthlyInvestmentDTO(year, month, monthName + " " + year, entry.getValue()));
        }

        return result;
    }

    public List<YearlyInvestmentDTO> getYearlyInvestment(LocalDate startDate, LocalDate endDate, String company) {
        List<PurchaseDateWise> data = getFilteredData(startDate, endDate, company);

        Map<Integer, Double> yearlyMap = new TreeMap<>();

        for (PurchaseDateWise p : data) {
            yearlyMap.merge(p.getDate().getYear(), p.getInvestment(), Double::sum);
        }

        return yearlyMap.entrySet().stream()
                .map(e -> new YearlyInvestmentDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public List<MonthlyStockDetailDTO> getMonthlyStockDetails(int year, int month, String company) {
        List<PurchaseDateWise> data = purchaseRepo.findAll().stream()
                .filter(p -> p.getDate().getYear() == year && p.getDate().getMonthValue() == month)
                .collect(Collectors.toList());

        if (company != null && !company.isBlank()) {
            data = data.stream()
                    .filter(p -> p.getCompany().equalsIgnoreCase(company.trim()))
                    .collect(Collectors.toList());
        }

        Map<String, MonthlyStockDetailDTO> stockMap = new HashMap<>();
        
        for (PurchaseDateWise p : data) {
            String stockName = p.getCompany();
            if (stockMap.containsKey(stockName)) {
                MonthlyStockDetailDTO existing = stockMap.get(stockName);
                existing.setQuantityPurchased(existing.getQuantityPurchased() + p.getQuantity());
                existing.setInvestedAmount(existing.getInvestedAmount() + p.getInvestment());
            } else {
                stockMap.put(stockName, new MonthlyStockDetailDTO(stockName, p.getQuantity(), p.getInvestment()));
            }
        }

        return new ArrayList<>(stockMap.values());
    }

    private List<PurchaseDateWise> getFilteredData(LocalDate startDate, LocalDate endDate, String company) {
        List<PurchaseDateWise> data;

        if (startDate != null && endDate != null) {
            data = purchaseRepo.findByDateBetween(startDate, endDate);
        } else {
            data = purchaseRepo.findAll();
        }

        if (company != null && !company.isBlank()) {
            data = data.stream()
                    .filter(p -> p.getCompany().equalsIgnoreCase(company.trim()))
                    .collect(Collectors.toList());
        }

        return data;
    }
}
