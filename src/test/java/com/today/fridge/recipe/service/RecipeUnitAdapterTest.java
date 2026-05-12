package com.today.fridge.recipe.service;

/*
 * UT-04
 * Method: convertGramsToQuantity
 * Test Name: 그램을 개수로 변환 - 직접 매칭
 * Purpose: 사전(Dictionary)에 등록된 재료명을 기준으로 그램(g) 단위를 개수 단위로 정확히 변환한다.
 * Input: ingredientName="양파", grams=400 (평균 200g)
 * Expected Result: 2.00 반환
 * Priority: High
 *
 * UT-05
 * Method: convertQuantityToGrams
 * Test Name: 개수를 그램으로 변환 - 직접 매칭
 * Purpose: 사전(Dictionary)에 등록된 재료명을 기준으로 개수 단위를 그램(g) 단위로 정확히 변환한다.
 * Input: ingredientName="사과", quantity=2 (평균 250g)
 * Expected Result: 500 반환
 * Priority: High
 *
 * UT-06
 * Method: convertGramsToQuantity
 * Test Name: 키워드 기반 유추 변환
 * Purpose: 사전에 없는 이름이라도 키워드(예: "양파")가 포함되어 있으면 기본 가중치를 적용한다.
 * Input: ingredientName="빨간양파", grams=200
 * Expected Result: 1.00 반환 (양파 가중치 200g 적용)
 * Priority: Medium
 *
 * UT-07
 * Method: convertGramsToQuantity
 * Test Name: 데이터 미존재 시 원본 반환
 * Purpose: 사전에 없고 유추도 불가능한 재료의 경우 입력된 수량을 그대로 반환한다.
 * Input: ingredientName="희귀한재료", grams=100
 * Expected Result: 100 반환
 * Priority: Low
 */

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class RecipeUnitAdapterTest {

    private final RecipeUnitAdapter adapter = new RecipeUnitAdapter();

    @Test
    @DisplayName("UT-04 - 그램을 개수로 변환 (직접 매칭)")
    void testConvertGramsToQuantity_Direct() {
        // Given: 양파 가중치는 200g
        String name = "양파";
        BigDecimal grams = new BigDecimal("400");

        // When
        BigDecimal quantity = adapter.convertGramsToQuantity(name, grams);

        // Then: 400 / 200 = 2.00
        assertThat(quantity).isEqualByComparingTo(new BigDecimal("2.00"));
    }

    @Test
    @DisplayName("UT-05 - 개수를 그램으로 변환 (직접 매칭)")
    void testConvertQuantityToGrams_Direct() {
        // Given: 사과 가중치는 250g
        String name = "사과";
        BigDecimal quantity = new BigDecimal("2");

        // When
        BigDecimal grams = adapter.convertQuantityToGrams(name, quantity);

        // Then: 2 * 250 = 500
        assertThat(grams).isEqualByComparingTo(new BigDecimal("500"));
    }

    @Test
    @DisplayName("UT-06 - 키워드 기반 유추 변환")
    void testInferenceWeight() {
        // Given: "빨간양파"는 없지만 "양파"가 포함됨 (200g 적용)
        String name = "빨간양파";
        BigDecimal grams = new BigDecimal("200");

        // When
        BigDecimal quantity = adapter.convertGramsToQuantity(name, grams);

        // Then: 200 / 200 = 1.00
        assertThat(quantity).isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    @DisplayName("UT-07 - 데이터 미존재 시 원본 반환")
    void testNoMatchReturnsOriginal() {
        // Given
        String name = "희귀한재료";
        BigDecimal grams = new BigDecimal("100");

        // When
        BigDecimal quantity = adapter.convertGramsToQuantity(name, grams);

        // Then: 매칭 실패 시 원본 수량 반환
        assertThat(quantity).isEqualByComparingTo(new BigDecimal("100"));
    }
}
