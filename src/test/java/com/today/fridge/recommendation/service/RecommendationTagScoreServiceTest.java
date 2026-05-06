package com.today.fridge.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.today.fridge.recipe.entity.RecipeTag;
import com.today.fridge.recipe.entity.RecipeTagType;

class RecommendationTagScoreServiceTest {

    private final RecommendationTagScoreService service =
            new RecommendationTagScoreService();

    @Test
    @DisplayName("국 요리 쿼리와 SOUP 태그가 매칭되면 20점을 반환한다")
    void calculateTagScore_soupMatch() {
        double score = service.calculateTagScore(
                "국 요리 추천해줘",
                List.of(tag(RecipeTagType.COOKING_TYPE, "SOUP"))
        );

        assertThat(score).isEqualTo(20.0);
    }

    @Test
    @DisplayName("볶음 요리 쿼리와 STIR_FRY 태그가 매칭되면 20점을 반환한다")
    void calculateTagScore_stirFryMatch() {
        double score = service.calculateTagScore(
                "볶음 요리 추천해줘",
                List.of(tag(RecipeTagType.COOKING_TYPE, "STIR_FRY"))
        );

        assertThat(score).isEqualTo(20.0);
    }

    @Test
    @DisplayName("매운 음식 쿼리와 SPICY 태그가 매칭되면 15점을 반환한다")
    void calculateTagScore_spicyMatch() {
        double score = service.calculateTagScore(
                "매운 음식 추천해줘",
                List.of(tag(RecipeTagType.STYLE, "SPICY"))
        );

        assertThat(score).isEqualTo(15.0);
    }

    @Test
    @DisplayName("가벼운 음식 쿼리와 LIGHT 태그가 매칭되면 15점을 반환한다")
    void calculateTagScore_lightMatch() {
        double score = service.calculateTagScore(
                "가벼운 음식 추천해줘",
                List.of(tag(RecipeTagType.STYLE, "LIGHT"))
        );

        assertThat(score).isEqualTo(15.0);
    }

    @Test
    @DisplayName("조리유형과 스타일이 모두 매칭되면 점수를 합산한다")
    void calculateTagScore_cookingTypeAndStyleMatch() {
        double score = service.calculateTagScore(
                "매운 볶음 요리 추천해줘",
                List.of(
                        tag(RecipeTagType.COOKING_TYPE, "STIR_FRY"),
                        tag(RecipeTagType.STYLE, "SPICY")
                )
        );

        assertThat(score).isEqualTo(35.0);
    }

    @Test
    @DisplayName("쿼리에 해당하는 태그가 없으면 0점을 반환한다")
    void calculateTagScore_noMatchedTag() {
        double score = service.calculateTagScore(
                "국 요리 추천해줘",
                List.of(tag(RecipeTagType.COOKING_TYPE, "STIR_FRY"))
        );

        assertThat(score).isEqualTo(0.0);
    }

    @Test
    @DisplayName("쿼리가 비어 있으면 0점을 반환한다")
    void calculateTagScore_blankQuery() {
        double score = service.calculateTagScore(
                " ",
                List.of(tag(RecipeTagType.COOKING_TYPE, "SOUP"))
        );

        assertThat(score).isEqualTo(0.0);
    }

    @Test
    @DisplayName("태그 목록이 비어 있으면 0점을 반환한다")
    void calculateTagScore_emptyTags() {
        double score = service.calculateTagScore(
                "국 요리 추천해줘",
                List.of()
        );

        assertThat(score).isEqualTo(0.0);
    }

    private RecipeTag tag(RecipeTagType tagType, String tagCode) {
        return RecipeTag.builder()
                .tagType(tagType)
                .tagCode(tagCode)
                .build();
    }
}