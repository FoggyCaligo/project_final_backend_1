package com.today.fridge.recipe.repository;

import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
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
