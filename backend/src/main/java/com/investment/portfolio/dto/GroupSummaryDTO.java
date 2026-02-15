package com.investment.portfolio.dto;

public class GroupSummaryDTO {
    private Long groupId;
    private String groupName;
    private int instrumentCount;
    private double totalQty;
    private double totalInvested;

    public GroupSummaryDTO() {
    }

    public GroupSummaryDTO(Long groupId, String groupName, int instrumentCount, double totalQty, double totalInvested) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.instrumentCount = instrumentCount;
        this.totalQty = totalQty;
        this.totalInvested = totalInvested;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getInstrumentCount() {
        return instrumentCount;
    }

    public void setInstrumentCount(int instrumentCount) {
        this.instrumentCount = instrumentCount;
    }

    public double getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(double totalQty) {
        this.totalQty = totalQty;
    }

    public double getTotalInvested() {
        return totalInvested;
    }

    public void setTotalInvested(double totalInvested) {
        this.totalInvested = totalInvested;
    }
}
