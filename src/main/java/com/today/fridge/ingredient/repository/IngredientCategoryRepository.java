package com.today.fridge.ingredient.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.ingredient.entity.IngredientCategory;

public interface IngredientCategoryRepository extends JpaRepository<IngredientCategory, Long> {

}
