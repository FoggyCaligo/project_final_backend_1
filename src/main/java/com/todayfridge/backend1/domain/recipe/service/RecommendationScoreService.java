package com.todayfridge.backend1.domain.recipe.service;

import com.todayfridge.backend1.domain.recipe.entity.RecipeIngredient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RecommendationScoreService {
    public LinkedHashMap<String, Object> score(Set<String> fridgeNormalizedNames, List<RecipeIngredient> recipeIngredients) {
        Set<String> recipeNames = recipeIngredients.stream()
                .map(RecipeIngredient::getNormalizedName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toSet());
        List<String> matched = recipeNames.stream().filter(fridgeNormalizedNames::contains).sorted().toList();
        List<String> missing = recipeNames.stream().filter(name -> !fridgeNormalizedNames.contains(name)).sorted().toList();
        double matchRate = recipeNames.isEmpty() ? 0.0 : (double) matched.size() / recipeNames.size();

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("matchRate", Math.round(matchRate * 1000) / 1000.0);
        result.put("matchedIngredients", matched);
        result.put("missingIngredients", missing);
        result.put("reason", matched.isEmpty() ? "현재 보유 재료와 일치하는 핵심 재료가 없습니다." : "보유 재료와 겹치는 재료가 있어 추천합니다.");
        return result;
    }
}
