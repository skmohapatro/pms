package com.investment.portfolio.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "dividend")
public class Dividend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "isin")
    private String isin;

    @Column(name = "ex_date")
    private LocalDate exDate;

    @Column(name = "quantity")
    private Double quantity;

    @Column(name = "dividend_per_share")
    private Double dividendPerShare;

    @Column(name = "net_dividend_amount")
    private Double netDividendAmount;

    @Column(name = "fy")
    private String fy;

    public Dividend() {}

    public Dividend(String symbol, String isin, LocalDate exDate, Double quantity,
                    Double dividendPerShare, Double netDividendAmount, String fy) {
        this.symbol = symbol;
        this.isin = isin;
        this.exDate = exDate;
        this.quantity = quantity;
        this.dividendPerShare = dividendPerShare;
        this.netDividendAmount = netDividendAmount;
        this.fy = fy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getIsin() { return isin; }
    public void setIsin(String isin) { this.isin = isin; }

    public LocalDate getExDate() { return exDate; }
    public void setExDate(LocalDate exDate) { this.exDate = exDate; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public Double getDividendPerShare() { return dividendPerShare; }
    public void setDividendPerShare(Double dividendPerShare) { this.dividendPerShare = dividendPerShare; }

    public Double getNetDividendAmount() { return netDividendAmount; }
    public void setNetDividendAmount(Double netDividendAmount) { this.netDividendAmount = netDividendAmount; }

    public String getFy() { return fy; }
    public void setFy(String fy) { this.fy = fy; }
}
