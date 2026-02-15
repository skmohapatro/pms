package com.investment.portfolio.dto;

public class MonthlyInvestmentDTO {
    private int year;
    private int month;
    private String monthName;
    private Double totalInvestment;

    public MonthlyInvestmentDTO() {}

    public MonthlyInvestmentDTO(int year, int month, String monthName, Double totalInvestment) {
        this.year = year;
        this.month = month;
        this.monthName = monthName;
        this.totalInvestment = totalInvestment;
    }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public String getMonthName() { return monthName; }
    public void setMonthName(String monthName) { this.monthName = monthName; }

    public Double getTotalInvestment() { return totalInvestment; }
    public void setTotalInvestment(Double totalInvestment) { this.totalInvestment = totalInvestment; }
}
