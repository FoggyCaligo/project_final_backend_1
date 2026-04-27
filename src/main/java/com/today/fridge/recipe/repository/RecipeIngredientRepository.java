package com.today.fridge.recipe.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.recipe.entity.RecipeIngredient;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {
    List<RecipeIngredient> findByRecipe_RecipeId(Long recipeId);
}
