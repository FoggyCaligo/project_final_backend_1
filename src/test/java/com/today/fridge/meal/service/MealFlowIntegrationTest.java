package com.today.fridge.meal.service;

import com.today.fridge.meal.dto.request.MealLogRequest;
import com.today.fridge.meal.dto.response.DailyRecommendationResponse;
import com.today.fridge.meal.dto.response.ReportSummaryResponse;
import com.today.fridge.meal.entity.DayNutrition;
import com.today.fridge.meal.repository.DayNutritionRepository;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeNutrition;
import com.today.fridge.recipe.repository.RecipeNutritionRepository;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import com.today.fridge.recipe.entity.RecipeIngredient;
import com.today.fridge.recipe.entity.RecipeStep;
import com.today.fridge.recipe.repository.RecipeIngredientRepository;
import com.today.fridge.recipe.repository.RecipeStepRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Meal 서비스들의 전체적인 데이터 흐름을 검증하는 통합 테스트입니다.
 */
@SpringBootTest
@ActiveProfiles("recipe")
@Transactional
@DisplayName("식단 도메인 E2E 통합 테스트 (로컬 데이터베이스)")
public class MealFlowIntegrationTest {

        @Autowired
        private MealService mealService;

        @Autowired
        private MealServiceDaily mealServiceDaily;

        @Autowired
        private MealServicePeriod mealServicePeriod;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RecipeRepository recipeRepository;

        @Autowired
        private RecipeNutritionRepository recipeNutritionRepository;

        @Autowired
        private DayNutritionRepository dayNutritionRepository;

        @Autowired
        private RecipeIngredientRepository recipeIngredientRepository;

        @Autowired
        private RecipeStepRepository recipeStepRepository;

        // ========================================================================
        // 통합 테스트: 식단 기록부터 일일 권장량 조회 및 통계 리포트 생성까지 연동 확인
        // ========================================================================
        @Test
        @DisplayName("E2E 식단 추적 흐름 통합 테스트")
        void testMealEndToEndFlow() {
                // given (1. 유저 데이터 준비)
                User user = User.create("tester_integration", "integration@todayfridge.com", "hashedpassword", "통합테스터");
                user.setHeightCm(170.0);
                user.setWeightKg(65.0);
                user.setAge(28);
                user.setGender("FEMALE");
                User savedUser = userRepository.save(user);
                Long userId = savedUser.getUserId();

                // given (2. 레시피 및 영양정보 데이터 준비)
                Recipe recipe = Recipe.builder()
                                .title("건강한 샐러드")
                                .servingsText("1인분")
                                .isActive(true)
                                .build();
                Recipe savedRecipe = recipeRepository.save(recipe);

                RecipeNutrition nutrition = RecipeNutrition.builder()
                                .recipe(savedRecipe)
                                .calories(new BigDecimal("300.0"))
                                .carbs(new BigDecimal("20.0"))
                                .protein(new BigDecimal("15.0"))
                                .fat(new BigDecimal("5.0"))
                                .sugar(new BigDecimal("2.0"))
                                .sodium(new BigDecimal("150.0"))
                                .cholesterol(new BigDecimal("10.0"))
                                .build();
                recipeNutritionRepository.save(nutrition);
                ReflectionTestUtils.setField(savedRecipe, "recipeNutrition", nutrition);
                recipeRepository.save(savedRecipe);

                RecipeIngredient ingredient = RecipeIngredient.builder()
                                .recipe(savedRecipe)
                                .rawText("양상추 100g")
                                .amountText("100")
                                .unit("g")
                                .isOptional(false)
                                .sortOrder(1)
                                .build();
                recipeIngredientRepository.save(ingredient);

                RecipeStep step = RecipeStep.builder()
                                .recipe(savedRecipe)
                                .stepNo(1)
                                .instructionText("양상추를 씻어서 먹기 좋게 찢습니다.")
                                .build();
                recipeStepRepository.save(step);

                // when (3. 식단 기록 저장 API 로직 실행 - Write)
                LocalDateTime consumedAt = LocalDateTime.now();
                MealLogRequest request = new MealLogRequest();
                ReflectionTestUtils.setField(request, "userId", userId);
                ReflectionTestUtils.setField(request, "recipeId", savedRecipe.getRecipeId());
                ReflectionTestUtils.setField(request, "servings", new BigDecimal("2.0")); // 2인분 섭취
                ReflectionTestUtils.setField(request, "consumedAt", consumedAt);

                mealService.recordMeal(userId, request);

                // then (4. DB 영양 누적 결과 검증 - HelperMethod 확인)
                Optional<DayNutrition> dnOpt = dayNutritionRepository.findByUserUserIdAndDate(userId,
                                consumedAt.toLocalDate().atStartOfDay());
                assertThat(dnOpt).isPresent();
                DayNutrition dn = dnOpt.get();
                // 300kcal * 2인분 = 600kcal 반영 확인
                assertThat(dn.getTotalCalories().setScale(1)).isEqualByComparingTo("600.0");
                assertThat(dn.getTotalProtein().setScale(1)).isEqualByComparingTo("30.0"); // 15.0 * 2

                // then (5. Daily Service 연동 검증: 오늘 남은 영양량 피드백 확인 - Daily Read)
                DailyRecommendationResponse dailyResp = mealServiceDaily.getDailyRecommendation(userId,
                                consumedAt.toLocalDate());
                assertThat(dailyResp.getCurrentCalories().setScale(1)).isEqualByComparingTo("600.0");
                assertThat(dailyResp.getAdvice()).isNotEmpty(); // 목표량 계산 및 피드백이 생성되었는지 확인

                // then (6. Period Service 연동 검증: 주간 리포트 생성 및 요약 확인 - Period Read)
                // 최근 4일간을 기간으로 설정 (실제 기록은 1일만 존재)
                LocalDate startDate = consumedAt.toLocalDate().minusDays(3);
                LocalDate endDate = consumedAt.toLocalDate();
                ReportSummaryResponse reportResp = mealServicePeriod.getReportSummary(userId, startDate, endDate);

                // 4일간의 리포트 중 하루만 식단이 기록되었으므로, 나머지 3일은 결측치 보정 로직이 적용됨
                assertThat(reportResp.getDailyData().size()).isEqualTo(4);
                assertThat(reportResp.getMissingDaysImputed()).isEqualTo(3);
        }
}
