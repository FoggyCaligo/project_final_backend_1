package com.today.fridge.global.external.fastapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FastApiMealEnvelope {
    private Boolean success;
    private String code;
    private String message;
    private MealRecommendationResponse data;
    private String requestId;
}
