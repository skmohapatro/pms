package com.investment.portfolio.dto;

public class YearlyInvestmentDTO {
    private int year;
    private Double totalInvestment;

    public YearlyInvestmentDTO() {}

    public YearlyInvestmentDTO(int year, Double totalInvestment) {
        this.year = year;
        this.totalInvestment = totalInvestment;
    }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public Double getTotalInvestment() { return totalInvestment; }
    public void setTotalInvestment(Double totalInvestment) { this.totalInvestment = totalInvestment; }
}
