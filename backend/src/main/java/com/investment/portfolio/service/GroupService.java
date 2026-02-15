package com.investment.portfolio.service;

import com.investment.portfolio.dto.GroupDetailDTO;
import com.investment.portfolio.dto.GroupSummaryDTO;
import com.investment.portfolio.dto.InstrumentSummaryDTO;
import com.investment.portfolio.entity.CompanyWiseAggregatedData;
import com.investment.portfolio.entity.InvestmentGroup;
import com.investment.portfolio.repository.CompanyWiseAggregatedDataRepository;
import com.investment.portfolio.repository.InvestmentGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final InvestmentGroupRepository groupRepo;
    private final CompanyWiseAggregatedDataRepository aggregatedRepo;

    public GroupService(InvestmentGroupRepository groupRepo, CompanyWiseAggregatedDataRepository aggregatedRepo) {
        this.groupRepo = groupRepo;
        this.aggregatedRepo = aggregatedRepo;
    }

    public List<InvestmentGroup> getAllGroups() {
        return groupRepo.findAll();
    }

    @Transactional
    public InvestmentGroup createGroup(String groupName) {
        if (groupRepo.findByGroupName(groupName).isPresent()) {
            throw new RuntimeException("Group '" + groupName + "' already exists.");
        }
        return groupRepo.save(new InvestmentGroup(groupName));
    }

    @Transactional
    public void deleteGroup(Long groupId) {
        groupRepo.deleteById(groupId);
    }

    @Transactional
    public InvestmentGroup assignInstruments(Long groupId, List<Long> instrumentIds) {
        InvestmentGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        Set<CompanyWiseAggregatedData> instruments = instrumentIds.stream()
                .map(id -> aggregatedRepo.findById(id)
                        .orElseThrow(() -> new RuntimeException("Instrument not found: " + id)))
                .collect(Collectors.toSet());

        group.setInstruments(instruments);
        return groupRepo.save(group);
    }

    @Transactional
    public InvestmentGroup addInstrumentToGroup(Long groupId, Long instrumentId) {
        InvestmentGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        CompanyWiseAggregatedData instrument = aggregatedRepo.findById(instrumentId)
                .orElseThrow(() -> new RuntimeException("Instrument not found"));

        group.getInstruments().add(instrument);
        return groupRepo.save(group);
    }

    @Transactional
    public InvestmentGroup removeInstrumentFromGroup(Long groupId, Long instrumentId) {
        InvestmentGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        group.getInstruments().removeIf(i -> i.getId().equals(instrumentId));
        return groupRepo.save(group);
    }

    public GroupDetailDTO getGroupDetail(Long groupId) {
        InvestmentGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        List<InstrumentSummaryDTO> instruments = group.getInstruments().stream()
                .map(i -> new InstrumentSummaryDTO(i.getId(), i.getInstrument(), i.getQty(), i.getAvgCost(), i.getInvested()))
                .collect(Collectors.toList());

        double totalQty = instruments.stream().mapToDouble(InstrumentSummaryDTO::getQty).sum();
        double totalInvested = instruments.stream().mapToDouble(InstrumentSummaryDTO::getInvested).sum();

        GroupDetailDTO dto = new GroupDetailDTO();
        dto.setGroupId(group.getId());
        dto.setGroupName(group.getGroupName());
        dto.setInstruments(instruments);
        dto.setTotalQty(totalQty);
        dto.setTotalInvested(totalInvested);

        return dto;
    }

    public List<GroupSummaryDTO> getGroupSummaries() {
        return groupRepo.findAll().stream()
                .map(g -> {
                    double totalQty = g.getInstruments().stream()
                            .mapToDouble(i -> i.getQty() == null ? 0.0 : i.getQty())
                            .sum();
                    double totalInvested = g.getInstruments().stream()
                            .mapToDouble(i -> i.getInvested() == null ? 0.0 : i.getInvested())
                            .sum();
                    return new GroupSummaryDTO(g.getId(), g.getGroupName(), g.getInstruments().size(), totalQty, totalInvested);
                })
                .sorted(Comparator.comparing(GroupSummaryDTO::getGroupName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }
}
