package com.investment.portfolio.controller;

import com.investment.portfolio.entity.WatchList;
import com.investment.portfolio.service.WatchListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/watchlists")
@CrossOrigin(origins = "*")
public class WatchListController {

    @Autowired
    private WatchListService watchListService;

    @GetMapping
    public ResponseEntity<List<WatchList>> getAllWatchLists() {
        return ResponseEntity.ok(watchListService.getAllWatchLists());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WatchList> getWatchListById(@PathVariable Long id) {
        return watchListService.getWatchListById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createWatchList(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Name is required"));
            }

            WatchList watchList = watchListService.createWatchList(name.trim(), description);
            return ResponseEntity.ok(watchList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateWatchList(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Name is required"));
            }

            WatchList watchList = watchListService.updateWatchList(id, name.trim(), description);
            return ResponseEntity.ok(watchList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWatchList(@PathVariable Long id) {
        try {
            watchListService.deleteWatchList(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/instruments/{instrumentId}")
    public ResponseEntity<?> addInstrumentToWatchList(
            @PathVariable Long id,
            @PathVariable Long instrumentId) {
        try {
            WatchList watchList = watchListService.addInstrumentToWatchList(id, instrumentId);
            return ResponseEntity.ok(watchList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/instruments/symbol/{symbol}")
    public ResponseEntity<?> addInstrumentBySymbol(
            @PathVariable Long id,
            @PathVariable String symbol) {
        try {
            WatchList watchList = watchListService.addInstrumentBySymbol(id, symbol);
            return ResponseEntity.ok(watchList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/instruments/{instrumentId}")
    public ResponseEntity<?> removeInstrumentFromWatchList(
            @PathVariable Long id,
            @PathVariable Long instrumentId) {
        try {
            WatchList watchList = watchListService.removeInstrumentFromWatchList(id, instrumentId);
            return ResponseEntity.ok(watchList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/instruments")
    public ResponseEntity<?> addMultipleInstruments(
            @PathVariable Long id,
            @RequestBody List<Long> instrumentIds) {
        try {
            WatchList watchList = watchListService.addMultipleInstruments(id, instrumentIds);
            return ResponseEntity.ok(watchList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
