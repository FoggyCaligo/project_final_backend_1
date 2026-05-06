package com.today.fridge.recipe.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.today.fridge.llm.client.RecipeTagClient;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeTag;
import com.today.fridge.recipe.entity.RecipeTagSourceType;
import com.today.fridge.recipe.entity.RecipeTagType;
import com.today.fridge.recipe.repository.RecipeIngredientRepository;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.recipe.repository.RecipeTagRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecipeTagBulkService {

    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeTagRepository recipeTagRepository;
    private final RecipeTagClient recipeTagClient;

    public int generateMissingTags() {

        List<Recipe> recipes = recipeRepository.findByIsActiveTrue();

        int savedCount = 0;
        int batchSize = 50;

        for (int i = 0; i < recipes.size(); i += batchSize) {

            List<Recipe> batch =
                    recipes.subList(i, Math.min(i + batchSize, recipes.size()));

            System.out.println("[RecipeTagBulk] batch start: " + i);

            for (Recipe recipe : batch) {
                try {
                    boolean llmExists =
                            recipeTagRepository.existsByRecipeIdAndSourceType(
                                    recipe.getRecipeId(),
                                    RecipeTagSourceType.LLM
                            );

                    if (llmExists) {
                        continue;
                    }

                    List<String> ingredients =
                            recipeIngredientRepository.findRequiredIngredientNamesByRecipeId(
                                    recipe.getRecipeId()
                            );

                    RecipeTagClient.RecipeTagClassifyResponse response =
                            recipeTagClient.classify(
                                    new RecipeTagClient.RecipeTagClassifyRequest(
                                            recipe.getRecipeId(),
                                            recipe.getTitle(),
                                            ingredients,
                                            recipe.getSummary()
                                    )
                            );

                    if (response == null || response.tags() == null) {
                        continue;
                    }

                    for (RecipeTagClient.RecipeTagDto tag : response.tags()) {

                        RecipeTagType tagType =
                                RecipeTagType.valueOf(tag.tagType());

                        String tagCode = tag.tagCode();

                        if ("UNKNOWN".equalsIgnoreCase(tagCode)) {
                            continue;
                        }

                        if (tagType == RecipeTagType.COOKING_TYPE) {
                            boolean sameExists =
                                    recipeTagRepository.existsByRecipeIdAndTagTypeAndTagCode(
                                            response.recipeId(),
                                            RecipeTagType.COOKING_TYPE,
                                            tagCode
                                    );

                            if (sameExists) {
                                continue;
                            }

                            recipeTagRepository.deleteByRecipeIdAndTagType(
                                    response.recipeId(),
                                    RecipeTagType.COOKING_TYPE
                            );
                        }

                        if (tagType == RecipeTagType.STYLE) {
                            boolean styleExists =
                                    recipeTagRepository.existsByRecipeIdAndTagTypeAndTagCode(
                                            response.recipeId(),
                                            RecipeTagType.STYLE,
                                            tagCode
                                    );

                            if (styleExists) {
                                continue;
                            }
                        }

                        recipeTagRepository.save(
                                RecipeTag.builder()
                                        .recipeId(response.recipeId())
                                        .tagType(tagType)
                                        .tagCode(tagCode)
                                        .confidence(tag.confidence())
                                        .sourceType(RecipeTagSourceType.LLM)
                                        .build()
                        );

                        savedCount++;
                    }

                } catch (Exception e) {
                    System.out.println(
                            "[RecipeTagBulkService] 실패 recipeId=" + recipe.getRecipeId()
                                    + ", title=" + recipe.getTitle()
                                    + ", error=" + e.getMessage()
                    );
                }
            }

            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            System.out.println("[RecipeTagBulk] batch done: " + i);
        }

        return savedCount;
    }
}