package com.today.fridge.recipe.repository;

import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeIngredient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest; // Added this import
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
        @DisplayName("레시피 ID로 해당 레시피의 모든 재료를 정상적으로 조회하는지 확인")
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
                                .findByRecipe_RecipeId(savedRecipe.getRecipeId());

                assertThat(foundIngredients).hasSize(1);
                assertThat(foundIngredients.get(0).getRawText()).isEqualTo("Onion");
        }
}