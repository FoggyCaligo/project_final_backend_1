package com.todayfridge.backend1.domain.recipe.repository;

import com.todayfridge.backend1.domain.recipe.entity.RecipeIngredient;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {
    List<RecipeIngredient> findByRecipe_Id(Long recipeId);
}
