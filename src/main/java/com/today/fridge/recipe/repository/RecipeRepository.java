package com.today.fridge.recipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.recipe.entity.Recipe;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

}
