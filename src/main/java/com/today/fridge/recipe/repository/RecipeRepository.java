package com.today.fridge.recipe.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recommendation.dto.response.RecipeRecommendationRow;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

	@EntityGraph(attributePaths = {"recipeNutrition"})
	List<Recipe> findByIsActiveTrue();
	
	Page<Recipe> findByIsActiveTrue(Pageable pageable);
	
	@EntityGraph(attributePaths = {"recipeNutrition"})
	List<Recipe> findByRecipeIdIn(List<Long> recipeIds);
	@Query("""
		    SELECT new com.today.fridge.recommendation.dto.response.RecipeRecommendationRow(
		        r.recipeId,
		        r.title,
		        r.thumbnailUrl,
		        r.summary,
		        r.cookTimeText,
		        r.difficultyLevel
		    )
		    FROM Recipe r
		    WHERE r.isActive = true
		""")
		List<RecipeRecommendationRow> findRecommendationRows();
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
	
	@Query("""
		    SELECT r
		    FROM Recipe r
		    WHERE r.isActive = true
		    ORDER BY
		        CASE TRIM(COALESCE(r.difficultyLevel, ''))
		            WHEN '아무나' THEN 0
		            WHEN '초급' THEN 1
		            WHEN '중급' THEN 2
		            WHEN '고급' THEN 3
		            WHEN '신의경지' THEN 4
		            ELSE 99
		        END ASC
		""")
		Page<Recipe> findActiveOrderByDifficultyAsc(Pageable pageable);
}
