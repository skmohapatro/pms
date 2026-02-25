package com.investment.portfolio.dto;

import java.util.List;

public class DashboardSummaryDTO {
    private double totalInvested;
    private int totalCompanies;
    private double totalPnl;
    private double portfolioValue;
    private int watchlistCount;
    private List<StockMoverDTO> topGainers;
    private List<StockMoverDTO> topLosers;

    public DashboardSummaryDTO() {}

    public double getTotalInvested() { return totalInvested; }
    public void setTotalInvested(double totalInvested) { this.totalInvested = totalInvested; }

    public int getTotalCompanies() { return totalCompanies; }
    public void setTotalCompanies(int totalCompanies) { this.totalCompanies = totalCompanies; }

    public double getTotalPnl() { return totalPnl; }
    public void setTotalPnl(double totalPnl) { this.totalPnl = totalPnl; }

    public double getPortfolioValue() { return portfolioValue; }
    public void setPortfolioValue(double portfolioValue) { this.portfolioValue = portfolioValue; }

    public int getWatchlistCount() { return watchlistCount; }
    public void setWatchlistCount(int watchlistCount) { this.watchlistCount = watchlistCount; }

    public List<StockMoverDTO> getTopGainers() { return topGainers; }
    public void setTopGainers(List<StockMoverDTO> topGainers) { this.topGainers = topGainers; }

    public List<StockMoverDTO> getTopLosers() { return topLosers; }
    public void setTopLosers(List<StockMoverDTO> topLosers) { this.topLosers = topLosers; }

    public static class StockMoverDTO {
        private String symbol;
        private double changePercent;
        private double lastPrice;
        private String source; // "portfolio" or "watchlist"

        public StockMoverDTO() {}

        public StockMoverDTO(String symbol, double changePercent, double lastPrice, String source) {
            this.symbol = symbol;
            this.changePercent = changePercent;
            this.lastPrice = lastPrice;
            this.source = source;
        }

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public double getChangePercent() { return changePercent; }
        public void setChangePercent(double changePercent) { this.changePercent = changePercent; }

        public double getLastPrice() { return lastPrice; }
        public void setLastPrice(double lastPrice) { this.lastPrice = lastPrice; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }
}
