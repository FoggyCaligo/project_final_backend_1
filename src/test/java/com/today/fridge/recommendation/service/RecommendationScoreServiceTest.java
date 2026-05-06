package com.today.fridge.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
        assertThat(score).isEqualTo(75.0);
    }

    @Test
    @DisplayName("필수 재료 수가 0이면 재료 점수는 0점이다")
    void calculateIngredientScore_requiredCountZero() {
        // when
        double score =
                recommendationScoreService.calculateIngredientScore(0, 0);

        // then
        assertThat(score).isEqualTo(0.0);
    }

    @Test
    @DisplayName("누락 재료가 많아 점수가 음수가 되면 최소 3점을 반환한다")
    void calculateIngredientScore_negativeScoreReturnsMinimumScore() {
        // when
        double score =
                recommendationScoreService.calculateIngredientScore(0, 10);

        // then
        assertThat(score).isEqualTo(3.0);
    }

    @Test
    @DisplayName("재료 일치율을 계산한다")
    void calculateMatchRate() {
        // when
        double matchRate =
                recommendationScoreService.calculateMatchRate(4, 5);

        // then
        assertThat(matchRate).isEqualTo(80.0);
    }

    @Test
    @DisplayName("필수 재료 수가 0이면 재료 일치율은 0이다")
    void calculateMatchRate_requiredCountZero() {
        // when
        double matchRate =
                recommendationScoreService.calculateMatchRate(0, 0);

        // then
        assertThat(matchRate).isEqualTo(0.0);
    }

    @Test
    @DisplayName("총점을 계산한다")
    void calculateTotalScore() {
        // when
        double totalScore =
                recommendationScoreService.calculateTotalScore(
                        56.0,
                        20.0
                );

        // then
        assertThat(totalScore).isCloseTo(50.2, within(0.001));
    }

    @Test
    @DisplayName("조건 점수가 0이면 조건 보너스를 더하지 않는다")
    void calculateTotalScore_withoutConditionBonus() {
        // when
        double totalScore =
                recommendationScoreService.calculateTotalScore(
                        70.0,
                        0.0
                );

        // then
        assertThat(totalScore).isEqualTo(49.0);
    }
}