package com.today.fridge.meal.repository;

import com.today.fridge.meal.entity.DayNutrition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DayNutritionRepository extends JpaRepository<DayNutrition, Long> {

    Optional<DayNutrition> findByUserUserIdAndDate(Long userId, LocalDateTime date);

    List<DayNutrition> findByUserUserIdAndDateBetweenOrderByDateAsc(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT new com.today.fridge.meal.dto.response.MealNutritionSummaryDTO(
                COALESCE(SUM(dn.totalCalories), 0),
                COALESCE(SUM(dn.totalCarbs), 0),
                COALESCE(SUM(dn.totalProtein), 0),
                COALESCE(SUM(dn.totalFat), 0),
                COALESCE(SUM(dn.totalSugar), 0),
                COALESCE(SUM(dn.totalSodium), 0),
                COALESCE(SUM(dn.totalCholesterol), 0)
            )
            FROM DayNutrition dn
            WHERE dn.user.userId = :userId
            AND dn.date >= :startDate
            AND dn.date < :endDate
            """)
    com.today.fridge.meal.dto.response.MealNutritionSummaryDTO getNutritionSummaryByDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
