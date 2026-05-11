package com.today.fridge.llm.dto.request;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubstitutionLlmRequest {

    private String recipeTitle;

    private List<String> recipeIngredients;

    private List<String> ownedIngredients;

    private List<String> missingIngredients;
}