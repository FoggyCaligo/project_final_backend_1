package com.today.fridge.meal.service;

/*
 * UT-MEAL-5
 * Method: getMeals
 * Test Name: 일일 식단 목록 조회 테스트
 * Purpose: 특정 날짜에 사용자가 기록한 식단 목록을 반환한다.
 * Input: userId, date
 * Expected Result: 해당 날짜의 MealLogResponse 리스트가 반환된다.
 * Priority: Medium
 *
 * UT-MEAL-6
 * Method: getDailyRecommendation
 * Test Name: 일일 영양 권장량 및 피드백 조회 테스트
 * Purpose: 일일 권장 목표와 현재 섭취량을 비교하여 피드백을 생성한다.
 * Input: userId, date
 * Expected Result: 목표치, 현재 섭취량, 그리고 조언이 포함된 DailyRecommendationResponse가 반환된다.
 * Priority: High
 */

import com.today.fridge.global.external.fastapi.FastApiService;
import com.today.fridge.global.external.fastapi.MealRecommendationFastAPIService;
import com.today.fridge.global.external.fastapi.MealRecommendationRequest;
import com.today.fridge.global.external.fastapi.MealRecommendationResponse;
import com.today.fridge.meal.dto.response.AIRecommendationResponse;
import com.today.fridge.meal.dto.response.DailyRecommendationResponse;
import com.today.fridge.meal.dto.response.MealLogResponse;
import com.today.fridge.meal.dto.response.MealNutritionSummaryDTO;
import com.today.fridge.meal.entity.DayNutrition;
import com.today.fridge.meal.repository.DayNutritionRepository;
import com.today.fridge.meal.repository.MealRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("recipe")
@DisplayName("일일 식단 서비스 단위 테스트 (일별 조회)")
public class MealService3DailyTest {

    @InjectMocks
    private MealServiceDaily mealServiceDaily;

    @Mock
    private MealRepository mealRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DayNutritionRepository dayNutritionRepository;

    @Mock
    private FastApiService fastApiService;

    @Mock
    private MealRecommendationFastAPIService mealRecommendationFastAPIService;

    @Mock
    private MealServiceHelperMethods helperMethods;

    @Test
    @DisplayName("AI 기반 식단 추천 분석 테스트")
    void testGetAIRecommendation() {
        // given
        Long userId = 1L;
        User user = User.create("dailytest", "daily@todayfridge.com", "hashedpassword", "데일리테스터");
        ReflectionTestUtils.setField(user, "userId", userId);
        user.setHeightCm(175.0);
        user.setWeightKg(70.0);
        user.setAge(30);
        user.setGender("MALE");

        MealRecommendationResponse mockResponse = new MealRecommendationResponse("Good report", null);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(mealRepository.countByUserUserIdAndConsumedAtBetween(eq(userId), any(), any())).willReturn(1L);
        given(mealRecommendationFastAPIService.getMealRecommendation(any(MealRecommendationRequest.class))).willReturn(mockResponse);

        // when
        AIRecommendationResponse result = mealServiceDaily.getAIRecommendation(userId);

        // then
        assertThat(result.getReport()).isEqualTo("Good report");
    }

    @Test
    @DisplayName("일일 식단 목록 조회 테스트")
    void testGetMeals() {
        Long userId = 1L;
        LocalDate date = LocalDate.of(2023, 10, 1);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        given(mealRepository.findByUserIdAndConsumedAtBetween(userId, startOfDay, endOfDay))
                .willReturn(List.of(new MealLogResponse(1L, 10L, "Salad", new BigDecimal("1"), startOfDay, startOfDay)));

        List<MealLogResponse> result = mealServiceDaily.getMeals(userId, date);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRecipeTitle()).isEqualTo("Salad");
    }

    @Test
    @DisplayName("일일 영양 권장량 및 피드백 조회 테스트")
    void testGetDailyRecommendation() {
        Long userId = 1L;
        LocalDate date = LocalDate.of(2023, 10, 1);
        User user = User.create("dailytest", "daily@todayfridge.com", "hashedpassword", "데일리테스터");
        ReflectionTestUtils.setField(user, "userId", userId);
        user.setHeightCm(175.0);
        user.setWeightKg(70.0);
        user.setAge(30);
        user.setGender("MALE");

        MealServiceHelperMethods.NutritionTarget target = new MealServiceHelperMethods.NutritionTarget(
                new BigDecimal("2000"), new BigDecimal("250"), new BigDecimal("100"), new BigDecimal("60"),
                new BigDecimal("50"), new BigDecimal("2000"), new BigDecimal("300")
        );

        DayNutrition dayNutrition = new DayNutrition();
        dayNutrition.setTotalCalories(new BigDecimal("1500"));
        dayNutrition.setTotalCarbs(new BigDecimal("200"));
        dayNutrition.setTotalProtein(new BigDecimal("80"));
        dayNutrition.setTotalFat(new BigDecimal("50"));
        dayNutrition.setTotalSugar(new BigDecimal("30"));
        dayNutrition.setTotalSodium(new BigDecimal("1000"));
        dayNutrition.setTotalCholesterol(new BigDecimal("150"));

        MealNutritionSummaryDTO intake = new MealNutritionSummaryDTO(
            new BigDecimal("1500"), new BigDecimal("200"), new BigDecimal("80"), new BigDecimal("50"), 
            new BigDecimal("30"), new BigDecimal("1000"), new BigDecimal("150")
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(helperMethods.calculateTargetsWithDefaults(any(User.class))).willReturn(target);
        given(dayNutritionRepository.getNutritionSummaryByDateRange(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(intake);

        DailyRecommendationResponse response = mealServiceDaily.getDailyRecommendation(userId, date);

        assertThat(response.getTargetCalories()).isEqualByComparingTo("2000");
        assertThat(response.getCurrentCalories()).isEqualByComparingTo("1500");
        assertThat(response.getAdvice()).anyMatch(advice -> advice.contains("남은 권장 칼로리는 500.0 kcal"));
    }
}
