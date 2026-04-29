package com.today.fridge.recipe.repository;

import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeIngredient;
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

        List<RecipeIngredient> foundIngredients = recipeIngredientRepository.findByRecipe_RecipeId(savedRecipe.getRecipeId());
        
        assertThat(foundIngredients).hasSize(1);
        assertThat(foundIngredients.get(0).getRawText()).isEqualTo("Onion");
    }
}
