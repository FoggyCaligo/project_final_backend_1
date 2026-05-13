package com.today.fridge.recommendation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.today.fridge.recommendation.entity.RecipeConditionMap;


public interface RecipeConditionMapRepository extends JpaRepository<RecipeConditionMap, Long> {

	List<RecipeConditionMap> findByRecipe_RecipeId(Long recipeId);
	
	List<RecipeConditionMap> findByConditionCode_ConditionIdIn(List<Long> conditionIds);
	
	List<RecipeConditionMap>
	findByRecipe_RecipeIdAndConditionCode_ConditionCodeIn(
	        Long recipeId,
	        List<String> conditionCodes
	);
	
	Optional<RecipeConditionMap> findByRecipe_RecipeIdAndConditionCode_ConditionId(Long recipeId, Long conditionId);
	
	@Query("""
		    SELECT rcm
		    FROM RecipeConditionMap rcm
		    JOIN FETCH rcm.conditionCode cc
		    WHERE rcm.recipe.recipeId IN :recipeIds
		""")
		List<RecipeConditionMap> findByRecipe_RecipeIdIn(
		        @Param("recipeIds") List<Long> recipeIds
		);
}
