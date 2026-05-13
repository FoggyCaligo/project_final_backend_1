package com.today.fridge.recipe.service;

/*
 * RecipeServiceAteTest는 RecipeService의 ateRecipe 메서드를 테스트하기 위한 클래스입니다.
 * 
 * ateRecipe 메서드는 사용자가 레시피를 먹었을 때(조리 완료), 냉장고에서 해당 재료를 차감하는 기능을 수행합니다.
 * 주요 테스트 시나리오:
 * 1. 여러 개의 동일 재료 레코드가 있을 경우 유통기한 임박순으로 차감되는지 확인
 * 2. 재료가 부족할 경우 보유한 수량을 모두 소진(0으로 설정 및 삭제)하는지 확인
 * 3. 재료가 아예 없는 경우에도 에러 없이 정상 동작하는지 확인
 */

import com.today.fridge.ingredient.entity.UserIngredient;
import com.today.fridge.ingredient.repository.UserIngredientRepository;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeIngredient;
import com.today.fridge.recipe.entity.RecipeNutrition;
import com.today.fridge.recipe.entity.RecipeStep;
import com.today.fridge.recipe.repository.RecipeIngredientRepository;
import com.today.fridge.recipe.repository.RecipeNutritionRepository;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.recipe.repository.RecipeStepRepository;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecipeServiceAteTest {

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserIngredientRepository userIngredientRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private RecipeNutritionRepository recipeNutritionRepository;

    @Autowired
    private RecipeStepRepository recipeStepRepository;

    @Autowired
    private RecipeIngredientRepository recipeIngredientRepository;

    // ========================================================================
    // ateRecipe: 여러 개의 레코드가 있을 경우 - 유통기한 임박순 차감 테스트
    // ========================================================================
    @Test
    @DisplayName("ateRecipe: Sufficient ingredients across multiple records - Should prioritize expiry date")
    void ateRecipe_SufficientMultipleRecords() {
        // 1. 사용자 추가
        User user = userRepository.save(User.create("ateuser1", "ate1@example.com", "password", "ater1"));
        Long userId = user.getUserId();

        // 2. 사용자의 냉장고에 재료 추가
        // 양파: 레코드 1 (10g, 오늘 만료), 레코드 2 (20g, 다음 주 만료)
        // 레시피에서 15g 요구함.
        // 예상 결과: 레코드 1 삭제, 레코드 2의 수량이 15g가 됨 (20 - (15-10))
        UserIngredient ui1 = new UserIngredient();
        ui1.setUser(user);
        ui1.setRawName("Onion");
        ui1.setNormalizedNameSnapshot("Onion");
        ui1.setQuantity(BigDecimal.valueOf(10));
        ui1.setExpiresAt(LocalDate.now());

        UserIngredient ui2 = new UserIngredient();
        ui2.setUser(user);
        ui2.setRawName("Onion");
        ui2.setNormalizedNameSnapshot("Onion");
        ui2.setQuantity(BigDecimal.valueOf(20));
        ui2.setExpiresAt(LocalDate.now().plusDays(7));

        userIngredientRepository.saveAll(List.of(ui1, ui2));

        // 3. 레시피 및 재료 설정
        Recipe recipe = recipeRepository.save(Recipe.builder().title("Onion Soup").isActive(true).build());
        recipeNutritionRepository.save(RecipeNutrition.builder().recipe(recipe).calories(BigDecimal.ZERO)
                .carbs(BigDecimal.ZERO).protein(BigDecimal.ZERO).fat(BigDecimal.ZERO).build());
        recipeStepRepository.save(RecipeStep.builder().recipe(recipe).stepNo(1).instructionText("Cook").build());
        recipeIngredientRepository.save(RecipeIngredient.builder().recipe(recipe).rawText("Onion")
                .normalizedNameSnapshot("Onion").amountText("15g").build());

        // 4. 서비스 메서드 실행
        recipeService.ateRecipe(recipe.getRecipeId(), userId);

        // 5. 결과 검증
        List<UserIngredient> remaining = userIngredientRepository.findByUserIdAndIngredientNameIn(userId,
                List.of("Onion"));
        assertThat(remaining).hasSize(1); // ui1은 삭제되어야 함
        assertThat(remaining.get(0).getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(15));
        assertThat(remaining.get(0).getExpiresAt()).isEqualTo(LocalDate.now().plusDays(7));
    }

    // ========================================================================
    // ateRecipe: 재료가 부족할 경우 - 모두 소진 및 삭제 테스트
    // ========================================================================
    @Test
    @DisplayName("ateRecipe: Insufficient ingredients - Should consume everything and set to 0 (delete)")
    void ateRecipe_InsufficientIngredients() {
        // 1. 사용자 추가
        User user = userRepository.save(User.create("ateuser2", "ate2@example.com", "password", "ater2"));
        Long userId = user.getUserId();

        // 2. 사용자의 냉장고에 재료 추가
        // 마늘: 5g 소유
        // 레시피에서 10g 요구함.
        // 예상 결과: 마늘 레코드 삭제 (수량 0 이하)
        UserIngredient ui1 = new UserIngredient();
        ui1.setUser(user);
        ui1.setRawName("Garlic");
        ui1.setNormalizedNameSnapshot("Garlic");
        ui1.setQuantity(BigDecimal.valueOf(5));
        ui1.setExpiresAt(LocalDate.now());

        userIngredientRepository.save(ui1);

        // 3. 레시피 및 재료 설정
        Recipe recipe = recipeRepository.save(Recipe.builder().title("Garlic Bread").isActive(true).build());
        recipeNutritionRepository.save(RecipeNutrition.builder().recipe(recipe).calories(BigDecimal.ZERO)
                .carbs(BigDecimal.ZERO).protein(BigDecimal.ZERO).fat(BigDecimal.ZERO).build());
        recipeStepRepository.save(RecipeStep.builder().recipe(recipe).stepNo(1).instructionText("Cook").build());
        recipeIngredientRepository.save(RecipeIngredient.builder().recipe(recipe).rawText("Garlic")
                .normalizedNameSnapshot("Garlic").amountText("10g").build());

        // 4. 서비스 메서드 실행
        recipeService.ateRecipe(recipe.getRecipeId(), userId);

        // 5. 결과 검증
        List<UserIngredient> remaining = userIngredientRepository.findByUserIdAndIngredientNameIn(userId,
                List.of("Garlic"));
        assertThat(remaining).isEmpty();
    }

    // ========================================================================
    // ateRecipe: 재료가 아예 없는 경우 - 에러 없이 통과 확인 테스트
    // ========================================================================
    @Test
    @DisplayName("ateRecipe: Missing ingredients - Should not crash")
    void ateRecipe_MissingIngredients() {
        // 1. 사용자 추가
        User user = userRepository.save(User.create("ateuser3", "ate3@example.com", "password", "ater3"));
        Long userId = user.getUserId();

        // 2. 레시피 설정 (유저가 가지지 않은 재료 요구)
        Recipe recipe = recipeRepository.save(Recipe.builder().title("Empty Salad").isActive(true).build());
        recipeNutritionRepository.save(RecipeNutrition.builder().recipe(recipe).calories(BigDecimal.ZERO)
                .carbs(BigDecimal.ZERO).protein(BigDecimal.ZERO).fat(BigDecimal.ZERO).build());
        recipeStepRepository.save(RecipeStep.builder().recipe(recipe).stepNo(1).instructionText("Cook").build());
        recipeIngredientRepository.save(RecipeIngredient.builder().recipe(recipe).rawText("Water")
                .normalizedNameSnapshot("Water").amountText("1L").build());

        // 3. 서비스 메서드 실행 및 에러 미발생 검증
        recipeService.ateRecipe(recipe.getRecipeId(), userId);
    }

    // ========================================================================
    // ateRecipe: 단위 불일치 테스트 (1kg 소유, 250g 요구)
    // ========================================================================
    @Test
    @DisplayName("ateRecipe: Unit mismatch - Consuming 250g from 1kg should leave 0.75kg")
    void ateRecipe_UnitMismatch_KgToG() {
        // 1. 사용자 추가
        User user = userRepository.save(User.create("unituser1", "unit1@example.com", "password", "unitr1"));
        Long userId = user.getUserId();

        // 2. 사용자의 냉장고에 1kg의 소고기 추가
        UserIngredient ui = new UserIngredient();
        ui.setUser(user);
        ui.setRawName("Beef");
        ui.setNormalizedNameSnapshot("Beef");
        ui.setQuantity(BigDecimal.valueOf(1));
        ui.setUnit("kg");
        ui.setExpiresAt(LocalDate.now().plusDays(10));
        userIngredientRepository.save(ui);

        // 3. 레시피 설정 (250g의 소고기 요구)
        Recipe recipe = recipeRepository.save(Recipe.builder().title("Beef Stew").isActive(true).build());
        recipeNutritionRepository.save(RecipeNutrition.builder().recipe(recipe).calories(BigDecimal.ZERO)
                .carbs(BigDecimal.ZERO).protein(BigDecimal.ZERO).fat(BigDecimal.ZERO).build());
        recipeStepRepository.save(RecipeStep.builder().recipe(recipe).stepNo(1).instructionText("Cook").build());
        recipeIngredientRepository.save(RecipeIngredient.builder().recipe(recipe).rawText("Beef")
                .normalizedNameSnapshot("Beef").amountText("250g").build());

        // 4. 서비스 메서드 실행
        recipeService.ateRecipe(recipe.getRecipeId(), userId);

        // 5. 결과 검증: 1kg - 250g = 750g -> 0.75kg
        List<UserIngredient> remaining = userIngredientRepository.findByUserIdAndIngredientNameIn(userId, List.of("Beef"));
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(0.75));
        assertThat(remaining.get(0).getUnit()).isEqualTo("kg");
    }

    // ========================================================================
    // ateRecipe: 컵(Cup) 단위 테스트 (2컵 요구, 500ml 소유)
    // ========================================================================
    @Test
    @DisplayName("ateRecipe: Cup unit - Consuming 2 cups (200ml) from 500ml should leave 300ml")
    void ateRecipe_CupUnit() {
        // 1. 사용자 추가
        User user = userRepository.save(User.create("unituser2", "unit2@example.com", "password", "unitr2"));
        Long userId = user.getUserId();

        // 2. 사용자의 냉장고에 500ml의 물 추가
        UserIngredient ui = new UserIngredient();
        ui.setUser(user);
        ui.setRawName("Water");
        ui.setNormalizedNameSnapshot("Water");
        ui.setQuantity(BigDecimal.valueOf(500));
        ui.setUnit("ml");
        ui.setExpiresAt(LocalDate.now().plusDays(10));
        userIngredientRepository.save(ui);

        // 3. 레시피 설정 (2컵의 물 요구 -> 2 * 100 = 200ml)
        Recipe recipe = recipeRepository.save(Recipe.builder().title("Boiled Water").isActive(true).build());
        recipeNutritionRepository.save(RecipeNutrition.builder().recipe(recipe).calories(BigDecimal.ZERO)
                .carbs(BigDecimal.ZERO).protein(BigDecimal.ZERO).fat(BigDecimal.ZERO).build());
        recipeStepRepository.save(RecipeStep.builder().recipe(recipe).stepNo(1).instructionText("Boil").build());
        recipeIngredientRepository.save(RecipeIngredient.builder().recipe(recipe).rawText("Water")
                .normalizedNameSnapshot("Water").amountText("2컵").build());

        // 4. 서비스 메서드 실행
        recipeService.ateRecipe(recipe.getRecipeId(), userId);

        // 5. 결과 검증: 500ml - 200ml = 300ml
        List<UserIngredient> remaining = userIngredientRepository.findByUserIdAndIngredientNameIn(userId, List.of("Water"));
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(300));
    }
}
