package com.today.fridge.llm.dto.response;

import com.today.fridge.substitution.type.SubstitutionDecisionType;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SubstitutionLlmResult {

    private String missingIngredient;

    private SubstitutionDecisionType decisionType;

    private String substituteIngredient;

    private String reason;
}