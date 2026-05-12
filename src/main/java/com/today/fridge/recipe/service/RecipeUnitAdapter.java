package com.today.fridge.recipe.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
public class RecipeUnitAdapter {

    /**
     * 레시피 요구량(g/ml)을 유저 냉장고의 단위(개수)로 변환합니다.
     */
    public BigDecimal convertGramsToQuantity(String ingredientName, BigDecimal grams) {
        log.info("[RecipeUnitAdapter] convertGramsToQuantity START - ingredientName: {}, grams: {}", ingredientName, grams);
        if (grams == null || grams.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("[RecipeUnitAdapter] convertGramsToQuantity END");
            return BigDecimal.ZERO;
        }

        BigDecimal weightPerItem = RecipeIngredientWeights.getWeight(ingredientName);
        if (weightPerItem != null && weightPerItem.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal result = grams.divide(weightPerItem, 2, RoundingMode.HALF_UP);
            log.info("[RecipeUnitAdapter] convertGramsToQuantity END");
            return result;
        }

        log.info("[RecipeUnitAdapter] convertGramsToQuantity END");
        return grams;
    }

    /**
     * 유저 냉장고의 개수를 계산용 Base Unit(g/ml)으로 변환합니다.
     */
    public BigDecimal convertQuantityToGrams(String ingredientName, BigDecimal quantity) {
        log.info("[RecipeUnitAdapter] convertQuantityToGrams START - ingredientName: {}, quantity: {}", ingredientName, quantity);
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("[RecipeUnitAdapter] convertQuantityToGrams END");
            return BigDecimal.ZERO;
        }
        
        BigDecimal weightPerItem = RecipeIngredientWeights.getWeight(ingredientName);
        if (weightPerItem != null) {
            BigDecimal result = quantity.multiply(weightPerItem);
            log.info("[RecipeUnitAdapter] convertQuantityToGrams END");
            return result;
        }

        log.info("[RecipeUnitAdapter] convertQuantityToGrams END");
        return quantity;
    }
}
