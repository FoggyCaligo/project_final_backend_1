package com.today.fridge.substitution.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubstitutionSuggestResponse {

    private Long recipeId;

    private String recipeTitle;

    private List<String> ownedIngredients;

    private List<String> missingIngredients;

    private List<SubstitutionResultDto> results;
}