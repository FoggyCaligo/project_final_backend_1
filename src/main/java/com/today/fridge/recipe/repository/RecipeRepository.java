package com.today.fridge.recipe.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.recipe.entity.Recipe;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

	List<Recipe> findByIsActiveTrue();
	
	Page<Recipe> findByIsActiveTrue(Pageable pageable);
}
