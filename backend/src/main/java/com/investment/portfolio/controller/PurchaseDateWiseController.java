package com.investment.portfolio.controller;

import com.investment.portfolio.entity.PurchaseDateWise;
import com.investment.portfolio.repository.PurchaseDateWiseRepository;
import com.investment.portfolio.service.AggregationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseDateWiseController {

    private final PurchaseDateWiseRepository purchaseRepo;
    private final AggregationService aggregationService;

    public PurchaseDateWiseController(PurchaseDateWiseRepository purchaseRepo,
                                       AggregationService aggregationService) {
        this.purchaseRepo = purchaseRepo;
        this.aggregationService = aggregationService;
    }

    @GetMapping
    public List<PurchaseDateWise> getAll() {
        return purchaseRepo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseDateWise> getById(@PathVariable Long id) {
        return purchaseRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PurchaseDateWise> create(@RequestBody PurchaseDateWise purchase) {
        PurchaseDateWise saved = purchaseRepo.save(purchase);
        aggregationService.rebuildAggregation();
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurchaseDateWise> update(@PathVariable Long id, @RequestBody PurchaseDateWise purchase) {
        return purchaseRepo.findById(id)
                .map(existing -> {
                    existing.setDate(purchase.getDate());
                    existing.setCompany(purchase.getCompany());
                    existing.setQuantity(purchase.getQuantity());
                    existing.setPrice(purchase.getPrice());
                    existing.setInvestment(purchase.getInvestment());
                    PurchaseDateWise saved = purchaseRepo.save(existing);
                    aggregationService.rebuildAggregation();
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (purchaseRepo.existsById(id)) {
            purchaseRepo.deleteById(id);
            aggregationService.rebuildAggregation();
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/companies")
    public List<String> getCompanies() {
        return purchaseRepo.findDistinctCompanies();
    }
}
