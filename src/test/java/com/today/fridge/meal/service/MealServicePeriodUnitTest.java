package com.today.fridge.meal.service;

import com.today.fridge.meal.dto.response.MealNutritionSummaryDTO;
import com.today.fridge.meal.dto.response.RemainingNutritionResponse;
import com.today.fridge.meal.dto.response.ReportSummaryResponse;
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
 * MealServicePeriod (Period Reads)에 대한 단위 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("recipe")
@DisplayName("MealServicePeriod 단위 테스트 (Period Reads)")
public class MealServicePeriodUnitTest {

    @InjectMocks
    private MealServicePeriod mealServicePeriod;

    @Mock
    private MealRepository mealRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DayNutritionRepository dayNutritionRepository;

    @Mock
    private MealServiceHelperMethods helperMethods;

    // ============================================================================================
    // getReportSummary 단위 테스트
    // ============================================================================================
    @Test
    @DisplayName("영양 보고서 요약 및 결측치 대체(Imputation) 테스트")
    void testGetReportSummary() {
        // given (1. 기간 및 기초 데이터 준비)
        Long userId = 1L;
        LocalDate startDate = LocalDate.of(2023, 10, 1);
        LocalDate endDate = LocalDate.of(2023, 10, 2); // 2일 간의 리포트
        
        DayNutrition day1 = new DayNutrition();
        day1.setDate(startDate.atStartOfDay());
        day1.setTotalCalories(new BigDecimal("1000")); // 첫째 날 기록 존재
        
        List<DayNutrition> dailyRecords = List.of(day1); // 둘째 날은 결측됨
        
        given(dayNutritionRepository.findByUserUserIdAndDateBetweenOrderByDateAsc(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(dailyRecords);
        
        // 하루 최대 식사 횟수를 4로 설정 (실제 M-1 보정을 위해 사용)
        given(mealRepository.findMaxMealsPerDayInPeriodNative(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(4);
        given(mealRepository.countByUserUserIdAndConsumedAtBetween(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(2L);

        // when (2. 리포트 생성 로직 호출)
        ReportSummaryResponse summary = mealServicePeriod.getReportSummary(userId, startDate, endDate);

        // then (3. 결측치 로직 및 평균치 검증)
        // 총 식사 2회, 총 칼로리 1000 => 끼니당 평균 500
        // 기준 식사 수 = 4 => 보정 횟수 = 3
        // 결측된 날의 칼로리 = 500 * 3 = 1500
        // 2일 전체 칼로리 합 = 1000(실제) + 1500(결측 보정) = 2500
        // 일일 평균 = 2500 / 2 = 1250
        assertThat(summary.getMissingDaysImputed()).isEqualTo(1);
        assertThat(summary.getAverageCalories()).isEqualByComparingTo("1250");
        assertThat(summary.getDailyData()).hasSize(2);
        assertThat(summary.getDailyData().get(0).getCalories()).isEqualByComparingTo("1000"); // 실제 데이터 확인
        assertThat(summary.getDailyData().get(1).getCalories()).isEqualByComparingTo("1500"); // 보정 데이터 확인
    }

    // ============================================================================================
    // getRemainingWeeklyNutrition 단위 테스트
    // ============================================================================================
    @Test
    @DisplayName("이번 주 남은 권장 영양 섭취량 계산 테스트")
    void testGetRemainingWeeklyNutrition() {
        // given (1. Mock 데이터 준비)
        Long userId = 1L;
        LocalDate date = LocalDate.of(2023, 10, 4); // 수요일
        User user = User.create("periodtest", "period@todayfridge.com", "hashedpassword", "피리어드테스터");
        ReflectionTestUtils.setField(user, "userId", userId);
        
        MealServiceHelperMethods.NutritionTarget target = new MealServiceHelperMethods.NutritionTarget(
                new BigDecimal("2000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );

        MealNutritionSummaryDTO intake = new MealNutritionSummaryDTO(
            new BigDecimal("5000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );

        RemainingNutritionResponse expectedResponse = RemainingNutritionResponse.builder().remainingCalories(new BigDecimal("1285.7")).build(); // Mock 반환값

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(helperMethods.calculateTargetsWithDefaults(any(User.class))).willReturn(target);
        given(dayNutritionRepository.getNutritionSummaryByDateRange(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(intake);
        given(helperMethods.buildRemainingResponse(target, intake, 7)).willReturn(expectedResponse); // 주간 = 7일 기준

        // when (2. 로직 호출)
        RemainingNutritionResponse result = mealServicePeriod.getRemainingWeeklyNutrition(userId, date);

        // then (3. 헬퍼 메서 반환값 연결 확인)
        assertThat(result.getRemainingCalories()).isEqualByComparingTo("1285.7");
    }
}
