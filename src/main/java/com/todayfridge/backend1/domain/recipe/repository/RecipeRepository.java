package com.todayfridge.backend1.domain.recipe.repository;

import com.todayfridge.backend1.domain.recipe.entity.Recipe;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findTop50ByOrderByIdDesc();
    List<Recipe> findByTitleContainingIgnoreCase(String keyword);
}
