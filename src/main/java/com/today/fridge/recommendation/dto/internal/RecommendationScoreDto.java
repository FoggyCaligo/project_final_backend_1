package com.today.fridge.recommendation.dto.internal;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RecommendationScoreDto {

    private double ingredientScore;
    private double conditionScore;
    private double totalScore;
}
