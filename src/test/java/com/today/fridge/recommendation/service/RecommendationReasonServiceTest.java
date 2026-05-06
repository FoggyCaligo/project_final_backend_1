package com.today.fridge.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecommendationReasonServiceTest {

    private final RecommendationReasonService service =
            new RecommendationReasonService();

    @Test
    @DisplayName("재료 일치율 높고 조건 만족, 부족 재료 없음")
    void highMatch_withCondition_noMissing() {
        String result = service.buildReason(
                95.0,
                List.of("LOW_SODIUM"),
                List.of()
        );

        assertThat(result)
                .isEqualTo("보유 재료가 충분히 일치하고 사용자 조건에도 잘 맞는 추천입니다.");
    }

    @Test
    @DisplayName("재료 일치율 중간 + 조건 있음")
    void midMatch_withCondition() {
        String result = service.buildReason(
                75.0,
                List.of("DIET"),
                List.of()
        );

        assertThat(result)
                .isEqualTo("보유 재료와 사용자 조건을 함께 고려한 추천입니다.");
    }

    @Test
    @DisplayName("부족 재료가 있으면 해당 메시지 반환")
    void hasMissingIngredients() {
        String result = service.buildReason(
                80.0,
                List.of(),
                List.of("우유")
        );

        assertThat(result)
                .isEqualTo("일부 부족한 재료가 있어 대체 재료 확인이 필요한 추천입니다.");
    }

    @Test
    @DisplayName("기본 케이스")
    void defaultCase() {
        String result = service.buildReason(
                50.0,
                List.of(),
                List.of()
        );

        assertThat(result)
                .isEqualTo("입력한 조건을 기준으로 추천된 레시피입니다.");
    }
}