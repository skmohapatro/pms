package com.investment.portfolio.repository;

import com.investment.portfolio.entity.WatchList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WatchListRepository extends JpaRepository<WatchList, Long> {
    Optional<WatchList> findByName(String name);
    boolean existsByName(String name);
}
