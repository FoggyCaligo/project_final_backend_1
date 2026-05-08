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
import com.today.fridge.meal.dto.response.MealNutritionSummaryDTO;
import com.today.fridge.meal.entity.DayNutrition;
import com.today.fridge.meal.entity.Meal;
import com.today.fridge.meal.repository.DayNutritionRepository;
import com.today.fridge.meal.repository.MealRepository;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class MealService {

        private final MealRepository mealRepository;
        private final UserRepository userRepository;
        private final RecipeRepository recipeRepository;
        private final DayNutritionRepository dayNutritionRepository;
        private final MealServiceHelperMethods helperMethods;

        // ============================================================================================
        // 식단 기록 저장
        // ============================================================================================
        @Transactional
        public void recordMeal(Long userId, MealLogRequest request) {
                log.info("[MealService] recordMeal (public) - userId: {}, recipeId: {}", userId, request.getRecipeId());
                recordMeal(userId, request.getRecipeId(), request.getServings(), request.getConsumedAt());
        }

        @Transactional
        public void recordMeal(Long userId, Long recipeId, BigDecimal servings, LocalDateTime consumedAt) {
                log.info("[MealService] recordMeal (public) - userId: {}, recipeId: {}, servings: {}", userId, recipeId, servings);
                // 사용자 정보 조회
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));

                // 레시피 정보 조회
                Recipe recipe = recipeRepository.findById(recipeId)
                                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.RECIPE_NOT_FOUND));

                // 식단 엔티티 생성 및 설정
                Meal meal = new Meal();
                meal.setUser(user);
                meal.setRecipe(recipe);

                // 인분 수 및 섭취 시간 설정 (기본값 처리)
                BigDecimal finalServings = servings != null ? servings : BigDecimal.ONE;
                meal.setServings(finalServings);
                LocalDateTime finalConsumedAt = consumedAt != null ? consumedAt : LocalDateTime.now();
                meal.setConsumedAt(finalConsumedAt);

                // 식단 기록 저장
                mealRepository.save(meal);
                log.info("식단 저장 완료 - 사용자 ID: {}, 레시피: {}, 섭취량: {}", userId, recipe.getTitle(), finalServings);

                // 일일 영양 섭취량 업데이트 로직 호출
                helperMethods.updateDayNutrition(user, recipe, finalServings, finalConsumedAt);
        }

        // ============================================================================================
        // 사용자 신체 정보 업데이트
        // ============================================================================================
        @Transactional
        public void updatePhysicalMetrics(Long userId, PhysicalMetricsRequest request) {
                log.info("[MealService] updatePhysicalMetrics (public) - userId: {}", userId);
                // 사용자 정보 조회
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.USER_NOT_FOUND));

                // 신체 정보 업데이트 적용
                helperMethods.updateUserPhysicalMetrics(user, request);

                // 변경된 사용자 정보 저장
                userRepository.save(user);
                log.info("사용자 신체 정보 업데이트 완료 - 사용자 ID: {} (키: {}cm, 몸무게: {}kg, 나이: {}, 성별: {})", 
                        userId, user.getHeightCm(), user.getWeightKg(), user.getAge(), user.getGender());
        }

        // ============================================================================================
        // 식단 기록 삭제
        // ============================================================================================
        @Transactional
        public void deleteMeal(Long userId, Long mealId) {
                log.info("[MealService] deleteMeal (public) - userId: {}, mealId: {}", userId, mealId);
                // 식단 기록 조회
                Meal meal = mealRepository.findById(mealId)
                                .orElseThrow(() -> new ExceptionTemplate(ErrorCode.MEAL_NOT_FOUND));

                // 사용자 권한 확인
                if (!meal.getUser().getUserId().equals(userId)) {
                        throw new ExceptionTemplate(ErrorCode.ACCESS_DENIED);
                }

                LocalDate consumedDate = meal.getConsumedAt().toLocalDate();
                LocalDateTime startOfDay = consumedDate.atStartOfDay();
                LocalDateTime endOfDay = consumedDate.plusDays(1).atStartOfDay();

                // 식단 삭제
                mealRepository.delete(meal);
                log.info("식단 삭제 완료 - 사용자 ID: {}, 식단 ID: {}", userId, mealId);

                // 일일 영양 섭취량 재계산
                MealNutritionSummaryDTO summary = mealRepository.getNutritionSummaryByDateRange(userId, startOfDay, endOfDay);

                if (summary.getTotalCalories() == null || summary.getTotalCalories().compareTo(BigDecimal.ZERO) <= 0) {
                        // 칼로리가 0 이하이면 해당 날짜의 영양 기록 삭제
                        dayNutritionRepository.findByUserUserIdAndDate(userId, startOfDay)
                                        .ifPresent(dayNutritionRepository::delete);
                        log.info("일일 영양 합계 삭제 완료 (데이터 없음) - 사용자 ID: {}, 날짜: {}", userId, consumedDate);
                } else {
                        // 재계산된 값으로 영양 기록 갱신 (없으면 생성)
                        DayNutrition dayNutrition = dayNutritionRepository.findByUserUserIdAndDate(userId, startOfDay)
                                        .orElseGet(() -> {
                                                DayNutrition dn = new DayNutrition();
                                                dn.setUser(meal.getUser());
                                                dn.setDate(startOfDay);
                                                return dn;
                                        });

                        dayNutrition.setTotalCalories(summary.getTotalCalories());
                        dayNutrition.setTotalCarbs(summary.getTotalCarbs() != null ? summary.getTotalCarbs() : BigDecimal.ZERO);
                        dayNutrition.setTotalProtein(summary.getTotalProtein() != null ? summary.getTotalProtein() : BigDecimal.ZERO);
                        dayNutrition.setTotalFat(summary.getTotalFat() != null ? summary.getTotalFat() : BigDecimal.ZERO);
                        dayNutrition.setTotalSugar(summary.getTotalSugar() != null ? summary.getTotalSugar() : BigDecimal.ZERO);
                        dayNutrition.setTotalSodium(summary.getTotalSodium() != null ? summary.getTotalSodium() : BigDecimal.ZERO);
                        dayNutrition.setTotalCholesterol(summary.getTotalCholesterol() != null ? summary.getTotalCholesterol() : BigDecimal.ZERO);

                        dayNutritionRepository.save(dayNutrition);
                        log.info("일일 영양 합계 재계산 완료 - 사용자 ID: {}, 날짜: {}", userId, consumedDate);
                }
        }
}
