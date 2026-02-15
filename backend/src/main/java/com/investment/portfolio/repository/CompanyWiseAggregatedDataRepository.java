package com.investment.portfolio.repository;

import com.investment.portfolio.entity.CompanyWiseAggregatedData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyWiseAggregatedDataRepository extends JpaRepository<CompanyWiseAggregatedData, Long> {
    Optional<CompanyWiseAggregatedData> findByInstrument(String instrument);
}
