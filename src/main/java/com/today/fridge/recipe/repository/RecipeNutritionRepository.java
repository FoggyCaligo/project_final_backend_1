package com.today.fridge.recipe.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.today.fridge.recipe.entity.RecipeNutrition;

public interface RecipeNutritionRepository extends JpaRepository<RecipeNutrition, Long> {

    Optional<RecipeNutrition> findByRecipe_RecipeId(@Param("recipeId") Long recipeId);
}
