package com.today.fridge.meal.service;

/*
 * UT-MEAL-1
 * Method: updateDayNutrition
 * Test Name: 일일 영양 섭취량 누적 업데이트 테스트
 * Purpose: 식단 기록 시 사용자의 일일 영양 섭취량을 계산하여 누적 업데이트한다.
 * Input: user, recipe, servings, consumedAt
 * Expected Result: 계산된 영양 섭취량이 기존 DayNutrition에 누적 반영되어 저장된다.
 * Priority: High
 *
 * UT-MEAL-2
 * Method: calculateDetailedTargets
 * Test Name: 상세 영양 목표치 산출 테스트
 * Purpose: 사용자의 신체 정보를 기반으로 TDEE 및 영양 목표치를 계산한다.
 * Input: heightCm, weightKg, age, gender
 * Expected Result: 각 영양소별 목표치가 포함된 NutritionTarget 객체가 반환된다.
 * Priority: High
 */

import com.today.fridge.meal.entity.DayNutrition;
import com.today.fridge.meal.repository.DayNutritionRepository;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeNutrition;
import com.today.fridge.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("식단 서비스 헬퍼 메서드 단위 테스트")
public class MealService1HelperTest {

    @InjectMocks
    private MealServiceHelperMethods helperMethods;

    @Mock
    private DayNutritionRepository dayNutritionRepository;

    @Test
    @DisplayName("일일 영양 섭취량 누적 업데이트 테스트")
    void testUpdateDayNutrition() {
        User user = User.create("helpertest", "helper@todayfridge.com", "hashedpassword", "헬퍼테스터");
        ReflectionTestUtils.setField(user, "userId", 1L);

        RecipeNutrition rn = RecipeNutrition.builder()
                .calories(new BigDecimal("300"))
                .carbs(new BigDecimal("30"))
                .protein(new BigDecimal("20"))
                .fat(new BigDecimal("10"))
                .sugar(new BigDecimal("5"))
                .sodium(new BigDecimal("500"))
                .cholesterol(new BigDecimal("50"))
                .build();

        Recipe recipe = Recipe.builder()
                .recipeId(1L)
                .servingsText("1인분")
                .recipeNutrition(rn)
                .build();

        LocalDateTime consumedAt = LocalDateTime.of(2023, 10, 1, 12, 0);
        BigDecimal servings = new BigDecimal("2"); 

        DayNutrition existingDn = new DayNutrition();
        existingDn.setUser(user);
        existingDn.setDate(consumedAt.toLocalDate().atStartOfDay());
        existingDn.setTotalCalories(BigDecimal.ZERO);
        existingDn.setTotalCarbs(BigDecimal.ZERO);
        existingDn.setTotalProtein(BigDecimal.ZERO);
        existingDn.setTotalFat(BigDecimal.ZERO);
        existingDn.setTotalSugar(BigDecimal.ZERO);
        existingDn.setTotalSodium(BigDecimal.ZERO);
        existingDn.setTotalCholesterol(BigDecimal.ZERO);

        given(dayNutritionRepository.findByUserUserIdAndDate(anyLong(), any(LocalDateTime.class)))
                .willReturn(Optional.of(existingDn));

        helperMethods.updateDayNutrition(user, recipe, servings, consumedAt);

        then(dayNutritionRepository).should(times(1)).save(existingDn);
        assertThat(existingDn.getTotalCalories()).isEqualByComparingTo("600"); 
        assertThat(existingDn.getTotalCarbs()).isEqualByComparingTo("60");
        assertThat(existingDn.getTotalProtein()).isEqualByComparingTo("40");
        assertThat(existingDn.getTotalFat()).isEqualByComparingTo("20");
        assertThat(existingDn.getTotalSugar()).isEqualByComparingTo("10");
        assertThat(existingDn.getTotalSodium()).isEqualByComparingTo("1000");
        assertThat(existingDn.getTotalCholesterol()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("상세 영양 목표치 산출 테스트")
    void testCalculateDetailedTargets() {
        double heightCm = 175.0;
        double weightKg = 70.0;
        int age = 30;
        String gender = "MALE";

        MealServiceHelperMethods.NutritionTarget target = helperMethods.calculateDetailedTargets(heightCm, weightKg,
                age, gender);

        assertThat(target).isNotNull();
        assertThat(target.getCalories()).isEqualByComparingTo("1978.5");
        assertThat(target.getCarbs()).isEqualByComparingTo("247.3");
        assertThat(target.getProtein()).isEqualByComparingTo("98.9");
        assertThat(target.getFat()).isBetween(new BigDecimal("65.9"), new BigDecimal("66.0"));
    }
}
