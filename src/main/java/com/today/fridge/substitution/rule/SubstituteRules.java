package com.today.fridge.substitution.rule;

import java.util.List;
import java.util.Map;

import com.today.fridge.substitution.dto.SubstituteSuggestionDto;

public final class SubstituteRules {

    private SubstituteRules(){}

    public static final Map<String, List<SubstituteSuggestionDto>> RULES =
            Map.of(
              "휘핑크림",
              List.of(
                SubstituteSuggestionDto.builder()
                    .substituteIngredient("생크림")
                    .decisionType("AVAILABLE")
                    .reason("비슷한 농도로 대체 가능합니다.")
                    .build(),

                SubstituteSuggestionDto.builder()
                    .substituteIngredient("우유")
                    .decisionType("CAUTION")
                    .reason("맛과 농도가 달라질 수 있습니다.")
                    .build()
              ),
              "소금",
              List.of(
                  SubstituteSuggestionDto.builder()
                      .substituteIngredient("굵은소금")
                      .decisionType("CAUTION")
                      .reason("대체는 가능하지만 입자가 커서 간이 고르게 배지 않을 수 있습니다.")
                      .build()
              )
            );
}