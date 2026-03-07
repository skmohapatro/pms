package com.investment.portfolio.repository;

import com.investment.portfolio.entity.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, Long> {
    
    Optional<Instrument> findByTradingSymbol(String tradingSymbol);
    
    Optional<Instrument> findByGrowwSymbol(String growwSymbol);
    
    List<Instrument> findBySegment(String segment);
    
    List<Instrument> findByExchange(String exchange);
    
    @Query("SELECT i FROM Instrument i WHERE i.segment = 'CASH' AND i.exchange = 'NSE'")
    List<Instrument> findAllNseCashInstruments();
    
    @Query("SELECT i FROM Instrument i WHERE i.exchange = 'NSE' " +
           "AND (LOWER(i.tradingSymbol) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY CASE WHEN i.segment = 'CASH' THEN 0 ELSE 1 END, i.tradingSymbol")
    List<Instrument> searchNseCashInstruments(@Param("query") String query);
    
    @Query("SELECT i FROM Instrument i WHERE i.exchange = 'NSE' AND i.segment = 'FNO' " +
           "AND LOWER(i.tradingSymbol) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Instrument> searchNseFnoInstruments(@Param("query") String query);
    
    @Query("SELECT i FROM Instrument i WHERE " +
           "LOWER(i.tradingSymbol) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Instrument> searchInstruments(@Param("query") String query);
    
    @Modifying
    @Query("DELETE FROM Instrument i")
    void deleteAllInstruments();
    
    long countBySegment(String segment);
}
