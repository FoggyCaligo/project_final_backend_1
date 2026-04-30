package com.today.fridge.recipe.repository;

/*
 * RecipeNutritionRepositoryTest는 RecipeNutritionRepository의 쿼리 메서드를 테스트하는 클래스입니다.
 *
 * 주요 테스트:
 * - findByRecipe_RecipeId: 레시피 ID로 해당 레시피의 영양정보를 정상적으로 조회하는지 확인
 */

import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeNutrition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RecipeNutritionRepositoryTest {

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private RecipeNutritionRepository recipeNutritionRepository;

    @Test
    @DisplayName("Test findByRecipe_RecipeId for Nutrition")
    void testFindByRecipe_RecipeId() {
        Recipe recipe = Recipe.builder()
                .title("Test Recipe")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Recipe savedRecipe = recipeRepository.save(recipe);

        RecipeNutrition nutrition = RecipeNutrition.builder()
                .recipe(savedRecipe)
                .calories(BigDecimal.valueOf(500))
                .sodium(BigDecimal.valueOf(100))
                .carbs(BigDecimal.valueOf(100))
                .sugar(BigDecimal.valueOf(100))
                .protein(BigDecimal.valueOf(100))
                .fat(BigDecimal.valueOf(100))
                .build();
        recipeNutritionRepository.save(nutrition);

        Optional<RecipeNutrition> foundNutrition = recipeNutritionRepository
                .findByRecipe_RecipeId(savedRecipe.getRecipeId());

        assertThat(foundNutrition).isPresent();
        assertThat(foundNutrition.get().getCalories()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }
}
