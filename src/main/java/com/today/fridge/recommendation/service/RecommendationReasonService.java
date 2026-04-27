package com.today.fridge.recommendation.service;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class RecommendationReasonService {

    public String buildReason(
            double matchRate,
            List<String> conditionTags,
            List<String> missingIngredients
    ) {
        boolean hasCondition = conditionTags != null && !conditionTags.isEmpty();
        boolean hasMissing = missingIngredients != null && !missingIngredients.isEmpty();

        if (matchRate >= 90 && hasCondition && !hasMissing) {
            return "보유 재료가 충분히 일치하고 사용자 조건에도 잘 맞는 추천입니다.";
        }

        if (matchRate >= 70 && hasCondition) {
            return "보유 재료와 사용자 조건을 함께 고려한 추천입니다.";
        }

        if (hasMissing) {
            return "일부 부족한 재료가 있어 대체 재료 확인이 필요한 추천입니다.";
        }

        return "입력한 조건을 기준으로 추천된 레시피입니다.";
    }
}