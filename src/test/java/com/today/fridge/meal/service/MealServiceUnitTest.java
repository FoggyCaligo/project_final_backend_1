package com.today.fridge.meal.service;

import com.today.fridge.meal.dto.request.MealLogRequest;
import com.today.fridge.meal.dto.request.PhysicalMetricsRequest;
import com.today.fridge.meal.entity.Meal;
import com.today.fridge.meal.repository.DayNutritionRepository;
import com.today.fridge.meal.repository.MealRepository;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * MealService (Writes)에 대한 단위 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("recipe")
@DisplayName("식단 서비스 단위 테스트 (저장 작업)")
public class MealServiceUnitTest {

    @InjectMocks
    private MealService mealService;

    @Mock
    private MealRepository mealRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private DayNutritionRepository dayNutritionRepository;

    @Mock
    private MealServiceHelperMethods helperMethods;

    // ============================================================================================
    // recordMeal 단위 테스트
    // ============================================================================================
    @Test
    @DisplayName("식단 기록 정상 저장 테스트")
    void testRecordMealSuccess() {
        // given (1. 요청 데이터 및 Mock 설정)
        MealLogRequest request = new MealLogRequest();
        ReflectionTestUtils.setField(request, "userId", 1L);
        ReflectionTestUtils.setField(request, "recipeId", 10L);
        ReflectionTestUtils.setField(request, "servings", new BigDecimal("1.5"));
        LocalDateTime consumedAt = LocalDateTime.now();
        ReflectionTestUtils.setField(request, "consumedAt", consumedAt);

        User user = User.create("unittest", "unittest@todayfridge.com", "hashedpassword", "유닛테스터");
        ReflectionTestUtils.setField(user, "userId", 1L);

        Recipe recipe = Recipe.builder().recipeId(10L).build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(recipeRepository.findById(10L)).willReturn(Optional.of(recipe));

        // when (2. 서비스 로직 호출)
        mealService.recordMeal(1L, request);

        // then (3. 엔티티 저장 및 헬퍼 메서드 호출 검증)
        ArgumentCaptor<Meal> mealCaptor = ArgumentCaptor.forClass(Meal.class);
        then(mealRepository).should(times(1)).save(mealCaptor.capture());

        Meal savedMeal = mealCaptor.getValue();
        assertThat(savedMeal.getUser().getUserId()).isEqualTo(1L);
        assertThat(savedMeal.getRecipe().getRecipeId()).isEqualTo(10L);
        assertThat(savedMeal.getServings()).isEqualByComparingTo("1.5");
        assertThat(savedMeal.getConsumedAt()).isEqualTo(consumedAt);

        then(helperMethods).should(times(1)).updateDayNutrition(user, recipe, new BigDecimal("1.5"), consumedAt);
    }

    // ============================================================================================
    // updatePhysicalMetrics 단위 테스트
    // ============================================================================================
    @Test
    @DisplayName("신체 정보 정상 업데이트 테스트")
    void testUpdatePhysicalMetrics() {
        // given (1. 사용자 및 요청 객체 생성)
        Long userId = 1L;
        PhysicalMetricsRequest request = new PhysicalMetricsRequest();
        ReflectionTestUtils.setField(request, "heightCm", 180.0);
        ReflectionTestUtils.setField(request, "weightKg", 75.0);
        ReflectionTestUtils.setField(request, "age", 25);
        ReflectionTestUtils.setField(request, "gender", "MALE");

        User user = User.create("unittest2", "unittest2@todayfridge.com", "hashedpassword", "유닛테스터2");
        ReflectionTestUtils.setField(user, "userId", userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when (2. 로직 호출)
        mealService.updatePhysicalMetrics(userId, request);

        // then (3. 값 변경 및 저장 검증)
        then(helperMethods).should(times(1)).updateUserPhysicalMetrics(user, request);

        then(userRepository).should(times(1)).save(user);
    }
}
