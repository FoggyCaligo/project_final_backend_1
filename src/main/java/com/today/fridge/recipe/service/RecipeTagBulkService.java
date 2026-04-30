package com.today.fridge.recipe.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    	List<Recipe> recipes = recipeRepository.findByIsActiveTrue()
    	        .stream()
    	        .limit(50)
    	        .toList();

        int savedCount = 0;

        for (Recipe recipe : recipes) {
            try {
                boolean exists = recipeTagRepository.existsByRecipeId(recipe.getRecipeId());

                if (exists) {
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
                    if ("UNKNOWN".equalsIgnoreCase(tag.tagCode())) {
                        continue;
                    }

                    recipeTagRepository.saveAndFlush(
                            RecipeTag.builder()
                                    .recipeId(response.recipeId())
                                    .tagType(RecipeTagType.valueOf(tag.tagType()))
                                    .tagCode(tag.tagCode())
                                    .confidence(tag.confidence())
                                    .sourceType(RecipeTagSourceType.valueOf(tag.sourceType()))
                                    .build()
                    );

                    savedCount++;
                }

            } catch (Exception e) {
                System.out.println("[RecipeTagBulkService] 태그 생성 실패 recipeId="
                        + recipe.getRecipeId()
                        + ", title="
                        + recipe.getTitle()
                        + ", error="
                        + e.getMessage());

                continue;
            }
        }

        return savedCount;
    }
}