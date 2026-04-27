package com.today.fridge.meal.entity;

import com.today.fridge.recipe.entity.RecipeNutrition;
import com.today.fridge.user.entity.User;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal", indexes = {
        @Index(name = "idx_meal_user_date", columnList = "user_id, consumed_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meal_id")
    private Long mealId;

    // Foreign Key linking to users.user_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Foreign Key linking to recipes.recipe_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_nutrition_id", nullable = false)
    private RecipeNutrition recipeNutrition;

    @Column(name = "custom_food_name", length = 100)
    private String customFoodName;

    @Column(name = "servings", precision = 5, scale = 2, nullable = false)
    private BigDecimal servings = BigDecimal.valueOf(1.00);

    @Column(name = "source_image_url", length = 500)
    private String sourceImageUrl;

    @Column(name = "consumed_at", nullable = false)
    private LocalDateTime consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}