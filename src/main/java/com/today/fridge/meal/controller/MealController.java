package com.today.fridge.meal.controller;

import java.util.List;
import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
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
import com.today.fridge.user.repository.UserRepository;

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
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MealLogResponse>>> getMeals(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "date", required = false) LocalDate date) {
        User user = userRepository.findByLoginId(userDetails.getUsername())
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));
        if (date == null)
            date = LocalDate.now();
        log.info("식단 기록 조회 - 사용자 ID: {}, 날짜: {}", user.getUserId(), date);
        return ResponseEntity.ok(ApiResponse.success(mealServiceDaily.getMeals(user.getUserId(), date), "식단 기록 조회 성공"));
    }

    @PostMapping("/record")
    public ResponseEntity<ApiResponse<Void>> recordMeal(@AuthenticationPrincipal UserDetails userDetails,
            @RequestBody MealLogRequest request) {
        User user = userRepository.findByLoginId(userDetails.getUsername())
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));
        log.info("식단 기록 저장 - 사용자 ID: {}, 레시피 ID: {}", user.getUserId(), request.getRecipeId());
        mealService.recordMeal(user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "식단 기록 저장 성공"));
    }

    @PostMapping("/physical-metrics")
    public ResponseEntity<ApiResponse<Void>> recordPhysicalMetrics(@AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PhysicalMetricsRequest request) {
        User user = userRepository.findByLoginId(userDetails.getUsername())
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));
        log.info("신체 지표 업데이트 - 사용자 ID: {}", user.getUserId());
        mealService.updatePhysicalMetrics(user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "신체 정보 업데이트 성공"));
    }

    @GetMapping("/daily-nutrition")
    public ResponseEntity<ApiResponse<DailyRecommendationResponse>> getDailyNutrition(
            @AuthenticationPrincipal UserDetails userDetails, @RequestParam(value = "date", required = false) LocalDate date) {
        User user = userRepository.findByLoginId(userDetails.getUsername())
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));
        if (date == null)
            date = LocalDate.now();
        log.info("일일 권장 영양 정보 조회 - 사용자 ID: {}, 날짜: {}", user.getUserId(), date);
        DailyRecommendationResponse recommendation = mealServiceDaily.getDailyRecommendation(user.getUserId(), date);
        return ResponseEntity.ok(ApiResponse.success(recommendation, "일일 영양 및 권장량 조회 성공"));
    }

    @GetMapping("/reports/weekly")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> getWeeklyReport(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "date", required = false) LocalDate date) {
        User user = userRepository.findByLoginId(userDetails.getUsername())
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));
        if (date == null)
            date = LocalDate.now();
        LocalDate startDate = date.minusDays(6);
        log.info("주간 보고서 조회 - 사용자 ID: {}, 기간: {} ~ {}", user.getUserId(), startDate, date);
        ReportSummaryResponse report = mealServicePeriod.getReportSummary(user.getUserId(), startDate, date);
        return ResponseEntity.ok(ApiResponse.success(report, "주간 영양 보고서 조회 성공"));
    }

    @GetMapping("/reports/monthly")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> getMonthlyReport(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "date", required = false) LocalDate date) {
        User user = userRepository.findByLoginId(userDetails.getUsername())
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));
        if (date == null)
            date = LocalDate.now();
        LocalDate startDate = date.minusDays(29);
        log.info("월간 보고서 조회 - 사용자 ID: {}, 기간: {} ~ {}", user.getUserId(), startDate, date);
        ReportSummaryResponse report = mealServicePeriod.getReportSummary(user.getUserId(), startDate, date);
        return ResponseEntity.ok(ApiResponse.success(report, "월간 영양 보고서 조회 성공"));
    }

    @DeleteMapping("/{mealId}")
    public ResponseEntity<ApiResponse<Void>> deleteMeal(@AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("mealId") Long mealId) {
        User user = userRepository.findByLoginId(userDetails.getUsername())
                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));
        log.info("식단 기록 삭제 - 사용자 ID: {}, 식단 ID: {}", user.getUserId(), mealId);
        mealService.deleteMeal(user.getUserId(), mealId);
        return ResponseEntity.ok(ApiResponse.success(null, "식단 기록 삭제 성공"));
    }
}
