package com.today.fridge.meal.dto.response;

import com.today.fridge.global.external.fastapi.RecipeBrief;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AIRecommendationResponse {
    private String report;
    private Map<String, List<RecipeBrief>> recommendations;
}
