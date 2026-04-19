package com.todayfridge.backend1.domain.recipe.service;

import com.todayfridge.backend1.domain.ingredient.repository.UserIngredientRepository;
import com.todayfridge.backend1.domain.recipe.repository.RecipeIngredientRepository;
import com.todayfridge.backend1.domain.recipe.repository.RecipeRepository;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RecommendationService {
    private final UserIngredientRepository userIngredientRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecommendationScoreService recommendationScoreService;

    public RecommendationService(UserIngredientRepository userIngredientRepository, RecipeRepository recipeRepository,
                                 RecipeIngredientRepository recipeIngredientRepository, RecommendationScoreService recommendationScoreService) {
        this.userIngredientRepository = userIngredientRepository;
        this.recipeRepository = recipeRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.recommendationScoreService = recommendationScoreService;
    }

    public List<Map<String, Object>> recommend(Long userId) {
        Set<String> fridgeNames = userIngredientRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(item -> item.getNormalizedName() == null || item.getNormalizedName().isBlank()
                        ? item.getIngredientName() : item.getNormalizedName())
                .collect(Collectors.toSet());

        List<Map<String, Object>> results = new ArrayList<>();
        recipeRepository.findTop50ByOrderByIdDesc().forEach(recipe -> {
            LinkedHashMap<String, Object> score = recommendationScoreService.score(
                    fridgeNames, recipeIngredientRepository.findByRecipe_Id(recipe.getId()));
            LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            item.put("recipeId", recipe.getId());
            item.put("title", recipe.getTitle());
            item.put("thumbnailUrl", recipe.getThumbnailUrl());
            item.putAll(score);
            results.add(item);
        });

        return results.stream()
                .sorted(Comparator.comparing((Map<String, Object> item) -> ((Number) item.get("matchRate")).doubleValue()).reversed())
                .toList();
    }

    public long countRecommendable(Long userId) {
        return recommend(userId).stream()
                .filter(item -> ((Number) item.get("matchRate")).doubleValue() > 0.0)
                .count();
    }
}
