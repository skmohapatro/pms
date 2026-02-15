package com.investment.portfolio.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "company_wise_aggregated_data")
public class CompanyWiseAggregatedData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instrument", unique = true)
    private String instrument;

    @Column(name = "qty")
    private Double qty;

    @Column(name = "avg_cost")
    private Double avgCost;

    @Column(name = "invested")
    private Double invested;

    public CompanyWiseAggregatedData() {}

    public CompanyWiseAggregatedData(String instrument, Double qty, Double avgCost, Double invested) {
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
