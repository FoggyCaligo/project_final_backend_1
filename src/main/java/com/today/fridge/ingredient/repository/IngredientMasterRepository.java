package com.today.fridge.ingredient.repository;

import com.today.fridge.ingredient.entity.IngredientMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IngredientMasterRepository extends JpaRepository<IngredientMaster, Long> {

    Optional<IngredientMaster> findByNormalizedNameIgnoreCase(String normalizedName);
}
