package com.today.fridge.meal.controller;

import java.util.List;
import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.meal.dto.request.MealLogRequest;
import com.today.fridge.meal.dto.request.PhysicalMetricsRequest;
import com.today.fridge.meal.dto.response.DailyRecommendationResponse;
import com.today.fridge.meal.dto.response.MealLogResponse;
import com.today.fridge.meal.dto.response.ReportSummaryResponse;
import com.today.fridge.meal.service.MealService;
import com.today.fridge.meal.service.MealServiceDaily;
import com.today.fridge.meal.service.MealServicePeriod;
import com.today.fridge.user.entity.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/meal")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;
    private final MealServiceDaily mealServiceDaily;
    private final MealServicePeriod mealServicePeriod;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MealLogResponse>>> getMeals(@AuthenticationPrincipal User user,
            @RequestParam(value = "date", required = false) LocalDate date) {
        if (date == null)
            date = LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(mealServiceDaily.getMeals(user.getUserId(), date), "식단 기록 조회 성공"));
    }

    @PostMapping("/record")
    public ResponseEntity<ApiResponse<Void>> recordMeal(@RequestBody MealLogRequest request) {
        mealService.recordMeal(request);
        return ResponseEntity.ok(ApiResponse.success(null, "식단 기록 저장 성공"));
    }

    @PostMapping("/physical-metrics")
    public ResponseEntity<ApiResponse<Void>> recordPhysicalMetrics(@AuthenticationPrincipal User user,
            @RequestBody PhysicalMetricsRequest request) {
        mealService.updatePhysicalMetrics(user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "신체 정보 업데이트 성공"));
    }

    @GetMapping("/daily-nutrition")
    public ResponseEntity<ApiResponse<DailyRecommendationResponse>> getDailyNutrition(
            @AuthenticationPrincipal User user, @RequestParam(value = "date", required = false) LocalDate date) {
        if (date == null)
            date = LocalDate.now();
        DailyRecommendationResponse recommendation = mealServiceDaily.getDailyRecommendation(user.getUserId(), date);
        return ResponseEntity.ok(ApiResponse.success(recommendation, "일일 영양 및 권장량 조회 성공"));
    }

    @GetMapping("/reports/weekly")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> getWeeklyReport(@AuthenticationPrincipal User user,
            @RequestParam(value = "date", required = false) LocalDate date) {
        if (date == null)
            date = LocalDate.now();
        LocalDate startDate = date.minusDays(6);
        ReportSummaryResponse report = mealServicePeriod.getReportSummary(user.getUserId(), startDate, date);
        return ResponseEntity.ok(ApiResponse.success(report, "주간 영양 보고서 조회 성공"));
    }

    @GetMapping("/reports/monthly")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> getMonthlyReport(@AuthenticationPrincipal User user,
            @RequestParam(value = "date", required = false) LocalDate date) {
        if (date == null)
            date = LocalDate.now();
        LocalDate startDate = date.minusDays(29);
        ReportSummaryResponse report = mealServicePeriod.getReportSummary(user.getUserId(), startDate, date);
        return ResponseEntity.ok(ApiResponse.success(report, "월간 영양 보고서 조회 성공"));
    }
}
