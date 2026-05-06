package com.today.fridge.meal.service;

/*
 * MealServiceTest는 MealService의 통합 기능을 테스트하는 클래스입니다.
 *
 * @SpringBootTest를 사용하여 실제 데이터베이스(H2 또는 설정된 DB) 환경에서 테스트하며,
 * @Transactional을 사용하여 각 테스트 후 데이터가 안전하게 롤백되도록 합니다.
 *
 * 주요 테스트 시나리오:
 * 1. 식단 기록(recordMeal)이 실제 DB 트랜잭션 내에서 DayNutrition을 정상적으로 업데이트하는지 확인
 * 2. 식단 기록 후 권장량(getDailyRecommendation) 조회가 변경된 데이터를 반영하는지 확인
 */

import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.meal.dto.request.MealLogRequest;
import com.today.fridge.meal.dto.response.DailyRecommendationResponse;
import com.today.fridge.meal.entity.DayNutrition;
import com.today.fridge.meal.repository.DayNutritionRepository;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeNutrition;
import com.today.fridge.recipe.repository.RecipeNutritionRepository;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("recipe")
@Transactional
class MealServiceTest {

    @Autowired
    private MealService mealService;

    @Autowired
    private MealServiceDaily mealServiceDaily;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private RecipeNutritionRepository recipeNutritionRepository;

    @Autowired
    private DayNutritionRepository dayNutritionRepository;

    // ========================================================================
    // 통합 테스트: 식단 기록 및 영양소 반영 시나리오
    // ========================================================================
    @Test
    @DisplayName("식단 기록 및 일일 영양소 추적 통합 테스트")
    void testRecordMealAndNutritionTracking() {
        // 1. 유저 데이터 준비
        User user = User.create("testmeal", "testmeal@todayfridge.com", "hashedpassword", "식단테스터");
        User savedUser = userRepository.save(user);
        Long userId = savedUser.getUserId();

        // 2. 레시피 데이터 준비
        Recipe recipe = Recipe.builder()
                .sourceSite("MANUAL")
                .sourceRecipeKey("TEST_KEY_" + System.currentTimeMillis())
                .title("건강한 샐러드")
                .thumbnailUrl("http://example.com/image.jpg")
                .summary("간단하고 건강한 샐러드입니다.")
                .servingsText("1인분")
                .cookTimeText("10분")
                .sourceUrl("http://example.com/recipe")
                .isActive(true)
                .difficultyLevel("EASY")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Recipe savedRecipe = recipeRepository.save(recipe);
        Long recipeId = savedRecipe.getRecipeId();

        // 3. 레시피 영양정보 준비
        RecipeNutrition nutrition = RecipeNutrition.builder()
                .recipe(savedRecipe)
                .referenceWeight(new BigDecimal("200.0"))
                .calories(new BigDecimal("300.0"))
                .carbs(new BigDecimal("20.0"))
                .protein(new BigDecimal("15.0"))
                .fat(new BigDecimal("5.0"))
                .sugar(new BigDecimal("2.0"))
                .sodium(new BigDecimal("100.0"))
                .cholesterol(new BigDecimal("0.0"))
                .build();
        recipeNutritionRepository.save(nutrition);

        // 양방향 연관관계 설정을 위해 메모리 객체 업데이트 (중요: MealService 로직에서 영양 정보를 조회할 수 있도록 함)
        org.springframework.test.util.ReflectionTestUtils.setField(savedRecipe, "recipeNutrition", nutrition);

        // 4. 식단 기록 요청 (MealLogRequest) 생성
        MealLogRequest request = new MealLogRequest();
        ReflectionTestUtils.setField(request, "userId", userId);
        ReflectionTestUtils.setField(request, "recipeId", recipeId);
        ReflectionTestUtils.setField(request, "servings", new BigDecimal("2.0"));
        // 2인분
        // 섭취
        ReflectionTestUtils.setField(request, "consumedAt", LocalDateTime.now());

        // 5. 식단 기록 서비스 호출
        mealService.recordMeal(userId, request);

        // 6. DB에 반영된 DayNutrition 결과 검증
        LocalDate today = LocalDate.now();
        DayNutrition dayNutrition = dayNutritionRepository.findByUserUserIdAndDate(userId, today.atStartOfDay())
                .orElseThrow(() -> new AssertionError("DayNutrition이 생성되지 않았습니다."));

        // 1인분이 300kcal 이고 2인분을 먹었으므로 600kcal가 되어야 함
        assertThat(dayNutrition.getTotalCalories()).isEqualByComparingTo(new BigDecimal("600.0"));
        assertThat(dayNutrition.getTotalProtein()).isEqualByComparingTo(new BigDecimal("30.0"));

        // 7. 일일 권장량(Recommendation) 연동 검증
        DailyRecommendationResponse recommendation = mealServiceDaily.getDailyRecommendation(userId, today);
        assertThat(recommendation.getCurrentCalories()).isEqualByComparingTo(new BigDecimal("600.0"));
        assertThat(recommendation.getCurrentProtein()).isEqualByComparingTo(new BigDecimal("30.0"));
    }

    // ========================================================================
    // 통합 테스트: 존재하지 않는 레시피 기록 예외 시나리오
    // ========================================================================
    @Test
    @DisplayName("존재하지 않는 레시피 기록 시 예외 발생 테스트")
    void testRecordMeal_RecipeNotFound() {
        // 1. 유저 데이터 준비
        User user = User.create("failtest", "failtest@todayfridge.com", "hashedpassword", "실패테스터");
        User savedUser = userRepository.save(user);

        // 2. 잘못된 레시피 ID를 포함한 요청 생성
        MealLogRequest request = new MealLogRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "userId", savedUser.getUserId());
        org.springframework.test.util.ReflectionTestUtils.setField(request, "recipeId", 99999L);
        org.springframework.test.util.ReflectionTestUtils.setField(request, "consumedAt", LocalDateTime.now());

        // 3. 존재하지 않는 레시피이므로 ExceptionTemplate 예외가 발생해야 함
        assertThrows(ExceptionTemplate.class, () -> {
            mealService.recordMeal(savedUser.getUserId(), request);
        });
    }
}
