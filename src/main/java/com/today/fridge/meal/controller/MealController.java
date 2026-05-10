package com.today.fridge.meal.controller;

import java.util.List;
import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.today.fridge.global.exception.BusinessException;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.response.ApiResponse;
import com.today.fridge.meal.dto.request.MealLogRequest;
import com.today.fridge.meal.dto.request.PhysicalMetricsRequest;
import com.today.fridge.meal.dto.response.AIRecommendationResponse;
import com.today.fridge.meal.dto.response.DailyRecommendationResponse;
import com.today.fridge.meal.dto.response.MealLogResponse;
import com.today.fridge.meal.dto.response.ReportSummaryResponse;
import com.today.fridge.meal.service.MealService;
import com.today.fridge.meal.service.MealServiceDaily;
import com.today.fridge.meal.service.MealServicePeriod;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/meal")
@RequiredArgsConstructor
@Slf4j
public class MealController {

    private final MealService mealService;
    private final MealServiceDaily mealServiceDaily;
    private final MealServicePeriod mealServicePeriod;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MealLogResponse>>> getMeals(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "date", required = false) LocalDate date) {
        log.info("[MealController] getMeals (public) - userId: {}, date: {}", userId, date);
        long uid = requireUserId(userId);
        
        if (date == null) date = LocalDate.now();
        
        return ResponseEntity.ok(ApiResponse.success(mealServiceDaily.getMeals(uid, date), "식단 기록 조회 성공"));
    }


    @GetMapping("/recommendation")
    public ResponseEntity<ApiResponse<AIRecommendationResponse>> getCachedAIRecommendation(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        log.info("[MealController] getCachedAIRecommendation - userId: {}", userId);
        long uid = requireUserId(userId);

        // GET 요청은 강제 새로고침을 하지 않고 캐시된 결과를 우선 반환합니다.
        AIRecommendationResponse response = mealServiceDaily.getAIRecommendation(uid, false);
        return ResponseEntity.ok(ApiResponse.success(response, "AI 식단 추천 데이터 조회 성공"));
    }

    @PostMapping("/recommendation")
    public ResponseEntity<ApiResponse<AIRecommendationResponse>> getAIRecommendation(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        log.info("[MealController] getAIRecommendation (force refresh) - userId: {}", userId);
        long uid = requireUserId(userId);

        // POST 요청은 캐시를 무시하고 강제로 새로운 추천을 생성합니다.
        AIRecommendationResponse response = mealServiceDaily.getAIRecommendation(uid, true);
        return ResponseEntity.ok(ApiResponse.success(response, "AI 식단 추천 분석이 완료되었습니다."));
    }

    @PostMapping("/record")
    public ResponseEntity<ApiResponse<Void>> recordMeal(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody MealLogRequest request) {
        log.info("[MealController] recordMeal (public) - userId: {}, recipeId: {}", userId, request.getRecipeId());
        long uid = requireUserId(userId);
        
        mealService.recordMeal(uid, request);
        return ResponseEntity.ok(ApiResponse.success(null, "식단 기록 저장 성공"));
    }

    @PostMapping("/physical-metrics")
    public ResponseEntity<ApiResponse<Void>> recordPhysicalMetrics(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody PhysicalMetricsRequest request) {
        log.info("[MealController] recordPhysicalMetrics (public) - userId: {}", userId);
        long uid = requireUserId(userId);
        
        mealService.updatePhysicalMetrics(uid, request);
        return ResponseEntity.ok(ApiResponse.success(null, "신체 정보 업데이트 성공"));
    }

    @GetMapping("/daily-nutrition")
    public ResponseEntity<ApiResponse<DailyRecommendationResponse>> getDailyNutrition(
            @RequestHeader(value = "X-User-Id", required = false) Long userId, 
            @RequestParam(value = "date", required = false) LocalDate date) {
        log.info("[MealController] getDailyNutrition (public) - userId: {}, date: {}", userId, date);
        long uid = requireUserId(userId);
        
        if (date == null) date = LocalDate.now();
        
        DailyRecommendationResponse recommendation = mealServiceDaily.getDailyRecommendation(uid, date);
        return ResponseEntity.ok(ApiResponse.success(recommendation, "일일 영양 및 권장량 조회 성공"));
    }

    @GetMapping("/reports/weekly")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> getWeeklyReport(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "date", required = false) LocalDate date) {
        log.info("[MealController] getWeeklyReport (public) - userId: {}, date: {}", userId, date);
        long uid = requireUserId(userId);
        
        if (date == null) date = LocalDate.now();
        LocalDate startDate = date.minusDays(6);
        
        ReportSummaryResponse report = mealServicePeriod.getReportSummary(uid, startDate, date);
        return ResponseEntity.ok(ApiResponse.success(report, "주간 영양 보고서 조회 성공"));
    }

    @GetMapping("/reports/monthly")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> getMonthlyReport(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "date", required = false) LocalDate date) {
        log.info("[MealController] getMonthlyReport (public) - userId: {}, date: {}", userId, date);
        long uid = requireUserId(userId);
        
        if (date == null) date = LocalDate.now();
        LocalDate startDate = date.minusDays(29);
        
        ReportSummaryResponse report = mealServicePeriod.getReportSummary(uid, startDate, date);
        return ResponseEntity.ok(ApiResponse.success(report, "월간 영양 보고서 조회 성공"));
    }

    @DeleteMapping("/{mealId}")
    public ResponseEntity<ApiResponse<Void>> deleteMeal(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("mealId") Long mealId) {
        log.info("[MealController] deleteMeal (public) - userId: {}, mealId: {}", userId, mealId);
        long uid = requireUserId(userId);
        
        mealService.deleteMeal(uid, mealId);
        return ResponseEntity.ok(ApiResponse.success(null, "식단 기록 삭제 성공"));
    }

    private static long requireUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}