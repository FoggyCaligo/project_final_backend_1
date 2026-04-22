package com.today.fridge.recipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.recipe.entity.RecipeIngredient;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {

}
