package com.today.fridge.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HybridRankingServiceTest {

    private final HybridRankingService service = new HybridRankingService();

    @Test
    @DisplayName("semantic distance를 score로 변환한다")
    void toSemanticScore() {
        double score = service.toSemanticScore(0.5);

        assertThat(score).isEqualTo(1.0 / 1.5);
    }

    @Test
    @DisplayName("distance가 0이면 semanticScore는 1이다")
    void toSemanticScore_zeroDistance() {
        double score = service.toSemanticScore(0.0);

        assertThat(score).isEqualTo(1.0);
    }

    @Test
    @DisplayName("hybridScore를 계산한다")
    void calculateHybridScore() {
        double result = service.calculateHybridScore(
                20.0,   // ruleScore
                0.8,    // semanticScore
                10.0    // tagScore
        );

        double expected =
                (20.0 * 0.35)
              + (0.8 * 100.0 * 0.20)
              + (10.0 * 0.45);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("semanticScore가 높을수록 hybridScore가 증가한다")
    void semanticScoreAffectsHybridScore() {
        double low = service.calculateHybridScore(20.0, 0.2, 10.0);
        double high = service.calculateHybridScore(20.0, 0.9, 10.0);

        assertThat(high).isGreaterThan(low);
    }
}