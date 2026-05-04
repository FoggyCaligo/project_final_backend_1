package com.today.fridge.recipe.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.today.fridge.recipe.entity.Recipe;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

	List<Recipe> findByIsActiveTrue();
	
	Page<Recipe> findByIsActiveTrue(Pageable pageable);
	
	@Query("""
		    SELECT DISTINCT r
		    FROM Recipe r
		    JOIN RecipeTag rt ON rt.recipeId = r.recipeId
		    WHERE r.isActive = true
		      AND rt.tagType = com.today.fridge.recipe.entity.RecipeTagType.COOKING_TYPE
		      AND rt.tagCode = :cookingType
		""")
		Page<Recipe> findActiveRecipesByCookingType(
		        @Param("cookingType") String cookingType,
		        Pageable pageable
		);
	@Query("""
		    SELECT DISTINCT r
		    FROM Recipe r
		    JOIN RecipeTag rt ON rt.recipeId = r.recipeId
		    WHERE r.isActive = true
		      AND rt.tagType = com.today.fridge.recipe.entity.RecipeTagType.STYLE
		      AND rt.tagCode = :style
		""")
		Page<Recipe> findActiveRecipesByStyle(
		        @Param("style") String style,
		        Pageable pageable
		);
	@Query("""
		    SELECT DISTINCT r
		    FROM Recipe r
		    JOIN RecipeTag ct ON ct.recipeId = r.recipeId
		    JOIN RecipeTag st ON st.recipeId = r.recipeId
		    WHERE r.isActive = true
		      AND ct.tagType = com.today.fridge.recipe.entity.RecipeTagType.COOKING_TYPE
		      AND ct.tagCode = :cookingType
		      AND st.tagType = com.today.fridge.recipe.entity.RecipeTagType.STYLE
		      AND st.tagCode = :style
		""")
		Page<Recipe> findActiveRecipesByCookingTypeAndStyle(
		        @Param("cookingType") String cookingType,
		        @Param("style") String style,
		        Pageable pageable
		);
}
