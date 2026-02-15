package com.investment.portfolio.repository;

import com.investment.portfolio.entity.InvestmentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvestmentGroupRepository extends JpaRepository<InvestmentGroup, Long> {
    Optional<InvestmentGroup> findByGroupName(String groupName);
}
