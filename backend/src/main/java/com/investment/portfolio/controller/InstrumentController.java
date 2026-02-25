package com.investment.portfolio.controller;

import com.investment.portfolio.entity.Instrument;
import com.investment.portfolio.service.InstrumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/instruments")
@CrossOrigin(origins = "*")
public class InstrumentController {

    @Autowired
    private InstrumentService instrumentService;

    @GetMapping
    public ResponseEntity<List<Instrument>> getAllInstruments() {
        return ResponseEntity.ok(instrumentService.getAllInstruments());
    }

    @GetMapping("/nse-cash")
    public ResponseEntity<List<Instrument>> getNseCashInstruments() {
        return ResponseEntity.ok(instrumentService.getNseCashInstruments());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Instrument>> searchInstruments(@RequestParam String q) {
        List<Instrument> results = instrumentService.searchInstruments(q);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search-all")
    public ResponseEntity<List<Instrument>> searchAllInstruments(@RequestParam String q) {
        List<Instrument> results = instrumentService.searchAllInstruments(q);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Instrument> getInstrumentById(@PathVariable Long id) {
        return instrumentService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<Instrument> getInstrumentBySymbol(@PathVariable String symbol) {
        return instrumentService.findByTradingSymbol(symbol)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshInstruments() {
        Map<String, Object> result = instrumentService.refreshInstruments();
        if ((boolean) result.getOrDefault("success", false)) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(instrumentService.getStatus());
    }
}
