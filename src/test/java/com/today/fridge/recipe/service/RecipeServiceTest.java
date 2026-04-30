package com.today.fridge.recipe.service;

import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.recipe.dto.response.RecipeResponse;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeIngredient;
import com.today.fridge.recipe.entity.RecipeNutrition;
import com.today.fridge.recipe.entity.RecipeStep;
import com.today.fridge.recipe.repository.RecipeIngredientRepository;
import com.today.fridge.recipe.repository.RecipeNutritionRepository;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.recipe.repository.RecipeStepRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class RecipeServiceTest {

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private RecipeNutritionRepository recipeNutritionRepository;

    @Autowired
    private RecipeStepRepository recipeStepRepository;

    @Autowired
    private RecipeIngredientRepository recipeIngredientRepository;

    @Autowired
    private RecipeService recipeService;

    @Test
    @DisplayName("Service Integration Test for getRecipe")
    void testGetRecipe() {
        // Save Recipe
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
        Long recipeId = savedRecipe.getRecipeId();

        // Save Nutrition
        RecipeNutrition nutrition = RecipeNutrition.builder()
                .recipe(savedRecipe)
                .calories(BigDecimal.valueOf(500))
                .carbs(BigDecimal.valueOf(50))
                .protein(BigDecimal.valueOf(20))
                .fat(BigDecimal.valueOf(10))
                .build();
        recipeNutritionRepository.save(nutrition);

        // Save Steps
        RecipeStep step1 = RecipeStep.builder()
                .recipe(savedRecipe)
                .stepNo(1)
                .instructionText("Chop the onions")
                .build();
        RecipeStep step2 = RecipeStep.builder()
                .recipe(savedRecipe)
                .stepNo(2)
                .instructionText("Cook the onions")
                .build();
        recipeStepRepository.saveAll(List.of(step1, step2));

        // Save Ingredients
        RecipeIngredient ingredient1 = RecipeIngredient.builder()
                .recipe(savedRecipe)
                .rawText("Onion")
                .amountText("1")
                .build();
        recipeIngredientRepository.save(ingredient1);

        // Test the Service method
        RecipeResponse response = recipeService.getRecipe(recipeId);

        // Verify Service Response
        assertThat(response.getRecipeId()).isEqualTo(recipeId);
        assertThat(response.getTitle()).isEqualTo("Test Recipe");
        assertThat(response.getCalories()).isEqualByComparingTo(BigDecimal.valueOf(500));

        assertThat(response.getRecipeSteps()).hasSize(2);
        assertThat(response.getRecipeSteps().get(0).getInstructionText()).isEqualTo("Chop the onions");

        assertThat(response.getRecipeIngredients()).hasSize(1);
        assertThat(response.getRecipeIngredients().get(0).getRawText()).isEqualTo("Onion");
    }

    @Test
    @DisplayName("Service throws exception when Recipe is not found")
    void testGetRecipe_NotFound() {
        assertThrows(ExceptionTemplate.class, () -> {
            recipeService.getRecipe(9999L);
        });
    }
}
