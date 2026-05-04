package com.today.fridge.meal.repository;

import com.today.fridge.meal.entity.DayNutrition;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
