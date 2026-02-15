package com.investment.portfolio.dto;

import java.util.List;

public class GroupDetailDTO {
    private Long groupId;
    private String groupName;
    private List<InstrumentSummaryDTO> instruments;
    private Double totalQty;
    private Double totalInvested;

    public GroupDetailDTO() {}

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public List<InstrumentSummaryDTO> getInstruments() { return instruments; }
    public void setInstruments(List<InstrumentSummaryDTO> instruments) { this.instruments = instruments; }

    public Double getTotalQty() { return totalQty; }
    public void setTotalQty(Double totalQty) { this.totalQty = totalQty; }

    public Double getTotalInvested() { return totalInvested; }
    public void setTotalInvested(Double totalInvested) { this.totalInvested = totalInvested; }
}
