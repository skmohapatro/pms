package com.investment.portfolio.dto;

import java.time.LocalDateTime;

public class ArbitrageOpportunity {
    
    private String companyCode;
    private String currentDateTime;
    private String selectedExpiry;
    private Double featurePriceL;
    private Double featurePriceC;
    private Double spotPrice;
    private Double lotSize;
    private Integer holdingDays;
    private Double priceDifference;
    private Double pctPriceDifference;
    private Double futuresInvestment;
    private Double spotInvestment;
    private Double totalInvestment;
    private Double totalProfit;
    private Double perDayReturn;
    private Double perAnnumReturn;
    
    // Constructors
    public ArbitrageOpportunity() {}
    
    public ArbitrageOpportunity(String companyCode, String currentDateTime, String selectedExpiry,
                                Double featurePriceL, Double featurePriceC, Double spotPrice,
                                Double lotSize, Integer holdingDays, Double priceDifference,
                                Double pctPriceDifference, Double futuresInvestment,
                                Double spotInvestment, Double totalInvestment,
                                Double totalProfit, Double perDayReturn, Double perAnnumReturn) {
        this.companyCode = companyCode;
        this.currentDateTime = currentDateTime;
        this.selectedExpiry = selectedExpiry;
        this.featurePriceL = featurePriceL;
        this.featurePriceC = featurePriceC;
        this.spotPrice = spotPrice;
        this.lotSize = lotSize;
        this.holdingDays = holdingDays;
        this.priceDifference = priceDifference;
        this.pctPriceDifference = pctPriceDifference;
        this.futuresInvestment = futuresInvestment;
        this.spotInvestment = spotInvestment;
        this.totalInvestment = totalInvestment;
        this.totalProfit = totalProfit;
        this.perDayReturn = perDayReturn;
        this.perAnnumReturn = perAnnumReturn;
    }
    
    // Getters and Setters
    public String getCompanyCode() {
        return companyCode;
    }
    
    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }
    
    public String getCurrentDateTime() {
        return currentDateTime;
    }
    
    public void setCurrentDateTime(String currentDateTime) {
        this.currentDateTime = currentDateTime;
    }
    
    public String getSelectedExpiry() {
        return selectedExpiry;
    }
    
    public void setSelectedExpiry(String selectedExpiry) {
        this.selectedExpiry = selectedExpiry;
    }
    
    public Double getFeaturePriceL() {
        return featurePriceL;
    }
    
    public void setFeaturePriceL(Double featurePriceL) {
        this.featurePriceL = featurePriceL;
    }
    
    public Double getFeaturePriceC() {
        return featurePriceC;
    }
    
    public void setFeaturePriceC(Double featurePriceC) {
        this.featurePriceC = featurePriceC;
    }
    
    public Double getSpotPrice() {
        return spotPrice;
    }
    
    public void setSpotPrice(Double spotPrice) {
        this.spotPrice = spotPrice;
    }
    
    public Double getLotSize() {
        return lotSize;
    }
    
    public void setLotSize(Double lotSize) {
        this.lotSize = lotSize;
    }
    
    public Integer getHoldingDays() {
        return holdingDays;
    }
    
    public void setHoldingDays(Integer holdingDays) {
        this.holdingDays = holdingDays;
    }
    
    public Double getPriceDifference() {
        return priceDifference;
    }
    
    public void setPriceDifference(Double priceDifference) {
        this.priceDifference = priceDifference;
    }
    
    public Double getPctPriceDifference() {
        return pctPriceDifference;
    }
    
    public void setPctPriceDifference(Double pctPriceDifference) {
        this.pctPriceDifference = pctPriceDifference;
    }
    
    public Double getFuturesInvestment() {
        return futuresInvestment;
    }
    
    public void setFuturesInvestment(Double futuresInvestment) {
        this.futuresInvestment = futuresInvestment;
    }
    
    public Double getSpotInvestment() {
        return spotInvestment;
    }
    
    public void setSpotInvestment(Double spotInvestment) {
        this.spotInvestment = spotInvestment;
    }
    
    public Double getTotalInvestment() {
        return totalInvestment;
    }
    
    public void setTotalInvestment(Double totalInvestment) {
        this.totalInvestment = totalInvestment;
    }
    
    public Double getTotalProfit() {
        return totalProfit;
    }
    
    public void setTotalProfit(Double totalProfit) {
        this.totalProfit = totalProfit;
    }
    
    public Double getPerDayReturn() {
        return perDayReturn;
    }
    
    public void setPerDayReturn(Double perDayReturn) {
        this.perDayReturn = perDayReturn;
    }
    
    public Double getPerAnnumReturn() {
        return perAnnumReturn;
    }
    
    public void setPerAnnumReturn(Double perAnnumReturn) {
        this.perAnnumReturn = perAnnumReturn;
    }
}
