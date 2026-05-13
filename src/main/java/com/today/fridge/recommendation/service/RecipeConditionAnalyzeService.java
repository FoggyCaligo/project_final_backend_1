package com.today.fridge.recommendation.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.today.fridge.embedding.client.EmbeddingClient;
import com.today.fridge.embedding.entity.RecipeEmbedding;
import com.today.fridge.embedding.repository.RecipeEmbeddingRepository;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.repository.RecipeIngredientRepository;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.recommendation.entity.ConditionCode;
import com.today.fridge.recommendation.entity.RecipeConditionMap;
import com.today.fridge.recommendation.repository.ConditionCodeRepository;
import com.today.fridge.recommendation.repository.RecipeConditionMapRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeConditionAnalyzeService {

    private static final double RECOMMENDED_THRESHOLD = 0.73;
    private static final double ALLOWED_THRESHOLD = 0.65;

    private final RecipeRepository recipeRepository;
    private final ConditionCodeRepository conditionCodeRepository;
    private final RecipeConditionMapRepository recipeConditionMapRepository;
    private final RecipeEmbeddingRepository recipeEmbeddingRepository;
    private final EmbeddingClient embeddingClient;
    private final RecipeIngredientRepository recipeIngredientRepository;
    
    private boolean containsAny(
            String text,
            List<String> keywords
    ) {
        return keywords.stream()
                .anyMatch(text::contains);
    }

    public void analyzeAllRecipes() {
        List<Recipe> recipes =
                recipeRepository.findByIsActiveTrue();

        for (Recipe recipe : recipes) {
            analyzeAndSave(
                    recipe.getRecipeId()
            );
        }
    }
    
    @Transactional
    public void analyzeAndSave(Long recipeId) {
    	Recipe recipe = recipeRepository.findById(recipeId)
    	        .orElse(null);

    	if (recipe == null) {
    	    log.warn("[CONDITION_ANALYZE_SKIP] recipe not found recipeId={}", recipeId);
    	    return;
    	}

        RecipeEmbedding recipeEmbedding = recipeEmbeddingRepository
                .findByRecipe_RecipeIdAndIsActiveTrue(recipeId)
                .orElse(null);
        
        if (recipeEmbedding == null) {
            log.warn("[CONDITION_ANALYZE_SKIP] embedding not found recipeId={}", recipeId);
            return;
        }
        List<String> ingredientNames =
                recipeIngredientRepository.findRequiredIngredientNamesByRecipeId(recipeId);

        String ingredientText =
                String.join(" ", ingredientNames);
        List<Double> recipeVector = parseVector(recipeEmbedding.getEmbedding());

        List<ConditionCode> conditions = conditionCodeRepository.findByIsActiveTrue();

        for (ConditionCode condition : conditions) {
            if (condition.getDescription() == null || condition.getDescription().isBlank()) {
                continue;
            }
            if ("ALLERGY".equals(condition.getConditionGroup().name())) {
                continue;
            }
            List<Double> conditionVector = embeddingClient.generateEmbedding(condition.getDescription());

            double similarity = cosineSimilarity(recipeVector, conditionVector);

            String fitType = resolveFitType(condition, similarity);

            if (fitType == null) {
                continue;
            }

            if ("LOW_SODIUM".equals(condition.getConditionCode())
                    && isLowSodiumCaution(condition, recipe, ingredientText)) {
                fitType = "CAUTION";
            }

            if ("DIET_LOW_CALORIE".equals(condition.getConditionCode())
                    && isDietCaution(condition, recipe, ingredientText)) {
                fitType = "CAUTION";
            }

            final String finalFitType = fitType;
            final BigDecimal finalConfidenceScore = BigDecimal.valueOf(similarity);

            RecipeConditionMap map = recipeConditionMapRepository
                    .findByRecipe_RecipeIdAndConditionCode_ConditionId(
                            recipeId,
                            condition.getConditionId()
                    )
                    .map(existing -> {
                        existing.updateAnalysis(
                                finalFitType,
                                "EMBEDDING",
                                finalConfidenceScore
                        );
                        return existing;
                    })
                    .orElseGet(() ->
                            RecipeConditionMap.create(
                                    recipe,
                                    condition,
                                    finalFitType,
                                    "EMBEDDING",
                                    finalConfidenceScore
                            )
                    );

            recipeConditionMapRepository.save(map);
        }
    }
    private boolean isLowSodiumCaution(
            ConditionCode condition,
            Recipe recipe,
            String ingredientText
    ) {
        if (!"LOW_SODIUM".equals(condition.getConditionCode())) {
            return false;
        }

        String text = (
                recipe.getTitle() + " " +
                recipe.getSummary() + " " +
                ingredientText
        ).toLowerCase();

        return containsAny(
                text,
                List.of(
                        "김치",
                        "장아찌",
                        "젓갈",
                        "간장",
                        "고추장",
                        "된장",
                        "쌈장",
                        "소스",
                        "양념",
                        "절임",
                        "피클",
                        "치킨무",
                        "떡볶이",
                        "어묵",
                        "오뎅",
                        "스팸",
                        "햄",
                        "소시지",
                        "소세지",
                        "베이컨",
                        "멸치",
                        "장조림"
                )
        );
    }

    private boolean isDietCaution(
            ConditionCode condition,
            Recipe recipe,
            String ingredientText
    ) {
        if (!"DIET_LOW_CALORIE".equals(condition.getConditionCode())) {
            return false;
        }

        String text = (
                recipe.getTitle() + " " +
                recipe.getSummary() + " " +
                ingredientText
        ).toLowerCase();

        return containsAny(
                text,
                List.of(
                        "베이컨",
                        "스팸",
                        "햄",
                        "소시지",
                        "소세지",
                        "버터",
                        "마요",
                        "튀김",
                        "치즈",
                        "크림",
                        "떡볶이",
                        "맛탕",
                        "강정"
                )
        );
    }
    private String resolveFitType(
            ConditionCode condition,
            double similarity
    ) {
        String code = condition.getConditionCode();

        if ("DIET_LOW_CALORIE".equals(code)
                || "LOW_SODIUM".equals(code)) {
            if (similarity >= RECOMMENDED_THRESHOLD) {
                return "RECOMMENDED";
            }
            if (similarity >= ALLOWED_THRESHOLD) {
                return "ALLOWED";
            }
            return null;
        }

        return null;
    }
    
    private double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) {
            throw new IllegalArgumentException("Embedding vector size mismatch");
        }

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.size(); i++) {
            double v1 = a.get(i);
            double v2 = b.get(i);

            dot += v1 * v2;
            normA += v1 * v1;
            normB += v2 * v2;
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    private List<Double> parseVector(String vectorText) {
        String cleaned = vectorText
                .replace("[", "")
                .replace("]", "")
                .trim();

        if (cleaned.isBlank()) {
            return List.of();
        }

        return java.util.Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .map(Double::parseDouble)
                .toList();
    }
}