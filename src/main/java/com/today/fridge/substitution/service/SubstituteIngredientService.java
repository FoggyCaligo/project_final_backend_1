package com.today.fridge.substitution.service;

import java.util.List;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.today.fridge.substitution.dto.SubstituteSuggestionDto;
@Service
public class SubstituteIngredientService {

    public List<SubstituteSuggestionDto> suggest(
            List<String> missingIngredients,
            List<String> ownedIngredients,
            String recipeTitle
    ) {
        // TODO: 추후 FastAPI/LLM 연동
        return List.of();
    }
}