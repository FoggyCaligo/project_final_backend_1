package com.today.fridge.recommendation.dto.internal;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecommendationQuery {

    private Long userId;

    private List<String> conditionCodes;

    private List<String> includeIngredients;

    private List<String> excludeIngredients;

    private List<String> keywords;

    private String sortHint;

    private String source; // HOME, SEARCH, CHATBOT

    private boolean useUserProfile;

    private boolean useUserFridge;
}