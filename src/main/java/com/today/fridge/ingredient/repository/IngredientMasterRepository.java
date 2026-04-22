package com.today.fridge.ingredient.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.ingredient.entity.IngredientMaster;

public interface IngredientMasterRepository extends JpaRepository<IngredientMaster, Long> {

}
