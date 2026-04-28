package com.today.fridge.substitution.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.today.fridge.substitution.dto.SubstituteSuggestionDto;
import com.today.fridge.substitution.rule.SubstituteRules;

@Service
public class SubstituteIngredientService {

    public List<SubstituteSuggestionDto> suggest(
            List<String> missingIngredients,
            List<String> ownedIngredients,
            String recipeTitle
    ) {
        return missingIngredients.stream()
                .filter(SubstituteRules.RULES::containsKey)

                .flatMap(missing ->
                    SubstituteRules.RULES.get(missing).stream()

                        // 내가 가진 대체재만 통과
                        .filter(rule ->
                            ownedIngredients.contains(
                                rule.getSubstituteIngredient()
                            )
                        )

                        // missingIngredient 채워서 반환
                        .map(rule ->
                            SubstituteSuggestionDto.builder()
                                .missingIngredient(missing)
                                .substituteIngredient(
                                        rule.getSubstituteIngredient()
                                )
                                .decisionType(
                                        rule.getDecisionType()
                                )
                                .reason(
                                        rule.getReason()
                                )
                                .build()
                        )
                )
                .toList();
    }
}