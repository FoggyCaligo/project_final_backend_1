package com.today.fridge.meal.entity;

import com.today.fridge.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "day_nutrition")
@Getter
@Setter
@NoArgsConstructor
public class DayNutrition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "day_nutrition_id")
    private Long dayNutritionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_calories", precision = 6, scale = 2, nullable = false)
    private BigDecimal totalCalories;

    @Column(name = "total_carbs", precision = 6, scale = 2, nullable = false)
    private BigDecimal totalCarbs;

    @Column(name = "total_protein", precision = 6, scale = 2, nullable = false)
    private BigDecimal totalProtein;

    @Column(name = "total_fat", precision = 6, scale = 2, nullable = false)
    private BigDecimal totalFat;

    @Column(name = "total_sugar", precision = 6, scale = 2, nullable = false)
    private BigDecimal totalSugar;

    @Column(name = "total_sodium", precision = 6, scale = 2, nullable = false)
    private BigDecimal totalSodium;

    @Column(name = "total_cholesterol", precision = 6, scale = 2, nullable = false)
    private BigDecimal totalCholesterol;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;
}
