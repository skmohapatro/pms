package com.investment.portfolio.service;

import com.investment.portfolio.dto.*;
import com.investment.portfolio.entity.CompanyWiseAggregatedData;
import com.investment.portfolio.entity.Instrument;
import com.investment.portfolio.entity.WatchList;
import com.investment.portfolio.repository.CompanyWiseAggregatedDataRepository;
import com.investment.portfolio.repository.WatchListRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    private final CompanyWiseAggregatedDataRepository companyRepo;
    private final WatchListRepository watchListRepo;
    private final NewsService newsService;
    private final RestTemplate restTemplate;

    @Value("${chat.backend.url:http://localhost:5000}")
    private String chatBackendUrl;

    private final ConcurrentHashMap<String, CachedData<?>> cache = new ConcurrentHashMap<>();
    private static final long SUMMARY_CACHE_TTL = 5 * 60 * 1000;   // 5 min
    private static final long NEWS_CACHE_TTL = 15 * 60 * 1000;      // 15 min
    private static final long INSIGHTS_CACHE_TTL = 15 * 60 * 1000;  // 15 min

    private static class CachedData<T> {
        T data;
        long timestamp;
        CachedData(T data) { this.data = data; this.timestamp = System.currentTimeMillis(); }
        boolean isExpired(long ttl) { return System.currentTimeMillis() - timestamp > ttl; }
    }

    public DashboardService(CompanyWiseAggregatedDataRepository companyRepo,
                            WatchListRepository watchListRepo,
                            NewsService newsService) {
        this.companyRepo = companyRepo;
        this.watchListRepo = watchListRepo;
        this.newsService = newsService;
        this.restTemplate = new RestTemplate();
    }

    @SuppressWarnings("unchecked")
    public DashboardSummaryDTO getDashboardSummary() {
        CachedData<DashboardSummaryDTO> cached = (CachedData<DashboardSummaryDTO>) cache.get("summary");
        if (cached != null && !cached.isExpired(SUMMARY_CACHE_TTL)) {
            return cached.data;
        }

        List<CompanyWiseAggregatedData> holdings = companyRepo.findAll();
        List<WatchList> watchLists = watchListRepo.findAll();

        DashboardSummaryDTO summary = new DashboardSummaryDTO();
        summary.setTotalCompanies(holdings.size());
        summary.setTotalInvested(holdings.stream().mapToDouble(CompanyWiseAggregatedData::getInvested).sum());
        summary.setPortfolioValue(summary.getTotalInvested()); // Will be updated with live prices
        summary.setTotalPnl(0); // Will be updated with live prices

        // Count unique watchlist instruments
        Set<String> watchlistSymbols = new HashSet<>();
        for (WatchList wl : watchLists) {
            for (Instrument inst : wl.getInstruments()) {
                watchlistSymbols.add(inst.getTradingSymbol());
            }
        }
        summary.setWatchlistCount(watchlistSymbols.size());

        // Top gainers/losers will be populated from live data on the frontend
        summary.setTopGainers(Collections.emptyList());
        summary.setTopLosers(Collections.emptyList());

        cache.put("summary", new CachedData<>(summary));
        return summary;
    }

    @SuppressWarnings("unchecked")
    public List<NewsArticleDTO> getNewsWithRelevance() {
        CachedData<List<NewsArticleDTO>> cached = (CachedData<List<NewsArticleDTO>>) cache.get("news");
        if (cached != null && !cached.isExpired(NEWS_CACHE_TTL)) {
            return cached.data;
        }

        List<String> holdings = companyRepo.findAll().stream()
                .map(CompanyWiseAggregatedData::getInstrument)
                .collect(Collectors.toList());

        List<String> watchlistSymbols = getWatchlistSymbols();

        List<NewsArticleDTO> news = newsService.getAllNewsWithRelevance(holdings, watchlistSymbols);

        cache.put("news", new CachedData<>(news));
        return news;
    }

    public List<WatchlistAlertDTO> getWatchlistAlerts() {
        List<WatchList> watchLists = watchListRepo.findAll();
        List<WatchlistAlertDTO> alerts = new ArrayList<>();

        for (WatchList wl : watchLists) {
            for (Instrument inst : wl.getInstruments()) {
                // Create alert entries - prices will be enriched on frontend
                WatchlistAlertDTO alert = new WatchlistAlertDTO();
                alert.setSymbol(inst.getTradingSymbol());
                alert.setWatchlistName(wl.getName());
                alert.setAlertType("info");
                alert.setNewsCount(0);
                alerts.add(alert);
            }
        }

        // Deduplicate by symbol, keep first watchlist name
        Map<String, WatchlistAlertDTO> uniqueAlerts = new LinkedHashMap<>();
        for (WatchlistAlertDTO alert : alerts) {
            uniqueAlerts.putIfAbsent(alert.getSymbol(), alert);
        }

        return new ArrayList<>(uniqueAlerts.values());
    }

    @SuppressWarnings("unchecked")
    public DashboardInsightsDTO getAiInsights() {
        CachedData<DashboardInsightsDTO> cached = (CachedData<DashboardInsightsDTO>) cache.get("insights");
        if (cached != null && !cached.isExpired(INSIGHTS_CACHE_TTL)) {
            return cached.data;
        }

        try {
            List<String> holdings = companyRepo.findAll().stream()
                    .map(CompanyWiseAggregatedData::getInstrument)
                    .collect(Collectors.toList());
            List<String> watchlistSymbols = getWatchlistSymbols();
            List<NewsArticleDTO> news = getNewsWithRelevance();

            // Build payload for chat-backend
            Map<String, Object> payload = new HashMap<>();

            Map<String, Object> portfolio = new HashMap<>();
            portfolio.put("holdings", holdings);
            portfolio.put("totalInvested", companyRepo.findAll().stream()
                    .mapToDouble(CompanyWiseAggregatedData::getInvested).sum());
            portfolio.put("totalCompanies", holdings.size());
            payload.put("portfolio", portfolio);

            Map<String, Object> watchlist = new HashMap<>();
            watchlist.put("instruments", watchlistSymbols);
            watchlist.put("totalTracking", watchlistSymbols.size());
            payload.put("watchlist", watchlist);

            // Send top 15 news articles as context
            List<Map<String, String>> newsContext = news.stream().limit(15).map(n -> {
                Map<String, String> m = new HashMap<>();
                m.put("title", n.getTitle());
                m.put("source", n.getSource());
                m.put("date", n.getPublishedDate());
                m.put("snippet", n.getSnippet());
                m.put("category", n.getCategory());
                return m;
            }).collect(Collectors.toList());
            payload.put("news", newsContext);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<DashboardInsightsDTO> response = restTemplate.postForEntity(
                    chatBackendUrl + "/api/dashboard/insights",
                    request,
                    DashboardInsightsDTO.class
            );

            DashboardInsightsDTO insights = response.getBody();
            if (insights != null) {
                cache.put("insights", new CachedData<>(insights));
                return insights;
            }
        } catch (Exception e) {
            logger.error("Failed to get AI insights: {}", e.getMessage());
        }

        // Return fallback insights
        DashboardInsightsDTO fallback = new DashboardInsightsDTO();
        fallback.setMarketSummary("Unable to generate AI insights at this time. Please try again later.");
        fallback.setPortfolioInsights(Collections.emptyList());
        fallback.setWatchlistAlerts(Collections.emptyList());
        fallback.setRiskAlerts(Collections.emptyList());
        fallback.setHeadlines(Collections.emptyList());
        return fallback;
    }

    public void clearCache() {
        cache.clear();
        newsService.clearCache();
    }

    private List<String> getWatchlistSymbols() {
        Set<String> symbols = new HashSet<>();
        for (WatchList wl : watchListRepo.findAll()) {
            for (Instrument inst : wl.getInstruments()) {
                symbols.add(inst.getTradingSymbol());
            }
        }
        return new ArrayList<>(symbols);
    }
}
