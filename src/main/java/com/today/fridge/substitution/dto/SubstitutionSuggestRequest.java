package com.today.fridge.substitution.dto;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SubstitutionSuggestRequest {

    private Long recipeId;

    private List<String> ownedIngredients;
}