package com.investment.portfolio.dto;

import java.util.List;

public class DashboardInsightsDTO {
    private String marketSummary;
    private List<String> portfolioInsights;
    private List<String> watchlistAlerts;
    private List<String> riskAlerts;
    private List<String> headlines;

    public DashboardInsightsDTO() {}

    public String getMarketSummary() { return marketSummary; }
    public void setMarketSummary(String marketSummary) { this.marketSummary = marketSummary; }

    public List<String> getPortfolioInsights() { return portfolioInsights; }
    public void setPortfolioInsights(List<String> portfolioInsights) { this.portfolioInsights = portfolioInsights; }

    public List<String> getWatchlistAlerts() { return watchlistAlerts; }
    public void setWatchlistAlerts(List<String> watchlistAlerts) { this.watchlistAlerts = watchlistAlerts; }

    public List<String> getRiskAlerts() { return riskAlerts; }
    public void setRiskAlerts(List<String> riskAlerts) { this.riskAlerts = riskAlerts; }

    public List<String> getHeadlines() { return headlines; }
    public void setHeadlines(List<String> headlines) { this.headlines = headlines; }
}
