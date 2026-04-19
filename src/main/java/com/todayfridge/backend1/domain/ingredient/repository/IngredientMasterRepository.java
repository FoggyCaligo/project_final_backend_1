package com.todayfridge.backend1.domain.ingredient.repository;

import com.todayfridge.backend1.domain.ingredient.entity.IngredientMaster;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientMasterRepository extends JpaRepository<IngredientMaster, Long> {
    Optional<IngredientMaster> findByNormalizedName(String normalizedName);
}
