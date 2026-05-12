package com.today.fridge.recipe.repository;

/*
 * RecipeIngredientRepositoryTest는 RecipeIngredientRepository의 쿼리 메서드를 테스트하는 클래스입니다.
 *
 * 주요 테스트:
 * - findByRecipe_RecipeId: 레시피 ID로 해당 레시피의 모든 재료를 정상적으로 조회하는지 확인
 */

import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeIngredient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecipeIngredientRepositoryTest {

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private RecipeIngredientRepository recipeIngredientRepository;

    @Test
    @DisplayName("Test findByRecipe_RecipeId for Ingredients")
    void testFindByRecipe_RecipeId() {
        Recipe recipe = Recipe.builder()
                .title("Test Recipe")
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
