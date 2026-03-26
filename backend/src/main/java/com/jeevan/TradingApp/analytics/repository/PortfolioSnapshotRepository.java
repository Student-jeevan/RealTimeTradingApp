package com.jeevan.TradingApp.analytics.repository;

import com.jeevan.TradingApp.analytics.model.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {

    Optional<PortfolioSnapshot> findByUserIdAndSnapshotDate(Long userId, LocalDate snapshotDate);

    List<PortfolioSnapshot> findByUserIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            Long userId, LocalDate start, LocalDate end);

    Optional<PortfolioSnapshot> findTopByUserIdOrderBySnapshotDateDesc(Long userId);

    List<PortfolioSnapshot> findByUserIdOrderBySnapshotDateDesc(Long userId);
}
