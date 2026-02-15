package com.investment.portfolio.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "investment_groups")
public class InvestmentGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_name", unique = true, nullable = false)
    private String groupName;

    @ManyToMany
    @JoinTable(
        name = "instrument_group_map",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "aggregated_data_id")
    )
    private Set<CompanyWiseAggregatedData> instruments = new HashSet<>();

    public InvestmentGroup() {}

    public InvestmentGroup(String groupName) {
        this.groupName = groupName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public Set<CompanyWiseAggregatedData> getInstruments() { return instruments; }
    public void setInstruments(Set<CompanyWiseAggregatedData> instruments) { this.instruments = instruments; }
}
