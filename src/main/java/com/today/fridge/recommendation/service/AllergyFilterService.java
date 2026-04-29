package com.today.fridge.recommendation.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.today.fridge.recommendation.repository.AllergenIngredientMapRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AllergyFilterService {

    private final AllergenIngredientMapRepository allergenIngredientMapRepository;

    public boolean containsAllergen(
            List<String> recipeIngredients,
            List<String> userAllergenCodes
    ) {
        if (recipeIngredients == null || recipeIngredients.isEmpty()) {
            return false;
        }

        if (userAllergenCodes == null || userAllergenCodes.isEmpty()) {
            return false;
        }

        Set<String> allergenSet = new HashSet<>(
                allergenIngredientMapRepository
                        .findIngredientNamesByAllergenCodes(userAllergenCodes)
        );

        if (allergenSet.isEmpty()) {
            return false;
        }

        return recipeIngredients.stream()
                .filter(ingredient -> ingredient != null && !ingredient.isBlank())
                .map(String::trim)
                .anyMatch(allergenSet::contains);
    }
}