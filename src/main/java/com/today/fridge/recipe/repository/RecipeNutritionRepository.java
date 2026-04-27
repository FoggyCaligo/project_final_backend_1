package com.today.fridge.recipe.repository;

/*
 * 이 리포지토리에서는 Recipe 객체의 Recipe_Id를 사용하여 모든 영양 정보를 반환받습니다.
 */

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.today.fridge.recipe.entity.RecipeNutrition;

public interface RecipeNutritionRepository extends JpaRepository<RecipeNutrition, Long> {

    Optional<RecipeNutrition> findByRecipe_RecipeId(@Param("recipeId") Long recipeId);
}
