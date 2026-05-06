package com.today.fridge.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;

import com.today.fridge.recommendation.repository.AllergenIngredientMapRepository;

@ExtendWith(MockitoExtension.class)
class AllergyFilterServiceTest {

    @Mock
    private AllergenIngredientMapRepository allergenIngredientMapRepository;

    @InjectMocks
    private AllergyFilterService allergyFilterService;

    @Test
    @DisplayName("레시피 재료에 알러지 매핑 재료가 포함되면 true를 반환한다")
    void containsAllergen_returnsTrueWhenRecipeContainsAllergenIngredient() {
        when(allergenIngredientMapRepository.findIngredientNamesByAllergenCodes(List.of("ALLERGY_MILK")))
                .thenReturn(List.of("우유", "치즈", "버터"));

        boolean result = allergyFilterService.containsAllergen(
                List.of("두부", "우유", "대파"),
                List.of("ALLERGY_MILK")
        );

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("레시피 재료에 알러지 매핑 재료가 없으면 false를 반환한다")
    void containsAllergen_returnsFalseWhenNoAllergenIngredient() {
        when(allergenIngredientMapRepository.findIngredientNamesByAllergenCodes(List.of("ALLERGY_MILK")))
                .thenReturn(List.of("우유", "치즈", "버터"));

        boolean result = allergyFilterService.containsAllergen(
                List.of("두부", "대파", "양파"),
                List.of("ALLERGY_MILK")
        );

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("레시피 재료가 비어 있으면 false를 반환하고 Repository를 호출하지 않는다")
    void containsAllergen_returnsFalseWhenRecipeIngredientsEmpty() {
        boolean result = allergyFilterService.containsAllergen(
                List.of(),
                List.of("ALLERGY_MILK")
        );

        assertThat(result).isFalse();
        verifyNoInteractions(allergenIngredientMapRepository);
    }

    @Test
    @DisplayName("사용자 알러지 코드가 비어 있으면 false를 반환하고 Repository를 호출하지 않는다")
    void containsAllergen_returnsFalseWhenUserAllergenCodesEmpty() {
        boolean result = allergyFilterService.containsAllergen(
                List.of("우유", "치즈"),
                List.of()
        );

        assertThat(result).isFalse();
        verifyNoInteractions(allergenIngredientMapRepository);
    }

    @Test
    @DisplayName("알러지 코드에 매핑된 재료가 없으면 false를 반환한다")
    void containsAllergen_returnsFalseWhenMappedIngredientsEmpty() {
        when(allergenIngredientMapRepository.findIngredientNamesByAllergenCodes(List.of("ALLERGY_UNKNOWN")))
                .thenReturn(List.of());

        boolean result = allergyFilterService.containsAllergen(
                List.of("우유", "치즈"),
                List.of("ALLERGY_UNKNOWN")
        );

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("레시피 재료의 공백은 trim 후 비교한다")
    void containsAllergen_trimIngredientName() {
        when(allergenIngredientMapRepository.findIngredientNamesByAllergenCodes(List.of("ALLERGY_EGG")))
                .thenReturn(List.of("계란", "달걀"));

        boolean result = allergyFilterService.containsAllergen(
                List.of(" 계란 ", "대파"),
                List.of("ALLERGY_EGG")
        );

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("레시피 재료에 null 또는 blank가 있어도 무시하고 검사한다")
    void containsAllergen_ignoreNullAndBlankIngredients() {
        when(allergenIngredientMapRepository.findIngredientNamesByAllergenCodes(List.of("ALLERGY_EGG")))
                .thenReturn(List.of("계란", "달걀"));

        boolean result = allergyFilterService.containsAllergen(
                Arrays.asList(null, " ", "두부", "달걀"),
                List.of("ALLERGY_EGG")
        );

        assertThat(result).isTrue();
    }
}