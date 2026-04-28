package com.today.fridge.substitution.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubstituteSuggestionDto {

    private String missingIngredient;
    private String substituteIngredient;
    private String reason;
    private String decisionType; // AVAILABLE, NOT_AVAILABLE, CAUTION
}