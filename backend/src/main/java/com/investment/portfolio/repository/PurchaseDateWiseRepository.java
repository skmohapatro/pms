package com.investment.portfolio.repository;

import com.investment.portfolio.entity.PurchaseDateWise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PurchaseDateWiseRepository extends JpaRepository<PurchaseDateWise, Long> {

    List<PurchaseDateWise> findByCompany(String company);

    List<PurchaseDateWise> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT DISTINCT p.company FROM PurchaseDateWise p ORDER BY p.company")
    List<String> findDistinctCompanies();
}
