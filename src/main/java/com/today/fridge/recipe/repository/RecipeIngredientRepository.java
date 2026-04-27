package com.today.fridge.recipe.repository;

/*
 * 이 리포지토리에서는 Recipe 객체의 Recipe_Id를 사용하여 모든 재료 정보를 반환받습니다.
 */

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.recipe.entity.RecipeIngredient;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {
    List<RecipeIngredient> findByRecipe_RecipeId(Long recipeId);
}
