package com.today.fridge.meal.service;

/*
 * MealService는 식단 기록의 생성 및 수정을 담당하는 핵심 서비스입니다.
 * recordMeal: 사용자의 식단 기록을 저장하고, 해당 날짜의 누적 영양 정보를 업데이트합니다.
 * updatePhysicalMetrics: 사용자의 키, 몸무게, 나이, 성별 등 신체 지표를 업데이트합니다.
 */

import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.meal.dto.request.MealLogRequest;
import com.today.fridge.meal.dto.request.PhysicalMetricsRequest;
import com.today.fridge.meal.entity.Meal;
import com.today.fridge.meal.repository.MealRepository;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MealService {

        private final MealRepository mealRepository;
        private final UserRepository userRepository;
        private final RecipeRepository recipeRepository;
        private final MealServiceHelperMethods helperMethods;

        // ============================================================================================
        // 식단 기록 저장
        // ============================================================================================
        @Transactional
        public void recordMeal(MealLogRequest request) {
                // 사용자 정보 조회
                User user = userRepository.findById(request.getUserId())
                                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));

                // 레시피 정보 조회
                Recipe recipe = recipeRepository.findById(request.getRecipeId())
                                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.RECIPE_NOT_FOUND));

                // 식단 엔티티 생성 및 설정
                Meal meal = new Meal();
                meal.setUser(user);
                meal.setRecipe(recipe);

                // 인분 수 및 섭취 시간 설정 (기본값 처리)
                BigDecimal servings = request.getServings() != null ? request.getServings() : BigDecimal.ONE;
                meal.setServings(servings);
                LocalDateTime consumedAt = request.getConsumedAt() != null ? request.getConsumedAt()
                                : LocalDateTime.now();
                meal.setConsumedAt(consumedAt);

                // 식단 기록 저장
                mealRepository.save(meal);

                // 일일 영양 섭취량 업데이트 로직 호출
                helperMethods.updateDayNutrition(user, recipe, servings, consumedAt);
        }

        // ============================================================================================
        // 사용자 신체 정보 업데이트
        // ============================================================================================
        @Transactional
        public void updatePhysicalMetrics(Long userId, PhysicalMetricsRequest request) {
                // 사용자 정보 조회
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));

                // 신체 정보 업데이트 적용
                helperMethods.updateUserPhysicalMetrics(user, request);

                // 변경된 사용자 정보 저장
                userRepository.save(user);
        }
}
