package com.investment.portfolio.controller;

import com.investment.portfolio.dto.GroupDetailDTO;
import com.investment.portfolio.dto.GroupSummaryDTO;
import com.investment.portfolio.entity.InvestmentGroup;
import com.investment.portfolio.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public List<InvestmentGroup> getAllGroups() {
        return groupService.getAllGroups();
    }

    @GetMapping("/summary")
    public List<GroupSummaryDTO> getGroupSummaries() {
        return groupService.getGroupSummaries();
    }

    @PostMapping
    public ResponseEntity<InvestmentGroup> createGroup(@RequestBody Map<String, String> body) {
        String groupName = body.get("groupName");
        if (groupName == null || groupName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(groupService.createGroup(groupName.trim()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        groupService.deleteGroup(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<GroupDetailDTO> getGroupDetail(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(groupService.getGroupDetail(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/instruments")
    public ResponseEntity<InvestmentGroup> assignInstruments(@PathVariable Long id, @RequestBody List<Long> instrumentIds) {
        try {
            return ResponseEntity.ok(groupService.assignInstruments(id, instrumentIds));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/instruments/{instrumentId}")
    public ResponseEntity<InvestmentGroup> addInstrument(@PathVariable Long id, @PathVariable Long instrumentId) {
        try {
            return ResponseEntity.ok(groupService.addInstrumentToGroup(id, instrumentId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/instruments/{instrumentId}")
    public ResponseEntity<InvestmentGroup> removeInstrument(@PathVariable Long id, @PathVariable Long instrumentId) {
        try {
            return ResponseEntity.ok(groupService.removeInstrumentFromGroup(id, instrumentId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
