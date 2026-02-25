package com.investment.portfolio.dto;

import java.util.List;

public class NewsArticleDTO {
    private String title;
    private String source;
    private String url;
    private String publishedDate;
    private String snippet;
    private List<String> relevantHoldings;
    private List<String> relevantWatchlist;
    private String category; // "portfolio", "watchlist", "general"

    public NewsArticleDTO() {}

    public NewsArticleDTO(String title, String source, String url, String publishedDate, String snippet) {
        this.title = title;
        this.source = source;
        this.url = url;
        this.publishedDate = publishedDate;
        this.snippet = snippet;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getPublishedDate() { return publishedDate; }
    public void setPublishedDate(String publishedDate) { this.publishedDate = publishedDate; }

    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }

    public List<String> getRelevantHoldings() { return relevantHoldings; }
    public void setRelevantHoldings(List<String> relevantHoldings) { this.relevantHoldings = relevantHoldings; }

    public List<String> getRelevantWatchlist() { return relevantWatchlist; }
    public void setRelevantWatchlist(List<String> relevantWatchlist) { this.relevantWatchlist = relevantWatchlist; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
