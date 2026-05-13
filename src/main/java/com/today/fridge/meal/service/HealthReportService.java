package com.today.fridge.meal.service;

import com.today.fridge.ingredient.repository.UserIngredientRepository;
import com.today.fridge.llm.client.FastApiLlmClient;
import com.today.fridge.meal.dto.request.FastApiHealthReportRequest;
import com.today.fridge.meal.dto.response.FastApiHealthReportResponse;
import com.today.fridge.meal.entity.HealthReport;
import com.today.fridge.meal.entity.Meal;
import com.today.fridge.meal.repository.HealthReportRepository;
import com.today.fridge.meal.repository.MealRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import com.today.fridge.global.exception.BusinessException;
import com.today.fridge.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthReportService {

    private final HealthReportRepository healthReportRepository;
    private final UserIngredientRepository userIngredientRepository;
    private final MealRepository mealRepository;
    private final UserRepository userRepository;
    private final FastApiLlmClient fastApiLlmClient;

    @Transactional
    public FastApiHealthReportResponse generateHealthReport(Long userId) {
        log.info("[HealthReportService] Generating health report for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 1. Fetch fridge ingredients
        List<String> ingredientNames = userIngredientRepository.findOwnedIngredientNamesByUserId(userId);

        // 2. Fetch recent meals (last 7 days)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Meal> meals = mealRepository.findByUserUserIdAndConsumedAtBetween(userId, sevenDaysAgo, LocalDateTime.now());
        
        List<Map<String, Object>> recentMealsData = meals.stream()
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    String foodName = m.getRecipe() != null ? m.getRecipe().getTitle() : m.getCustomFoodName();
                    map.put("food_name", foodName);
                    map.put("consumed_at", m.getConsumedAt().toString());
                    return map;
                })
                .collect(Collectors.toList());

        // 3. Prepare request for FastAPI
        FastApiHealthReportRequest request = new FastApiHealthReportRequest(ingredientNames, recentMealsData);

        // 4. Call FastAPI
        FastApiHealthReportResponse response;
        try {
            response = fastApiLlmClient.generateHealthReport(request);
        } catch (Exception e) {
            log.error("[HealthReportService] Failed to call FastAPI health report", e);
            throw new BusinessException(ErrorCode.AI_RECOGNITION_FAILED, "AI 건강 레포트 생성에 실패했습니다.");
        }

        // 5. Save to DB
        HealthReport healthReport = HealthReport.builder()
                .user(user)
                .summary(response.getSummary())
                .advice(response.getAdvice())
                .meals(response.getMeals())
                .videos(response.getVideos())
                .build();

        healthReportRepository.save(healthReport);

        return response;
    }

    @Transactional(readOnly = true)
    public FastApiHealthReportResponse getLatestHealthReport(Long userId) {
        return healthReportRepository.findFirstByUserUserIdOrderByCreatedAtDesc(userId)
                .map(hr -> new FastApiHealthReportResponse(
                        hr.getSummary(),
                        new ArrayList<>(hr.getAdvice()),
                        new ArrayList<>(hr.getMeals()),
                        new ArrayList<>(hr.getVideos())
                ))
                .orElse(null);
    }
}
