package com.investment.portfolio.dto;

public class MonthlyStockDetailDTO {
    private String stockName;
    private double quantityPurchased;
    private double investedAmount;

    public MonthlyStockDetailDTO() {
    }

    public MonthlyStockDetailDTO(String stockName, double quantityPurchased, double investedAmount) {
        this.stockName = stockName;
        this.quantityPurchased = quantityPurchased;
        this.investedAmount = investedAmount;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public double getQuantityPurchased() {
        return quantityPurchased;
    }

    public void setQuantityPurchased(double quantityPurchased) {
        this.quantityPurchased = quantityPurchased;
    }

    public double getInvestedAmount() {
        return investedAmount;
    }

    public void setInvestedAmount(double investedAmount) {
        this.investedAmount = investedAmount;
    }
}
