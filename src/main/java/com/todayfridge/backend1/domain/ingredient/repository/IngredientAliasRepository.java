package com.todayfridge.backend1.domain.ingredient.repository;

import com.todayfridge.backend1.domain.ingredient.entity.IngredientAlias;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientAliasRepository extends JpaRepository<IngredientAlias, Long> {
}
