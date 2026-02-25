package com.investment.portfolio.dto;

public class WatchlistAlertDTO {
    private String symbol;
    private String watchlistName;
    private double priceChange;
    private double priceChangePercent;
    private double lastPrice;
    private int newsCount;
    private String alertType; // "opportunity", "risk", "info"

    public WatchlistAlertDTO() {}

    public WatchlistAlertDTO(String symbol, String watchlistName, double priceChange,
                             double priceChangePercent, double lastPrice, int newsCount, String alertType) {
        this.symbol = symbol;
        this.watchlistName = watchlistName;
        this.priceChange = priceChange;
        this.priceChangePercent = priceChangePercent;
        this.lastPrice = lastPrice;
        this.newsCount = newsCount;
        this.alertType = alertType;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getWatchlistName() { return watchlistName; }
    public void setWatchlistName(String watchlistName) { this.watchlistName = watchlistName; }

    public double getPriceChange() { return priceChange; }
    public void setPriceChange(double priceChange) { this.priceChange = priceChange; }

    public double getPriceChangePercent() { return priceChangePercent; }
    public void setPriceChangePercent(double priceChangePercent) { this.priceChangePercent = priceChangePercent; }

    public double getLastPrice() { return lastPrice; }
    public void setLastPrice(double lastPrice) { this.lastPrice = lastPrice; }

    public int getNewsCount() { return newsCount; }
    public void setNewsCount(int newsCount) { this.newsCount = newsCount; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
}
