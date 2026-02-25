package com.investment.portfolio.service;

import com.investment.portfolio.dto.NewsArticleDTO;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class NewsService {

    private static final Logger logger = LoggerFactory.getLogger(NewsService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String GOOGLE_NEWS_RSS = "https://news.google.com/rss/search?q=%s&hl=en-IN&gl=IN&ceid=IN:en";
    private static final String ET_MARKETS_RSS = "https://economictimes.indiatimes.com/rssfeedstopstories.cms";
    private static final String MONEYCONTROL_RSS = "https://www.moneycontrol.com/rss/latestnews.xml";

    private final ConcurrentHashMap<String, CachedNews> newsCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 15 * 60 * 1000; // 15 minutes

    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    private static class CachedNews {
        List<NewsArticleDTO> articles;
        long timestamp;

        CachedNews(List<NewsArticleDTO> articles) {
            this.articles = articles;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    public List<NewsArticleDTO> fetchMarketNews() {
        CachedNews cached = newsCache.get("market");
        if (cached != null && !cached.isExpired()) {
            return cached.articles;
        }

        List<NewsArticleDTO> allNews = new ArrayList<>();

        List<Future<List<NewsArticleDTO>>> futures = new ArrayList<>();
        futures.add(executor.submit(() -> parseRssFeed(ET_MARKETS_RSS, "Economic Times")));
        futures.add(executor.submit(() -> parseRssFeed(MONEYCONTROL_RSS, "Moneycontrol")));
        futures.add(executor.submit(() -> fetchGoogleNews("Indian stock market today")));

        for (Future<List<NewsArticleDTO>> future : futures) {
            try {
                allNews.addAll(future.get(10, TimeUnit.SECONDS));
            } catch (Exception e) {
                logger.warn("Failed to fetch a news feed: {}", e.getMessage());
            }
        }

        // Sort by date descending, limit to 30
        allNews.sort((a, b) -> b.getPublishedDate().compareTo(a.getPublishedDate()));
        List<NewsArticleDTO> result = allNews.stream().limit(30).collect(Collectors.toList());

        newsCache.put("market", new CachedNews(result));
        return result;
    }

    public List<NewsArticleDTO> fetchStockNews(String symbol) {
        String cacheKey = "stock_" + symbol.toUpperCase();
        CachedNews cached = newsCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.articles;
        }

        List<NewsArticleDTO> news = fetchGoogleNews(symbol + " NSE stock");
        news.forEach(n -> n.setCategory("general"));

        newsCache.put(cacheKey, new CachedNews(news));
        return news;
    }

    public List<NewsArticleDTO> fetchPortfolioNews(List<String> holdings) {
        CachedNews cached = newsCache.get("portfolio_all");
        if (cached != null && !cached.isExpired()) {
            return cached.articles;
        }

        List<NewsArticleDTO> allNews = new CopyOnWriteArrayList<>();
        Set<String> holdingSet = holdings.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        // Fetch news for top 10 holdings in parallel
        List<String> topHoldings = holdings.stream().limit(10).collect(Collectors.toList());
        List<Future<?>> futures = new ArrayList<>();

        for (String symbol : topHoldings) {
            futures.add(executor.submit(() -> {
                try {
                    List<NewsArticleDTO> stockNews = fetchGoogleNews(symbol + " NSE stock");
                    stockNews.forEach(article -> {
                        article.setCategory("portfolio");
                        article.setRelevantHoldings(findRelevantSymbols(article, holdingSet));
                    });
                    allNews.addAll(stockNews);
                } catch (Exception e) {
                    logger.warn("Failed to fetch news for {}: {}", symbol, e.getMessage());
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get(15, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("News fetch timed out: {}", e.getMessage());
            }
        }

        // Deduplicate by title
        List<NewsArticleDTO> deduplicated = new ArrayList<>(allNews.stream()
                .collect(Collectors.toMap(NewsArticleDTO::getTitle, a -> a, (a, b) -> {
                    // Merge relevant holdings
                    Set<String> merged = new HashSet<>();
                    if (a.getRelevantHoldings() != null) merged.addAll(a.getRelevantHoldings());
                    if (b.getRelevantHoldings() != null) merged.addAll(b.getRelevantHoldings());
                    a.setRelevantHoldings(new ArrayList<>(merged));
                    return a;
                }))
                .values());

        deduplicated.sort((a, b) -> b.getPublishedDate().compareTo(a.getPublishedDate()));
        List<NewsArticleDTO> result = deduplicated.stream().limit(20).collect(Collectors.toList());

        newsCache.put("portfolio_all", new CachedNews(result));
        return result;
    }

    public List<NewsArticleDTO> fetchWatchlistNews(List<String> watchlistSymbols) {
        CachedNews cached = newsCache.get("watchlist_all");
        if (cached != null && !cached.isExpired()) {
            return cached.articles;
        }

        List<NewsArticleDTO> allNews = new CopyOnWriteArrayList<>();
        Set<String> symbolSet = watchlistSymbols.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        List<String> topSymbols = watchlistSymbols.stream().limit(10).collect(Collectors.toList());
        List<Future<?>> futures = new ArrayList<>();

        for (String symbol : topSymbols) {
            futures.add(executor.submit(() -> {
                try {
                    List<NewsArticleDTO> stockNews = fetchGoogleNews(symbol + " NSE stock");
                    stockNews.forEach(article -> {
                        article.setCategory("watchlist");
                        article.setRelevantWatchlist(findRelevantSymbols(article, symbolSet));
                    });
                    allNews.addAll(stockNews);
                } catch (Exception e) {
                    logger.warn("Failed to fetch watchlist news for {}: {}", symbol, e.getMessage());
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get(15, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("Watchlist news fetch timed out: {}", e.getMessage());
            }
        }

        List<NewsArticleDTO> deduplicated = new ArrayList<>(allNews.stream()
                .collect(Collectors.toMap(NewsArticleDTO::getTitle, a -> a, (a, b) -> {
                    Set<String> merged = new HashSet<>();
                    if (a.getRelevantWatchlist() != null) merged.addAll(a.getRelevantWatchlist());
                    if (b.getRelevantWatchlist() != null) merged.addAll(b.getRelevantWatchlist());
                    a.setRelevantWatchlist(new ArrayList<>(merged));
                    return a;
                }))
                .values());

        deduplicated.sort((a, b) -> b.getPublishedDate().compareTo(a.getPublishedDate()));
        List<NewsArticleDTO> result = deduplicated.stream().limit(20).collect(Collectors.toList());

        newsCache.put("watchlist_all", new CachedNews(result));
        return result;
    }

    public List<NewsArticleDTO> getAllNewsWithRelevance(List<String> holdings, List<String> watchlistSymbols) {
        List<NewsArticleDTO> allNews = new ArrayList<>();

        // Fetch all categories in parallel
        Future<List<NewsArticleDTO>> marketFuture = executor.submit(this::fetchMarketNews);
        Future<List<NewsArticleDTO>> portfolioFuture = executor.submit(() -> fetchPortfolioNews(holdings));
        Future<List<NewsArticleDTO>> watchlistFuture = executor.submit(() -> fetchWatchlistNews(watchlistSymbols));

        try { allNews.addAll(marketFuture.get(20, TimeUnit.SECONDS)); } catch (Exception e) { logger.warn("Market news failed"); }
        try { allNews.addAll(portfolioFuture.get(20, TimeUnit.SECONDS)); } catch (Exception e) { logger.warn("Portfolio news failed"); }
        try { allNews.addAll(watchlistFuture.get(20, TimeUnit.SECONDS)); } catch (Exception e) { logger.warn("Watchlist news failed"); }

        // Tag market news with relevant holdings/watchlist
        Set<String> holdingSet = holdings.stream().map(String::toUpperCase).collect(Collectors.toSet());
        Set<String> watchlistSet = watchlistSymbols.stream().map(String::toUpperCase).collect(Collectors.toSet());

        for (NewsArticleDTO article : allNews) {
            if (article.getRelevantHoldings() == null) {
                article.setRelevantHoldings(findRelevantSymbols(article, holdingSet));
            }
            if (article.getRelevantWatchlist() == null) {
                article.setRelevantWatchlist(findRelevantSymbols(article, watchlistSet));
            }
            if (article.getCategory() == null) {
                if (!article.getRelevantHoldings().isEmpty()) {
                    article.setCategory("portfolio");
                } else if (!article.getRelevantWatchlist().isEmpty()) {
                    article.setCategory("watchlist");
                } else {
                    article.setCategory("general");
                }
            }
        }

        // Deduplicate and sort by relevance + recency
        Map<String, NewsArticleDTO> unique = new LinkedHashMap<>();
        for (NewsArticleDTO article : allNews) {
            unique.putIfAbsent(article.getTitle(), article);
        }

        List<NewsArticleDTO> result = new ArrayList<>(unique.values());
        result.sort((a, b) -> {
            int scoreA = relevanceScore(a);
            int scoreB = relevanceScore(b);
            if (scoreA != scoreB) return scoreB - scoreA;
            return b.getPublishedDate().compareTo(a.getPublishedDate());
        });

        return result.stream().limit(50).collect(Collectors.toList());
    }

    public void clearCache() {
        newsCache.clear();
    }

    // --- Private helpers ---

    private List<NewsArticleDTO> fetchGoogleNews(String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = String.format(GOOGLE_NEWS_RSS, encoded);
            return parseRssFeed(url, "Google News");
        } catch (Exception e) {
            logger.warn("Google News fetch failed for '{}': {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<NewsArticleDTO> parseRssFeed(String feedUrl, String sourceName) {
        List<NewsArticleDTO> articles = new ArrayList<>();
        try {
            URL url = URI.create(feedUrl).toURL();
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(url));

            for (SyndEntry entry : feed.getEntries()) {
                NewsArticleDTO article = new NewsArticleDTO();
                article.setTitle(entry.getTitle() != null ? cleanHtml(entry.getTitle()) : "");
                article.setSource(sourceName);
                article.setUrl(entry.getLink() != null ? entry.getLink() : "");

                if (entry.getPublishedDate() != null) {
                    article.setPublishedDate(entry.getPublishedDate().toInstant()
                            .atZone(ZoneId.of("Asia/Kolkata"))
                            .format(DATE_FMT));
                } else {
                    article.setPublishedDate("");
                }

                String desc = "";
                if (entry.getDescription() != null) {
                    desc = cleanHtml(entry.getDescription().getValue());
                }
                article.setSnippet(desc.length() > 300 ? desc.substring(0, 300) + "..." : desc);

                articles.add(article);
            }
        } catch (Exception e) {
            logger.warn("Failed to parse RSS feed {}: {}", feedUrl, e.getMessage());
        }
        return articles.stream().limit(10).collect(Collectors.toList());
    }

    private List<String> findRelevantSymbols(NewsArticleDTO article, Set<String> symbols) {
        List<String> found = new ArrayList<>();
        String content = ((article.getTitle() != null ? article.getTitle() : "") + " " +
                (article.getSnippet() != null ? article.getSnippet() : "")).toUpperCase();
        for (String symbol : symbols) {
            if (content.contains(symbol)) {
                found.add(symbol);
            }
        }
        return found;
    }

    private int relevanceScore(NewsArticleDTO article) {
        int score = 0;
        if (article.getRelevantHoldings() != null) score += article.getRelevantHoldings().size() * 3;
        if (article.getRelevantWatchlist() != null) score += article.getRelevantWatchlist().size() * 2;
        if ("portfolio".equals(article.getCategory())) score += 2;
        if ("watchlist".equals(article.getCategory())) score += 1;
        return score;
    }

    private String cleanHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", "").replaceAll("&amp;", "&").replaceAll("&nbsp;", " ").trim();
    }
}
