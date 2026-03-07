package com.investment.portfolio.service;

import com.investment.portfolio.entity.Instrument;
import com.investment.portfolio.repository.InstrumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class InstrumentService {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentService.class);
    private static final String GROWW_INSTRUMENTS_CSV_URL = "https://growwapi-assets.groww.in/instruments/instrument.csv";

    @Autowired
    private InstrumentRepository instrumentRepository;

    private LocalDateTime lastRefreshTime;

    public List<Instrument> getAllInstruments() {
        return instrumentRepository.findAll();
    }

    public List<Instrument> getNseCashInstruments() {
        return instrumentRepository.findAllNseCashInstruments();
    }

    public List<Instrument> searchInstruments(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return instrumentRepository.searchNseCashInstruments(query.trim());
    }

    public List<Instrument> searchAllInstruments(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return instrumentRepository.searchInstruments(query.trim());
    }

    public Optional<Instrument> findByTradingSymbol(String symbol) {
        return instrumentRepository.findByTradingSymbol(symbol);
    }

    public Optional<Instrument> findById(Long id) {
        return instrumentRepository.findById(id);
    }

    @Transactional
    public Map<String, Object> refreshInstruments() {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            logger.info("Starting instruments refresh from Groww API...");

            List<Instrument> instruments = fetchInstrumentsFromCsv();

            instrumentRepository.deleteAllInBatch();
            instrumentRepository.flush();

            List<Instrument> savedInstruments = instrumentRepository.saveAll(instruments);
            instrumentRepository.flush();

            lastRefreshTime = LocalDateTime.now();

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Instruments refresh completed. Total: {}, Duration: {}ms", savedInstruments.size(), duration);

            result.put("success", true);
            result.put("totalInstruments", savedInstruments.size());
            result.put("nseCashCount", instrumentRepository.countBySegment("CASH"));
            result.put("durationMs", duration);
            result.put("refreshTime", lastRefreshTime.toString());

        } catch (Exception e) {
            logger.error("Error refreshing instruments: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    private List<Instrument> fetchInstrumentsFromCsv() throws Exception {
        List<Instrument> instruments = new ArrayList<>();

        URL url = new URL(GROWW_INSTRUMENTS_CSV_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            boolean isHeader = true;
            String[] headers = null;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    headers = line.split(",");
                    isHeader = false;
                    continue;
                }

                try {
                    Instrument instrument = parseCsvLine(line, headers);
                    if (instrument != null && instrument.getTradingSymbol() != null) {
                        instruments.add(instrument);
                    }
                } catch (Exception e) {
                    logger.debug("Skipping malformed line: {}", e.getMessage());
                }
            }
        }

        logger.info("Fetched {} instruments from Groww CSV", instruments.size());
        return instruments;
    }

    private Instrument parseCsvLine(String line, String[] headers) {
        String[] values = line.split(",", -1);

        if (values.length < 5) {
            return null;
        }

        Instrument instrument = new Instrument();

        for (int i = 0; i < Math.min(headers.length, values.length); i++) {
            String header = headers[i].trim().toLowerCase();
            String value = values[i].trim();

            if (value.isEmpty() || value.equalsIgnoreCase("NaN")) {
                continue;
            }

            switch (header) {
                case "exchange":
                    instrument.setExchange(value);
                    break;
                case "exchange_token":
                    instrument.setExchangeToken(value);
                    break;
                case "trading_symbol":
                    instrument.setTradingSymbol(value);
                    break;
                case "groww_symbol":
                    instrument.setGrowwSymbol(value);
                    break;
                case "name":
                    instrument.setName(value);
                    break;
                case "instrument_type":
                    instrument.setInstrumentType(value);
                    break;
                case "segment":
                    instrument.setSegment(value);
                    break;
                case "series":
                    instrument.setSeries(value);
                    break;
                case "isin":
                    instrument.setIsin(value);
                    break;
                case "lot_size":
                    try {
                        instrument.setLotSize(Integer.parseInt(value));
                    } catch (NumberFormatException ignored) {}
                    break;
                case "tick_size":
                    try {
                        instrument.setTickSize(Double.parseDouble(value));
                    } catch (NumberFormatException ignored) {}
                    break;
            }
        }

        return instrument;
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        long totalCount = instrumentRepository.count();
        long nseCashCount = instrumentRepository.countBySegment("CASH");
        long nseFnoCount = instrumentRepository.countBySegment("FNO");

        status.put("totalInstruments", totalCount);
        status.put("nseCashCount", nseCashCount);
        status.put("nseFnoCount", nseFnoCount);
        status.put("lastRefreshTime", lastRefreshTime != null ? lastRefreshTime.toString() : "Never");
        status.put("isLoaded", totalCount > 0);

        return status;
    }
}
