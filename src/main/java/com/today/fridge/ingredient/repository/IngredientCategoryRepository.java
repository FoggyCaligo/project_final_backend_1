package com.today.fridge.ingredient.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.ingredient.entity.IngredientCategory;

import java.util.List;
import java.util.Optional;

public interface IngredientCategoryRepository extends JpaRepository<IngredientCategory, Long> {

    List<IngredientCategory> findAllByIsActiveTrueOrderBySortOrderAsc();

    Optional<IngredientCategory> findByCategoryCode(String categoryCode);
}
