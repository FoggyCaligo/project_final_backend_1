package com.today.fridge.meal.service;

/*
 * MealServicePeriod는 주간, 월간 등 장기적인 식단 리포트 및 통계 계산을 담당하는 서비스입니다.
 * getReportSummary: 특정 기간 동안의 영양 섭취 보고서를 생성합니다. 데이터가 부족한 날은 평균치를 기반으로 결측치를 보정(Imputation)합니다.
 * getRemainingWeeklyNutrition: 이번 주 남은 권장 영양 섭취량을 계산합니다.
 * getRemainingMonthlyNutrition: 이번 달 남은 권장 영양 섭취량을 계산합니다.
 */

import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.meal.dto.response.MealNutritionSummaryDTO;
import com.today.fridge.meal.dto.response.RemainingNutritionResponse;
import com.today.fridge.meal.dto.response.ReportSummaryResponse;
import com.today.fridge.meal.entity.DayNutrition;
import com.today.fridge.meal.repository.DayNutritionRepository;
import com.today.fridge.meal.repository.MealRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MealServicePeriod {

    private final MealRepository mealRepository;
    private final UserRepository userRepository;
    private final DayNutritionRepository dayNutritionRepository;
    private final MealServiceHelperMethods helperMethods;

    // ============================================================================================
    // 특정 기간(주간/월간 등) 영양 보고서 요약 조회
    // 결측치 대체(Imputation) 로직 포함
    // ============================================================================================
    public ReportSummaryResponse getReportSummary(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("[MealServicePeriod] getReportSummary (public) - userId: {}, startDate: {}, endDate: {}", userId, startDate, endDate);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // 기간 내 일일 영양 데이터 조회
        List<DayNutrition> dailyRecords = dayNutritionRepository.findByUserUserIdAndDateBetweenOrderByDateAsc(userId,
                startDateTime, endDateTime);

        // 해당 기간 동안 하루 최대 식사 횟수 조회
        Integer maxMeals = mealRepository.findMaxMealsPerDayInPeriodNative(userId, startDateTime, endDateTime);

        // 결측치 대체를 위한 기준 식사 횟수 설정 (최소 3회 기준, M-1 법칙 적용하되 하한선 3회)
        // 사용자가 식사를 한 기록이 있는 경우에만 결측치 대체를 수행
        int imputedMealsCount = 0;
        if (maxMeals != null && maxMeals > 0) {
            // maxMeals가 4보다 작으면(예: 1, 2, 3) 3회로 고정, 4 이상이면 maxMeals - 1회로 계산
            imputedMealsCount = (maxMeals < 4) ? 3 : (maxMeals - 1);
        }

        // 기간 내 총 영양 섭취량 합산
        BigDecimal sumCals = BigDecimal.ZERO, sumCarbs = BigDecimal.ZERO, sumProtein = BigDecimal.ZERO,
                sumFat = BigDecimal.ZERO;
        BigDecimal sumSugar = BigDecimal.ZERO, sumSodium = BigDecimal.ZERO, sumCholesterol = BigDecimal.ZERO;
        for (DayNutrition dn : dailyRecords) {
            sumCals = sumCals.add(dn.getTotalCalories() != null ? dn.getTotalCalories() : BigDecimal.ZERO);
            sumCarbs = sumCarbs.add(dn.getTotalCarbs() != null ? dn.getTotalCarbs() : BigDecimal.ZERO);
            sumProtein = sumProtein.add(dn.getTotalProtein() != null ? dn.getTotalProtein() : BigDecimal.ZERO);
            sumFat = sumFat.add(dn.getTotalFat() != null ? dn.getTotalFat() : BigDecimal.ZERO);
            sumSugar = sumSugar.add(dn.getTotalSugar() != null ? dn.getTotalSugar() : BigDecimal.ZERO);
            sumSodium = sumSodium.add(dn.getTotalSodium() != null ? dn.getTotalSodium() : BigDecimal.ZERO);
            sumCholesterol = sumCholesterol
                    .add(dn.getTotalCholesterol() != null ? dn.getTotalCholesterol() : BigDecimal.ZERO);
        }

        // 1끼당 평균 영양 섭취량 계산
        long totalMealsInPeriod = mealRepository.countByUserUserIdAndConsumedAtBetween(userId, startDateTime,
                endDateTime);
        BigDecimal avgCalsPerMeal = BigDecimal.ZERO;
        BigDecimal avgCarbsPerMeal = BigDecimal.ZERO;
        BigDecimal avgProteinPerMeal = BigDecimal.ZERO;
        BigDecimal avgFatPerMeal = BigDecimal.ZERO;
        BigDecimal avgSugarPerMeal = BigDecimal.ZERO;
        BigDecimal avgSodiumPerMeal = BigDecimal.ZERO;
        BigDecimal avgCholesterolPerMeal = BigDecimal.ZERO;

        if (totalMealsInPeriod > 0) {
            BigDecimal divisor = new BigDecimal(totalMealsInPeriod);
            avgCalsPerMeal = sumCals.divide(divisor, 2, RoundingMode.HALF_UP);
            avgCarbsPerMeal = sumCarbs.divide(divisor, 2, RoundingMode.HALF_UP);
            avgProteinPerMeal = sumProtein.divide(divisor, 2, RoundingMode.HALF_UP);
            avgFatPerMeal = sumFat.divide(divisor, 2, RoundingMode.HALF_UP);
            avgSugarPerMeal = sumSugar.divide(divisor, 2, RoundingMode.HALF_UP);
            avgSodiumPerMeal = sumSodium.divide(divisor, 2, RoundingMode.HALF_UP);
            avgCholesterolPerMeal = sumCholesterol.divide(divisor, 2, RoundingMode.HALF_UP);
        }

        // 결측치(데이터가 없는 날)에 적용할 하루 대체 영양량 계산
        BigDecimal imputedDailyCals = avgCalsPerMeal.multiply(new BigDecimal(imputedMealsCount));
        BigDecimal imputedDailyCarbs = avgCarbsPerMeal.multiply(new BigDecimal(imputedMealsCount));
        BigDecimal imputedDailyProtein = avgProteinPerMeal.multiply(new BigDecimal(imputedMealsCount));
        BigDecimal imputedDailyFat = avgFatPerMeal.multiply(new BigDecimal(imputedMealsCount));
        BigDecimal imputedDailySugar = avgSugarPerMeal.multiply(new BigDecimal(imputedMealsCount));
        BigDecimal imputedDailySodium = avgSodiumPerMeal.multiply(new BigDecimal(imputedMealsCount));
        BigDecimal imputedDailyCholesterol = avgCholesterolPerMeal.multiply(new BigDecimal(imputedMealsCount));

        List<ReportSummaryResponse.ReportDailyData> dailyDataList = new ArrayList<>();
        BigDecimal totalReportCals = BigDecimal.ZERO;
        BigDecimal totalReportCarbs = BigDecimal.ZERO;
        BigDecimal totalReportProtein = BigDecimal.ZERO;
        BigDecimal totalReportFat = BigDecimal.ZERO;
        BigDecimal totalReportSugar = BigDecimal.ZERO;
        BigDecimal totalReportSodium = BigDecimal.ZERO;
        BigDecimal totalReportCholesterol = BigDecimal.ZERO;
        int missingDaysImputed = 0;

        // 기간 내 모든 날짜를 순회하며 데이터 구성 및 결측치 채우기
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            final LocalDate currentDate = date;
            Optional<DayNutrition> recordOpt = dailyRecords.stream()
                    .filter(dn -> dn.getDate().toLocalDate().equals(currentDate))
                    .findFirst();

            if (recordOpt.isPresent()) {
                // 기록이 존재하는 날짜: 실제 데이터 사용
                DayNutrition dn = recordOpt.get();
                dailyDataList.add(new ReportSummaryResponse.ReportDailyData(
                        date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        dn.getTotalCalories() != null ? dn.getTotalCalories() : BigDecimal.ZERO,
                        dn.getTotalCarbs() != null ? dn.getTotalCarbs() : BigDecimal.ZERO,
                        dn.getTotalProtein() != null ? dn.getTotalProtein() : BigDecimal.ZERO,
                        dn.getTotalFat() != null ? dn.getTotalFat() : BigDecimal.ZERO,
                        dn.getTotalSugar() != null ? dn.getTotalSugar() : BigDecimal.ZERO,
                        dn.getTotalSodium() != null ? dn.getTotalSodium() : BigDecimal.ZERO,
                        dn.getTotalCholesterol() != null ? dn.getTotalCholesterol() : BigDecimal.ZERO,
                        false));
                totalReportCals = totalReportCals
                        .add(dn.getTotalCalories() != null ? dn.getTotalCalories() : BigDecimal.ZERO);
                totalReportCarbs = totalReportCarbs
                        .add(dn.getTotalCarbs() != null ? dn.getTotalCarbs() : BigDecimal.ZERO);
                totalReportProtein = totalReportProtein
                        .add(dn.getTotalProtein() != null ? dn.getTotalProtein() : BigDecimal.ZERO);
                totalReportFat = totalReportFat.add(dn.getTotalFat() != null ? dn.getTotalFat() : BigDecimal.ZERO);
                totalReportSugar = totalReportSugar
                        .add(dn.getTotalSugar() != null ? dn.getTotalSugar() : BigDecimal.ZERO);
                totalReportSodium = totalReportSodium
                        .add(dn.getTotalSodium() != null ? dn.getTotalSodium() : BigDecimal.ZERO);
                totalReportCholesterol = totalReportCholesterol
                        .add(dn.getTotalCholesterol() != null ? dn.getTotalCholesterol() : BigDecimal.ZERO);
            } else {
                // 기록이 없는 날짜: 결측치 대체 데이터 사용
                missingDaysImputed++;
                dailyDataList.add(new ReportSummaryResponse.ReportDailyData(
                        date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        imputedDailyCals, imputedDailyCarbs, imputedDailyProtein, imputedDailyFat, imputedDailySugar,
                        imputedDailySodium, imputedDailyCholesterol, true));
                totalReportCals = totalReportCals.add(imputedDailyCals);
                totalReportCarbs = totalReportCarbs.add(imputedDailyCarbs);
                totalReportProtein = totalReportProtein.add(imputedDailyProtein);
                totalReportFat = totalReportFat.add(imputedDailyFat);
                totalReportSugar = totalReportSugar.add(imputedDailySugar);
                totalReportSodium = totalReportSodium.add(imputedDailySodium);
                totalReportCholesterol = totalReportCholesterol.add(imputedDailyCholesterol);
            }
        }

        // 전체 평균 계산
        int totalDays = dailyDataList.size();
        BigDecimal daysDivisor = new BigDecimal(totalDays);

        // 최종 보고서 요약 응답 반환
        ReportSummaryResponse response = ReportSummaryResponse.builder()
                .averageCalories(
                        totalDays > 0 ? totalReportCals.divide(daysDivisor, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .averageCarbs(
                        totalDays > 0 ? totalReportCarbs.divide(daysDivisor, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .averageProtein(totalDays > 0 ? totalReportProtein.divide(daysDivisor, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .averageFat(
                        totalDays > 0 ? totalReportFat.divide(daysDivisor, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .averageSugar(
                        totalDays > 0 ? totalReportSugar.divide(daysDivisor, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .averageSodium(totalDays > 0 ? totalReportSodium.divide(daysDivisor, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .averageCholesterol(totalDays > 0 ? totalReportCholesterol.divide(daysDivisor, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .missingDaysImputed(missingDaysImputed)
                .dailyData(dailyDataList)
                .build();
        log.info("보고서 생성 완료 - 사용자 ID: {}, 기간: {} ~ {}. 결측치 보정 일수: {}", userId, startDate, endDate, missingDaysImputed);
        return response;
    }

    // ============================================================================================
    // 이번 주 남은 권장 영양 섭취량 조회
    // ============================================================================================
    public RemainingNutritionResponse getRemainingWeeklyNutrition(Long userId, LocalDate date) {
        log.info("[MealServicePeriod] getRemainingWeeklyNutrition (public) - userId: {}, date: {}", userId, date);
        // 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));

        // 일일 권장 목표 계산 로직 호출 (중앙화된 기본값 처리 포함)
        MealServiceHelperMethods.NutritionTarget target = helperMethods.calculateTargetsWithDefaults(user);

        // 이번 주 시작일 및 종료일 계산
        int dayOfWeek = date.getDayOfWeek().getValue();
        LocalDate startOfWeek = date.minusDays(dayOfWeek - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        LocalDateTime startDateTime = startOfWeek.atStartOfDay();
        LocalDateTime endDateTime = endOfWeek.plusDays(1).atStartOfDay();

        // 주간 영양 섭취량 조회
        MealNutritionSummaryDTO intake = dayNutritionRepository.getNutritionSummaryByDateRange(userId, startDateTime,
                endDateTime);

        // 남은 주간 영양 섭취량 계산 (7일 기준)
        return helperMethods.buildRemainingResponse(target, intake, 7);
    }

    // ============================================================================================
    // 이번 달 남은 권장 영양 섭취량 조회
    // ============================================================================================
    public RemainingNutritionResponse getRemainingMonthlyNutrition(Long userId, LocalDate date) {
        log.info("[MealServicePeriod] getRemainingMonthlyNutrition (public) - userId: {}, date: {}", userId, date);
        // 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));

        // 일일 권장 목표 계산 로직 호출 (중앙화된 기본값 처리 포함)
        MealServiceHelperMethods.NutritionTarget target = helperMethods.calculateTargetsWithDefaults(user);

        // 이번 달 시작일 및 종료일 계산
        LocalDate startOfMonth = date.withDayOfMonth(1);
        LocalDate endOfMonth = date.withDayOfMonth(date.lengthOfMonth());

        LocalDateTime startDateTime = startOfMonth.atStartOfDay();
        LocalDateTime endDateTime = endOfMonth.plusDays(1).atStartOfDay();

        // 월간 영양 섭취량 조회
        MealNutritionSummaryDTO intake = dayNutritionRepository.getNutritionSummaryByDateRange(userId, startDateTime,
                endDateTime);

        // 남은 월간 영양 섭취량 계산 (해당 월의 총 일수 기준)
        return helperMethods.buildRemainingResponse(target, intake, date.lengthOfMonth());
    }
}
