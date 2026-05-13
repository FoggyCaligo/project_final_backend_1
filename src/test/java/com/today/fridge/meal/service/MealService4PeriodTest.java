package com.today.fridge.meal.service;

/*
 * UT-MEAL-7
 * Method: getReportSummary
 * Test Name: 영양 보고서 요약 및 결측치 대체 테스트
 * Purpose: 특정 기간의 영양 섭취 요약을 생성하고 데이터가 없는 날은 결측치를 대체한다.
 * Input: userId, startDate, endDate
 * Expected Result: 기간 동안의 평균 섭취량과 일별 데이터가 포함된 ReportSummaryResponse가 반환된다.
 * Priority: High
 */

import com.today.fridge.meal.dto.response.ReportSummaryResponse;
import com.today.fridge.meal.entity.DayNutrition;
import com.today.fridge.meal.repository.DayNutritionRepository;
import com.today.fridge.meal.repository.MealRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("recipe")
@DisplayName("기간별 식단 서비스 단위 테스트 (기간별 조회)")
public class MealService4PeriodTest {

    @InjectMocks
    private MealServicePeriod mealServicePeriod;

    @Mock
    private MealRepository mealRepository;

    @Mock
    private DayNutritionRepository dayNutritionRepository;

    @Test
    @DisplayName("영양 보고서 요약 및 결측치 대체(Imputation) 테스트")
    void testGetReportSummary() {
        Long userId = 1L;
        LocalDate startDate = LocalDate.of(2023, 10, 1);
        LocalDate endDate = LocalDate.of(2023, 10, 2); 
        
        DayNutrition day1 = new DayNutrition();
        day1.setDate(startDate.atStartOfDay());
        day1.setTotalCalories(new BigDecimal("1000")); 
        
        List<DayNutrition> dailyRecords = List.of(day1); 
        
        given(dayNutritionRepository.findByUserUserIdAndDateBetweenOrderByDateAsc(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(dailyRecords);
        
        given(mealRepository.findMaxMealsPerDayInPeriodNative(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(4);
        given(mealRepository.countByUserUserIdAndConsumedAtBetween(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(2L);

        ReportSummaryResponse summary = mealServicePeriod.getReportSummary(userId, startDate, endDate);

        assertThat(summary.getMissingDaysImputed()).isEqualTo(1);
        assertThat(summary.getAverageCalories()).isEqualByComparingTo("1250");
        assertThat(summary.getDailyData()).hasSize(2);
        assertThat(summary.getDailyData().get(0).getCalories()).isEqualByComparingTo("1000"); 
        assertThat(summary.getDailyData().get(1).getCalories()).isEqualByComparingTo("1500"); 
    }
}
