package com.today.fridge.recipe.repository;

/*
 * UT-03
 * Method: findByRecipe_RecipeIdOrderByStepNoAsc
 * Test Name: 레시피 ID로 조리단계 조회
 * Purpose: 특정 레시피 ID와 연관된 모든 조리 단계를 번호순(Asc)으로 정확히 조회한다.
 * Input: recipeId
 * Expected Result: 해당 레시피의 단계 리스트를 반환하며, 텍스트 데이터가 일치한다.
 * Priority: Medium
 */

import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeStep;
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
class RecipeStepRepositoryTest {

        @Autowired
        private RecipeRepository recipeRepository;

        @Autowired
        private RecipeStepRepository recipeStepRepository;

        @Test
        @DisplayName("UT-RECIPE-03 - 레시피 ID로 조리단계 조회")
        void testFindByRecipe_RecipeId() {
                // Given
                Recipe recipe = Recipe.builder()
                                .title("Test Recipe")
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
                Recipe savedRecipe = recipeRepository.save(recipe);

                RecipeStep step1 = RecipeStep.builder()
                                .recipe(savedRecipe)
                                .stepNo(1)
                                .instructionText("Chop")
                                .build();
                RecipeStep step2 = RecipeStep.builder()
                                .recipe(savedRecipe)
                                .stepNo(2)
                                .instructionText("Cook")
                                .build();
                recipeStepRepository.saveAll(List.of(step1, step2));

                // When
                List<RecipeStep> foundSteps = recipeStepRepository.findByRecipe_RecipeIdOrderByStepNoAsc(savedRecipe.getRecipeId());

                // Then
                assertThat(foundSteps).hasSize(2);

                // 추가 검증: 저장된 데이터가 정확히 조회되는지 확인
                assertThat(foundSteps).extracting("instructionText")
                                .containsExactlyInAnyOrder("Chop", "Cook");
        }
}