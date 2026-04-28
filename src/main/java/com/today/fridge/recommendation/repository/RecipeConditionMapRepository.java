package com.today.fridge.recommendation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.today.fridge.recommendation.entity.RecipeConditionMap;
import com.fasterxml.jackson.databind.node.LongNode;
import com.today.fridge.recipe.entity.Recipe;


public interface RecipeConditionMapRepository extends JpaRepository<RecipeConditionMap, Long> {

	List<RecipeConditionMap> findByRecipe_RecipeId(Long recipeId);
	
	List<RecipeConditionMap> findByConditionCode_ConditionIdIn(List<Long> conditionIds);
	
	List<RecipeConditionMap>
	findByRecipe_RecipeIdAndConditionCode_ConditionCodeIn(
	        Long recipeId,
	        List<String> conditionCodes
	);
	
	Optional<RecipeConditionMap> findByRecipe_RecipeIdAndConditionCode_ConditionId(Long recipeId, Long conditionId);
}
