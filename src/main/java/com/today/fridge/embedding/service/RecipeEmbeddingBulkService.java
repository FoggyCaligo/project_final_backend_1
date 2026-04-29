package com.today.fridge.embedding.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.today.fridge.embedding.client.EmbeddingClient;
import com.today.fridge.embedding.entity.RecipeEmbedding;
import com.today.fridge.embedding.repository.RecipeEmbeddingRepository;
import com.today.fridge.embedding.util.VectorUtils;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.repository.RecipeIngredientRepository;
import com.today.fridge.recipe.repository.RecipeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RecipeEmbeddingBulkService {

    private static final String MODEL_NAME = "all-MiniLM-L6-v2";

    private final RecipeRepository recipeRepository;
    private final RecipeEmbeddingRepository recipeEmbeddingRepository;
    private final EmbeddingClient embeddingClient;
    private final RecipeIngredientRepository recipeIngredientRepository;

    public int generateMissingEmbeddings() {

        List<Recipe> recipes = recipeRepository.findAll();

        int generatedCount = 0;

        for (Recipe recipe : recipes) {

            boolean exists =
                    recipeEmbeddingRepository.existsByRecipeRecipeIdAndModelName(
                            recipe.getRecipeId(),
                            MODEL_NAME
                    );

            if (exists) {
                continue;
            }

            String embeddingText = buildEmbeddingText(recipe);

            List<Double> vector =
                    embeddingClient.generateEmbedding(embeddingText);

            String vectorLiteral =
                    VectorUtils.toVectorLiteral(vector);

            recipeEmbeddingRepository.insertEmbedding(
            	    recipe.getRecipeId(),
            	    embeddingText,
            	    vectorLiteral,
            	    MODEL_NAME
            	);

            generatedCount++;
        }

        return generatedCount;
    }

    private String buildEmbeddingText(
            Recipe recipe
    ) {

        List<String> ingredients =
                recipeIngredientRepository
                        .findRequiredIngredientNamesByRecipeId(
                                recipe.getRecipeId()
                        );

        String ingredientText =
                String.join(", ", ingredients);

        return """
            레시피명: %s
            요약: %s
            재료: %s
            인분: %s
            조리시간: %s
            """.formatted(
                safe(recipe.getTitle()),
                safe(recipe.getSummary()),
                safe(ingredientText),
                safe(recipe.getServingsText()),
                safe(recipe.getCookTimeText())
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}