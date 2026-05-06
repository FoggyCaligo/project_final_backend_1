package com.today.fridge.recipe.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.today.fridge.recipe.entity.RecipeTag;
import com.today.fridge.recipe.entity.RecipeTagSourceType;
import com.today.fridge.recipe.entity.RecipeTagType;

public interface RecipeTagRepository extends JpaRepository<RecipeTag, Long> {

    List<RecipeTag> findByRecipeIdIn(List<Long> recipeIds);

    List<RecipeTag> findByRecipeIdAndTagType(Long recipeId, RecipeTagType tagType);
    
    boolean existsByRecipeId(Long recipeId);
    
    boolean existsByRecipeIdAndSourceType(
            Long recipeId,
            RecipeTagSourceType sourceType
    );

    boolean existsByRecipeIdAndTagTypeAndTagCode(
            Long recipeId,
            RecipeTagType tagType,
            String tagCode
    );
    @Modifying
    @Transactional
    void deleteByRecipeIdAndTagType(
            Long recipeId,
            RecipeTagType tagType
    );
}