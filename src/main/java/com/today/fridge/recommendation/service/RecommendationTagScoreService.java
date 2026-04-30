package com.today.fridge.recommendation.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.today.fridge.recipe.entity.RecipeTag;
import com.today.fridge.recipe.entity.RecipeTagType;

@Service
public class RecommendationTagScoreService {

    private static final double COOKING_TYPE_MATCH_SCORE = 80.0;
    private static final double STYLE_MATCH_SCORE = 60.0;

    private static final Map<String, String> COOKING_TYPE_KEYWORD_MAP = Map.ofEntries(
            Map.entry("국", "SOUP"),
            Map.entry("탕", "SOUP"),
            Map.entry("찌개", "STEW"),
            Map.entry("볶음", "STIR_FRY"),
            Map.entry("조림", "BRAISED"),
            Map.entry("무침", "SEASONED"),
            Map.entry("전", "PANCAKE")
    );

    private static final Map<String, String> STYLE_KEYWORD_MAP = Map.ofEntries(
            Map.entry("매운", "SPICY"),
            Map.entry("맵", "SPICY"),
            Map.entry("얼큰", "SPICY"),
            Map.entry("담백", "LIGHT"),
            Map.entry("가벼운", "LIGHT"),
            Map.entry("시원", "REFRESHING"),
            Map.entry("상큼", "REFRESHING"),
            Map.entry("감칠맛", "SAVORY"),
            Map.entry("짭짤", "SAVORY"),
            Map.entry("달달", "SWEET"),
            Map.entry("단맛", "SWEET")
    );

    public double calculateTagScore(String query, List<RecipeTag> recipeTags) {
        if (query == null || query.isBlank()) {
            return 0.0;
        }

        if (recipeTags == null || recipeTags.isEmpty()) {
            return 0.0;
        }

        double score = 0.0;

        score += calculateCookingTypeScore(query, recipeTags);
        score += calculateStyleScore(query, recipeTags);

        return score;
    }

    private double calculateCookingTypeScore(String query, List<RecipeTag> recipeTags) {
        double score = 0.0;

        for (Map.Entry<String, String> entry : COOKING_TYPE_KEYWORD_MAP.entrySet()) {
            String keyword = entry.getKey();
            String expectedTagCode = entry.getValue();

            if (!query.contains(keyword)) {
                continue;
            }

            boolean matched = recipeTags.stream()
                    .anyMatch(tag ->
                            tag.getTagType() == RecipeTagType.COOKING_TYPE
                                    && expectedTagCode.equals(tag.getTagCode())
                    );

            if (matched) {
                score += COOKING_TYPE_MATCH_SCORE;
            }
        }

        return score;
    }

    private double calculateStyleScore(String query, List<RecipeTag> recipeTags) {
        double score = 0.0;

        for (Map.Entry<String, String> entry : STYLE_KEYWORD_MAP.entrySet()) {
            String keyword = entry.getKey();
            String expectedTagCode = entry.getValue();

            if (!query.contains(keyword)) {
                continue;
            }

            boolean matched = recipeTags.stream()
                    .anyMatch(tag ->
                            tag.getTagType() == RecipeTagType.STYLE
                                    && expectedTagCode.equals(tag.getTagCode())
                    );

            if (matched) {
                score += STYLE_MATCH_SCORE;
            }
        }

        return score;
    }
}