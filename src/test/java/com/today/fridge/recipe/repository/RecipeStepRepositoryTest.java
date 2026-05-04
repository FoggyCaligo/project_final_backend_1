package com.today.fridge.recipe.repository;

/*
 * RecipeStepRepositoryTest는 RecipeStepRepository의 쿼리 메서드를 테스트하는 클래스입니다.
 *
 * 주요 테스트:
 * - findByRecipe_RecipeId: 레시피 ID로 해당 레시피의 모든 단계를 정상적으로 조회하는지 확인
 */

import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeStep;
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
class RecipeStepRepositoryTest {

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private RecipeStepRepository recipeStepRepository;

    @Test
    @DisplayName("Test findByRecipe_RecipeId for Steps")
    void testFindByRecipe_RecipeId() {
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

        List<RecipeStep> foundSteps = recipeStepRepository.findByRecipe_RecipeId(savedRecipe.getRecipeId());
        
        assertThat(foundSteps).hasSize(2);
    }
}
