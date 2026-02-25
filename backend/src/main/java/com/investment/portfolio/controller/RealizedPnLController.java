package com.investment.portfolio.controller;

import com.investment.portfolio.entity.RealizedPnL;
import com.investment.portfolio.repository.RealizedPnLRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/realized-pnl")
public class RealizedPnLController {

    private final RealizedPnLRepository realizedPnLRepo;

    public RealizedPnLController(RealizedPnLRepository realizedPnLRepo) {
        this.realizedPnLRepo = realizedPnLRepo;
    }

    @GetMapping
    public List<RealizedPnL> getAll() {
        return realizedPnLRepo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RealizedPnL> getById(@PathVariable Long id) {
        return realizedPnLRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<RealizedPnL> create(@RequestBody RealizedPnL record) {
        return ResponseEntity.ok(realizedPnLRepo.save(record));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RealizedPnL> update(@PathVariable Long id, @RequestBody RealizedPnL record) {
        return realizedPnLRepo.findById(id)
                .map(existing -> {
                    existing.setSymbol(record.getSymbol());
                    existing.setIsin(record.getIsin());
                    existing.setQuantity(record.getQuantity());
                    existing.setBuyValue(record.getBuyValue());
                    existing.setSellValue(record.getSellValue());
                    existing.setRealizedPnl(record.getRealizedPnl());
                    return ResponseEntity.ok(realizedPnLRepo.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (realizedPnLRepo.existsById(id)) {
            realizedPnLRepo.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/symbols")
    public List<String> getSymbols() {
        return realizedPnLRepo.findDistinctSymbols();
    }
}
