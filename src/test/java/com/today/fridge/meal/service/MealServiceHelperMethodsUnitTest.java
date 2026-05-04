package com.today.fridge.meal.service;

import com.today.fridge.meal.dto.response.MealNutritionSummaryDTO;
import com.today.fridge.meal.dto.response.RemainingNutritionResponse;
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

/**
 * MealServiceHelperMethods 컴포넌트에 대한 단위 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MealServiceHelperMethods 단위 테스트")
public class MealServiceHelperMethodsUnitTest {

    @InjectMocks
    private MealServiceHelperMethods helperMethods;

    @Mock
    private DayNutritionRepository dayNutritionRepository;

    // ============================================================================================
    // updateDayNutrition 단위 테스트
    // ============================================================================================
    @Test
    @DisplayName("일일 영양 섭취량 누적 업데이트 테스트")
    void testUpdateDayNutrition() {
        // given (1. 유저, 레시피, 영양 정보 준비)
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
        BigDecimal servings = new BigDecimal("2"); // 2인분 섭취 가정

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

        // when (2. 누적 업데이트 로직 실행)
        helperMethods.updateDayNutrition(user, recipe, servings, consumedAt);

        // then (3. 검증)
        then(dayNutritionRepository).should(times(1)).save(existingDn);
        assertThat(existingDn.getTotalCalories()).isEqualByComparingTo("600"); // 300 * 2
        assertThat(existingDn.getTotalCarbs()).isEqualByComparingTo("60");
        assertThat(existingDn.getTotalProtein()).isEqualByComparingTo("40");
        assertThat(existingDn.getTotalFat()).isEqualByComparingTo("20");
        assertThat(existingDn.getTotalSugar()).isEqualByComparingTo("10");
        assertThat(existingDn.getTotalSodium()).isEqualByComparingTo("1000");
        assertThat(existingDn.getTotalCholesterol()).isEqualByComparingTo("100");
    }

    // ============================================================================================
    // calculateDetailedTargets 단위 테스트
    // ============================================================================================
    @Test
    @DisplayName("상세 영양 목표치 산출 테스트")
    void testCalculateDetailedTargets() {
        // given (1. 신체 정보 준비)
        double heightCm = 175.0;
        double weightKg = 70.0;
        int age = 30;
        String gender = "MALE";

        // when (2. 목표치 계산 실행)
        MealServiceHelperMethods.NutritionTarget target = helperMethods.calculateDetailedTargets(heightCm, weightKg,
                age, gender);

        // then (3. 계산 결과 검증)
        assertThat(target).isNotNull();
        // BMR = (10 * 70) + (6.25 * 175) - (5 * 30) + 5 = 700 + 1093.75 - 150 + 5 =
        // 1648.75
        // TDEE = 1648.75 * 1.2 = 1978.5
        assertThat(target.getCalories()).isEqualByComparingTo("1978.5");
        // Carbs (50%) = (1978.5 * 0.5) / 4 = 247.3125 -> 247.3
        assertThat(target.getCarbs()).isEqualByComparingTo("247.3");
        // Protein (20%) = (1978.5 * 0.2) / 4 = 98.925 -> 98.9
        assertThat(target.getProtein()).isEqualByComparingTo("98.9");
        // Fat (30%) = (1978.5 * 0.3) / 9 = 65.95
        // double 정밀도 문제로 65.9 또는 66.0이 나올 수 있으므로 범위로 검증하거나
        // 현재 구현값(65.9)에 맞춰 소수점 비교 수행
        assertThat(target.getFat()).isBetween(new BigDecimal("65.9"), new BigDecimal("66.0"));
    }

    // ============================================================================================
    // buildRemainingResponse 단위 테스트
    // ============================================================================================
    @Test
    @DisplayName("남은 영양성분 응답 객체 생성 테스트")
    void testBuildRemainingResponse() {
        // given (1. 목표 및 누적 섭취량 준비)
        MealServiceHelperMethods.NutritionTarget target = new MealServiceHelperMethods.NutritionTarget(
                new BigDecimal("2000"), new BigDecimal("250"), new BigDecimal("100"), new BigDecimal("60"),
                new BigDecimal("50"), new BigDecimal("2000"), new BigDecimal("300"));

        MealNutritionSummaryDTO intake = new MealNutritionSummaryDTO(
                new BigDecimal("1500"), new BigDecimal("200"), new BigDecimal("80"), new BigDecimal("50"),
                new BigDecimal("30"), new BigDecimal("1500"), new BigDecimal("200"));

        // when (2. 1일 기준 남은 영양량 계산 실행)
        RemainingNutritionResponse response = helperMethods.buildRemainingResponse(target, intake, 1);

        // then (3. 계산 검증)
        assertThat(response.getRemainingCalories()).isEqualByComparingTo("500");
        assertThat(response.getRemainingCarbs()).isEqualByComparingTo("50");
        assertThat(response.getRemainingProtein()).isEqualByComparingTo("20");
        assertThat(response.getRemainingFat()).isEqualByComparingTo("10");
        assertThat(response.getRemainingSugar()).isEqualByComparingTo("20");
        assertThat(response.getRemainingSodium()).isEqualByComparingTo("500");
        assertThat(response.getRemainingCholesterol()).isEqualByComparingTo("100");
    }
}
