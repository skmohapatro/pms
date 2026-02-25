package com.investment.portfolio.repository;

import com.investment.portfolio.entity.RealizedPnL;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RealizedPnLRepository extends JpaRepository<RealizedPnL, Long> {

    List<RealizedPnL> findBySymbol(String symbol);

    @Query("SELECT DISTINCT r.symbol FROM RealizedPnL r ORDER BY r.symbol")
    List<String> findDistinctSymbols();
}
