package com.todayfridge.backend1.domain.recipe.repository;

import com.todayfridge.backend1.domain.recipe.entity.RecipeStep;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeStepRepository extends JpaRepository<RecipeStep, Long> {
    List<RecipeStep> findByRecipe_IdOrderByStepOrderAsc(Long recipeId);
}
