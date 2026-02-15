package com.investment.portfolio.controller;

import com.investment.portfolio.entity.CompanyWiseAggregatedData;
import com.investment.portfolio.repository.CompanyWiseAggregatedDataRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company-wise")
public class CompanyWiseController {

    private final CompanyWiseAggregatedDataRepository aggregatedRepo;

    public CompanyWiseController(CompanyWiseAggregatedDataRepository aggregatedRepo) {
        this.aggregatedRepo = aggregatedRepo;
    }

    @GetMapping
    public List<CompanyWiseAggregatedData> getAll() {
        return aggregatedRepo.findAll();
    }
}
