package com.today.fridge.meal.repository;

import com.today.fridge.meal.entity.HealthReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HealthReportRepository extends JpaRepository<HealthReport, Long> {
    List<HealthReport> findByUserUserIdOrderByCreatedAtDesc(Long userId);
    
    // Find latest report
    Optional<HealthReport> findFirstByUserUserIdOrderByCreatedAtDesc(Long userId);
}
