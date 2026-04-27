package com.today.fridge.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecommendationScoreServiceTest {

    private final RecommendationScoreService recommendationScoreService =
            new RecommendationScoreService();

    @Test
    @DisplayName("재료 매칭 점수를 계산한다")
    void calculateIngredientScore() {

        // given
        int matchedCount = 4;
        int requiredCount = 5;

        // when
        double score =
                recommendationScoreService.calculateIngredientScore(
                        matchedCount,
                        requiredCount
                );

        // then
        assertThat(score).isEqualTo(56.0);
    }

    @Test
    @DisplayName("재료 일치율을 계산한다")
    void calculateMatchRate() {

        double matchRate =
                recommendationScoreService.calculateMatchRate(4,5);

        assertThat(matchRate).isEqualTo(80.0);
    }

    @Test
    @DisplayName("총점을 계산한다")
    void calculateTotalScore() {

        double totalScore =
                recommendationScoreService.calculateTotalScore(
                        56.0,
                        20.0
                );

        assertThat(totalScore).isEqualTo(76.0);
    }

    @Test
    @DisplayName("필수 재료 수가 0이면 0점 반환")
    void zeroRequiredIngredientReturnsZero() {

        double score =
                recommendationScoreService.calculateIngredientScore(
                        0,
                        0
                );

        assertThat(score).isEqualTo(0.0);
    }
}