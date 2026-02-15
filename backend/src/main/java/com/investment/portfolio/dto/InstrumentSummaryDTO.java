package com.investment.portfolio.dto;

public class InstrumentSummaryDTO {
    private Long id;
    private String instrument;
    private Double qty;
    private Double avgCost;
    private Double invested;

    public InstrumentSummaryDTO() {}

    public InstrumentSummaryDTO(Long id, String instrument, Double qty, Double avgCost, Double invested) {
        this.id = id;
        this.instrument = instrument;
        this.qty = qty;
        this.avgCost = avgCost;
        this.invested = invested;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInstrument() { return instrument; }
    public void setInstrument(String instrument) { this.instrument = instrument; }

    public Double getQty() { return qty; }
    public void setQty(Double qty) { this.qty = qty; }

    public Double getAvgCost() { return avgCost; }
    public void setAvgCost(Double avgCost) { this.avgCost = avgCost; }

    public Double getInvested() { return invested; }
    public void setInvested(Double invested) { this.invested = invested; }
}
