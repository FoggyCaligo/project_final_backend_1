package com.today.fridge.substitution.dto;

import com.today.fridge.substitution.type.SubstitutionDecisionType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubstitutionResultDto {

    private String missingIngredient;

    private SubstitutionDecisionType decisionType;

    private String substituteIngredient;

    private String reason;
}