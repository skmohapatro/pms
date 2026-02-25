package com.investment.portfolio.repository;

import com.investment.portfolio.entity.Dividend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DividendRepository extends JpaRepository<Dividend, Long> {

    List<Dividend> findBySymbol(String symbol);

    List<Dividend> findByFy(String fy);

    @Query("SELECT DISTINCT d.symbol FROM Dividend d ORDER BY d.symbol")
    List<String> findDistinctSymbols();

    @Query("SELECT DISTINCT d.fy FROM Dividend d ORDER BY d.fy")
    List<String> findDistinctFy();
}
