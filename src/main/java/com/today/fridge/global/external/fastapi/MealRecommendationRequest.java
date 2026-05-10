package com.today.fridge.global.external.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MealRecommendationRequest(
    @JsonProperty("user_id") String userId,
    Double height,
    Double weight,
    Integer age,
    String gender
) {
}
