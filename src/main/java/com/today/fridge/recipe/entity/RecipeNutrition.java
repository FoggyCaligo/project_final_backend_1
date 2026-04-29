package com.today.fridge.recipe.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recipe_nutrition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeNutrition {

    @Id
    @Column(name = "recipe_nutrition_id")
    private Long recipeNutritionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @Column(name = "reference_weight", precision = 8, scale = 2)
    private BigDecimal referenceWeight;

    @Column(name = "calories", precision = 8, scale = 2, nullable = false)
    private BigDecimal calories;

    @Column(name = "carbs", precision = 8, scale = 2, nullable = false)
    private BigDecimal carbs;

    @Column(name = "protein", precision = 8, scale = 2, nullable = false)
    private BigDecimal protein;

    @Column(name = "fat", precision = 8, scale = 2, nullable = false)
    private BigDecimal fat;

    @Column(name = "sugar", precision = 8, scale = 2)
    private BigDecimal sugar;

    @Column(name = "sodium", precision = 8, scale = 2)
    private BigDecimal sodium;

    @Column(name = "cholesterol", precision = 8, scale = 2)
    private BigDecimal cholesterol;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}