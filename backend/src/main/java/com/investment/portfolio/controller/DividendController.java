package com.investment.portfolio.controller;

import com.investment.portfolio.entity.Dividend;
import com.investment.portfolio.repository.DividendRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dividends")
public class DividendController {

    private final DividendRepository dividendRepo;

    public DividendController(DividendRepository dividendRepo) {
        this.dividendRepo = dividendRepo;
    }

    @GetMapping
    public List<Dividend> getAll() {
        return dividendRepo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dividend> getById(@PathVariable Long id) {
        return dividendRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Dividend> create(@RequestBody Dividend dividend) {
        return ResponseEntity.ok(dividendRepo.save(dividend));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Dividend> update(@PathVariable Long id, @RequestBody Dividend dividend) {
        return dividendRepo.findById(id)
                .map(existing -> {
                    existing.setSymbol(dividend.getSymbol());
                    existing.setIsin(dividend.getIsin());
                    existing.setExDate(dividend.getExDate());
                    existing.setQuantity(dividend.getQuantity());
                    existing.setDividendPerShare(dividend.getDividendPerShare());
                    existing.setNetDividendAmount(dividend.getNetDividendAmount());
                    existing.setFy(dividend.getFy());
                    return ResponseEntity.ok(dividendRepo.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (dividendRepo.existsById(id)) {
            dividendRepo.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/symbols")
    public List<String> getSymbols() {
        return dividendRepo.findDistinctSymbols();
    }

    @GetMapping("/fy")
    public List<String> getFyList() {
        return dividendRepo.findDistinctFy();
    }

    @GetMapping("/by-symbol/{symbol}")
    public ResponseEntity<DividendSummary> getBySymbol(@PathVariable String symbol) {
        List<Dividend> dividends = dividendRepo.findBySymbol(symbol.toUpperCase());
        if (dividends.isEmpty()) {
            return ResponseEntity.ok(new DividendSummary(symbol, 0.0, 0));
        }
        double totalDividend = dividends.stream()
                .mapToDouble(d -> d.getNetDividendAmount() != null ? d.getNetDividendAmount() : 0.0)
                .sum();
        return ResponseEntity.ok(new DividendSummary(symbol, totalDividend, dividends.size()));
    }

    public static class DividendSummary {
        private String symbol;
        private Double totalDividendReceived;
        private Integer dividendCount;

        public DividendSummary(String symbol, Double totalDividendReceived, Integer dividendCount) {
            this.symbol = symbol;
            this.totalDividendReceived = totalDividendReceived;
            this.dividendCount = dividendCount;
        }

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public Double getTotalDividendReceived() { return totalDividendReceived; }
        public void setTotalDividendReceived(Double totalDividendReceived) { this.totalDividendReceived = totalDividendReceived; }
        public Integer getDividendCount() { return dividendCount; }
        public void setDividendCount(Integer dividendCount) { this.dividendCount = dividendCount; }
    }
}
