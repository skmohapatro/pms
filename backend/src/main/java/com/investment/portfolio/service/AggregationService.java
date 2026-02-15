package com.investment.portfolio.service;

import com.investment.portfolio.entity.CompanyWiseAggregatedData;
import com.investment.portfolio.entity.PurchaseDateWise;
import com.investment.portfolio.repository.CompanyWiseAggregatedDataRepository;
import com.investment.portfolio.repository.PurchaseDateWiseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AggregationService {

    private final PurchaseDateWiseRepository purchaseRepo;
    private final CompanyWiseAggregatedDataRepository aggregatedRepo;

    public AggregationService(PurchaseDateWiseRepository purchaseRepo,
                              CompanyWiseAggregatedDataRepository aggregatedRepo) {
        this.purchaseRepo = purchaseRepo;
        this.aggregatedRepo = aggregatedRepo;
    }

    @Transactional
    public void rebuildAggregation() {
        List<PurchaseDateWise> allPurchases = purchaseRepo.findAll();

        Map<String, List<PurchaseDateWise>> grouped = allPurchases.stream()
                .collect(Collectors.groupingBy(PurchaseDateWise::getCompany));

        // Clear existing aggregated data
        aggregatedRepo.deleteAll();

        for (Map.Entry<String, List<PurchaseDateWise>> entry : grouped.entrySet()) {
            String company = entry.getKey();
            List<PurchaseDateWise> transactions = entry.getValue();

            double totalQty = transactions.stream().mapToDouble(PurchaseDateWise::getQuantity).sum();
            double totalInvested = transactions.stream().mapToDouble(PurchaseDateWise::getInvestment).sum();
            double avgCost = totalQty > 0 ? totalInvested / totalQty : 0;

            CompanyWiseAggregatedData agg = new CompanyWiseAggregatedData(company, totalQty, avgCost, totalInvested);
            aggregatedRepo.save(agg);
        }
    }
}
