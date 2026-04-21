package com.today.fridge.recipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.recipe.entity.RecipeStep;

public interface RecipeStepRepository extends JpaRepository<RecipeStep, Long> {

}
