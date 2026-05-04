package com.today.fridge.recipe.service;

/*
 * RecipeServiceTest는 RecipeService의 getRecipe(비회원) 메서드를 통합 테스트하는 클래스입니다.
 *
 * @SpringBootTest를 사용하여 실제 데이터베이스(H2) 환경에서 테스트합니다.
 * @Transactional로 각 테스트 후 데이터가 롤백됩니다.
 *
 * 주요 테스트 시나리오:
 * 1. 정상적인 recipeId로 조회 시 레시피 정보, 영양정보, 단계, 재료가 올바르게 반환되는지 확인
 * 2. 존재하지 않는 recipeId로 조회 시 ExceptionTemplate 예외가 발생하는지 확인
 */

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
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

        // ========================================================================
        // 비회원 전용 레시피 조회 - 정상 시나리오
        // ========================================================================
        @Test
        @DisplayName("Service Integration Test for getRecipe")
        void testGetRecipe() {
                // 1. 레시피 데이터 저장
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

                // 2. 레시피 영양정보 저장
                RecipeNutrition nutrition = RecipeNutrition.builder()
                                .recipe(savedRecipe)
                                .calories(BigDecimal.valueOf(500))
                                .carbs(BigDecimal.valueOf(50))
                                .protein(BigDecimal.valueOf(20))
                                .fat(BigDecimal.valueOf(10))
                                .build();
                recipeNutritionRepository.save(nutrition);

                // 3. 레시피 단계 저장
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

                // 4. 레시피 재료 저장
                RecipeIngredient ingredient1 = RecipeIngredient.builder()
                                .recipe(savedRecipe)
                                .rawText("Onion")
                                .amountText("1")
                                .build();
                recipeIngredientRepository.save(ingredient1);

                // 5. 서비스 메서드 호출
                RecipeResponse response = recipeService.getRecipe(recipeId);

                // 6. 결과 검증
                assertThat(response.getRecipeId()).isEqualTo(recipeId);
                assertThat(response.getTitle()).isEqualTo("Test Recipe");
                assertThat(response.getCalories()).isEqualByComparingTo(BigDecimal.valueOf(500));

                assertThat(response.getRecipeSteps()).hasSize(2);
                assertThat(response.getRecipeSteps().get(0).getInstructionText()).isEqualTo("Chop the onions");

                assertThat(response.getRecipeIngredients()).hasSize(1);
                assertThat(response.getRecipeIngredients().get(0).getRawText()).isEqualTo("Onion");
        }

        // ========================================================================
        // 비회원 전용 레시피 조회 - 존재하지 않는 레시피 예외 시나리오
        // ========================================================================
        @Test
        @DisplayName("Service throws exception when Recipe is not found")
        void testGetRecipe_NotFound() {
                // 존재하지 않는 recipeId로 조회 시 ExceptionTemplate 예외가 발생해야 함
                assertThrows(ExceptionTemplate.class, () -> {
                        recipeService.getRecipe(9999L);
                });
        }
}
