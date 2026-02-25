package com.investment.portfolio.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "realized_pnl")
public class RealizedPnL {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "isin")
    private String isin;

    @Column(name = "quantity")
    private Double quantity;

    @Column(name = "buy_value")
    private Double buyValue;

    @Column(name = "sell_value")
    private Double sellValue;

    @Column(name = "realized_pnl")
    private Double realizedPnl;

    public RealizedPnL() {}

    public RealizedPnL(String symbol, String isin, Double quantity,
                       Double buyValue, Double sellValue, Double realizedPnl) {
        this.symbol = symbol;
        this.isin = isin;
        this.quantity = quantity;
        this.buyValue = buyValue;
        this.sellValue = sellValue;
        this.realizedPnl = realizedPnl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getIsin() { return isin; }
    public void setIsin(String isin) { this.isin = isin; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public Double getBuyValue() { return buyValue; }
    public void setBuyValue(Double buyValue) { this.buyValue = buyValue; }

    public Double getSellValue() { return sellValue; }
    public void setSellValue(Double sellValue) { this.sellValue = sellValue; }

    public Double getRealizedPnl() { return realizedPnl; }
    public void setRealizedPnl(Double realizedPnl) { this.realizedPnl = realizedPnl; }
}
