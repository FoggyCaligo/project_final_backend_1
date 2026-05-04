package com.today.fridge.meal.service;

import com.today.fridge.meal.dto.response.DailyRecommendationResponse;
import com.today.fridge.meal.dto.response.MealLogResponse;
import com.today.fridge.meal.dto.response.MealNutritionSummaryDTO;
import com.today.fridge.meal.dto.response.RemainingNutritionResponse;
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

/**
 * MealServiceDaily (Daily Reads)에 대한 단위 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("MealServiceDaily 단위 테스트 (Daily Reads)")
public class MealServiceDailyUnitTest {

    @InjectMocks
    private MealServiceDaily mealServiceDaily;

    @Mock
    private MealRepository mealRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DayNutritionRepository dayNutritionRepository;

    @Mock
    private MealServiceHelperMethods helperMethods;

    // ============================================================================================
    // getMeals 단위 테스트
    // ============================================================================================
    @Test
    @DisplayName("일일 식단 목록 조회 테스트")
    void testGetMeals() {
        // given (1. 날짜 및 Mock 설정)
        Long userId = 1L;
        LocalDate date = LocalDate.of(2023, 10, 1);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        given(mealRepository.findByUserIdAndConsumedAtBetween(userId, startOfDay, endOfDay))
                .willReturn(List.of(new MealLogResponse(1L, 10L, "Salad", new BigDecimal("1"), startOfDay, startOfDay)));

        // when (2. 로직 호출)
        List<MealLogResponse> result = mealServiceDaily.getMeals(userId, date);

        // then (3. 결과 검증)
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRecipeTitle()).isEqualTo("Salad");
    }

    // ============================================================================================
    // getDailyRecommendation 단위 테스트
    // ============================================================================================
    @Test
    @DisplayName("일일 영양 권장량 및 피드백 조회 테스트")
    void testGetDailyRecommendation() {
        // given (1. 유저, 목표치, 누적 영양 정보 설정)
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

        // when (2. 로직 호출)
        DailyRecommendationResponse response = mealServiceDaily.getDailyRecommendation(userId, date);

        // then (3. 목표치와 현재 섭취량, 피드백 내용 검증)
        assertThat(response.getTargetCalories()).isEqualByComparingTo("2000");
        assertThat(response.getCurrentCalories()).isEqualByComparingTo("1500");
        assertThat(response.getAdvice()).anyMatch(advice -> advice.contains("남은 권장 칼로리는 500.0 kcal"));
    }

    // ============================================================================================
    // getRemainingDailyNutrition 단위 테스트
    // ============================================================================================
    @Test
    @DisplayName("오늘 남은 권장 영양 섭취량 조회 테스트")
    void testGetRemainingDailyNutrition() {
        // given (1. Mock 설정)
        Long userId = 1L;
        LocalDate date = LocalDate.of(2023, 10, 1);
        User user = User.create("dailytest2", "daily2@todayfridge.com", "hashedpassword", "데일리테스터2");
        ReflectionTestUtils.setField(user, "userId", userId);
        
        MealServiceHelperMethods.NutritionTarget target = new MealServiceHelperMethods.NutritionTarget(
                new BigDecimal("2000"), new BigDecimal("250"), new BigDecimal("100"), new BigDecimal("60"),
                new BigDecimal("50"), new BigDecimal("2000"), new BigDecimal("300")
        );

        MealNutritionSummaryDTO intake = new MealNutritionSummaryDTO(
            new BigDecimal("500"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );

        RemainingNutritionResponse expectedResponse = RemainingNutritionResponse.builder().remainingCalories(new BigDecimal("1500")).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(helperMethods.calculateTargetsWithDefaults(any(User.class))).willReturn(target);
        given(dayNutritionRepository.getNutritionSummaryByDateRange(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(intake);
        given(helperMethods.buildRemainingResponse(target, intake, 1)).willReturn(expectedResponse);

        // when (2. 로직 호출)
        RemainingNutritionResponse result = mealServiceDaily.getRemainingDailyNutrition(userId, date);

        // then (3. 반환값 검증)
        assertThat(result.getRemainingCalories()).isEqualByComparingTo("1500");
    }
}
