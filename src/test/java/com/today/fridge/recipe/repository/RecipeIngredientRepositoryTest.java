package com.today.fridge.recipe.repository;

/*
 * UT-01
 * Method: findByRecipe_RecipeIdOrderBySortOrderAsc
 * Test Name: 레시피 ID로 재료 조회
 * Purpose: 특정 레시피 ID와 연관된 모든 재료 정보를 정렬 순서대로 정확히 조회한다.
 * Input: recipeId
 * Expected Result: 해당 레시피의 재료 리스트를 반환한다.
 * Priority: Medium
 */

import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeIngredient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("recipe")
class RecipeIngredientRepositoryTest {

        @Autowired
        private RecipeRepository recipeRepository;

        @Autowired
        private RecipeIngredientRepository recipeIngredientRepository;

        @Test
        @DisplayName("UT-RECIPE-01 - 레시피 ID로 재료 조회")
        void testFindByRecipe_RecipeId() {
                Recipe recipe = Recipe.builder()
                                .sourceSite("test.com")
                                .sourceRecipeKey("test")
                                .title("Test Recipe")
                                .thumbnailUrl("test.jpg")
                                .summary("This is a test recipe")
                                .servingsText("1")
                                .cookTimeText("10분")
                                .sourceUrl("test.com")
                                .isActive(true)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
                Recipe savedRecipe = recipeRepository.save(recipe);

                RecipeIngredient ingredient = RecipeIngredient.builder()
                                .recipe(savedRecipe)
                                .rawText("Onion")
                                .build();
                recipeIngredientRepository.save(ingredient);

                List<RecipeIngredient> foundIngredients = recipeIngredientRepository
                                .findByRecipe_RecipeIdOrderBySortOrderAsc(savedRecipe.getRecipeId());

                assertThat(foundIngredients).hasSize(1);
                assertThat(foundIngredients.get(0).getRawText()).isEqualTo("Onion");
        }
}