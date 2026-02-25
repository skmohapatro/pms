package com.investment.portfolio.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "instruments", indexes = {
    @Index(name = "idx_trading_symbol", columnList = "trading_symbol"),
    @Index(name = "idx_name", columnList = "name"),
    @Index(name = "idx_segment", columnList = "segment")
})
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exchange")
    private String exchange;

    @Column(name = "exchange_token")
    private String exchangeToken;

    @Column(name = "trading_symbol", nullable = false)
    private String tradingSymbol;

    @Column(name = "groww_symbol")
    private String growwSymbol;

    @Column(name = "name")
    private String name;

    @Column(name = "instrument_type")
    private String instrumentType;

    @Column(name = "segment")
    private String segment;

    @Column(name = "series")
    private String series;

    @Column(name = "isin")
    private String isin;

    @Column(name = "lot_size")
    private Integer lotSize;

    @Column(name = "tick_size")
    private Double tickSize;

    public Instrument() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getExchangeToken() { return exchangeToken; }
    public void setExchangeToken(String exchangeToken) { this.exchangeToken = exchangeToken; }

    public String getTradingSymbol() { return tradingSymbol; }
    public void setTradingSymbol(String tradingSymbol) { this.tradingSymbol = tradingSymbol; }

    public String getGrowwSymbol() { return growwSymbol; }
    public void setGrowwSymbol(String growwSymbol) { this.growwSymbol = growwSymbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getInstrumentType() { return instrumentType; }
    public void setInstrumentType(String instrumentType) { this.instrumentType = instrumentType; }

    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }

    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }

    public String getIsin() { return isin; }
    public void setIsin(String isin) { this.isin = isin; }

    public Integer getLotSize() { return lotSize; }
    public void setLotSize(Integer lotSize) { this.lotSize = lotSize; }

    public Double getTickSize() { return tickSize; }
    public void setTickSize(Double tickSize) { this.tickSize = tickSize; }
}
