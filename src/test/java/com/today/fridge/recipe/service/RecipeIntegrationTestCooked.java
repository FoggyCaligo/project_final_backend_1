package com.today.fridge.recipe.service;

import com.today.fridge.ingredient.entity.UserIngredient;
import com.today.fridge.ingredient.repository.UserIngredientRepository;
import com.today.fridge.recipe.dto.intermediate.RecipeIngredientDTO;
import com.today.fridge.recipe.dto.response.RecipeResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * RecipeIntegrationTestCooked 클래스는 레시피 조리 전체 프로세스를 단계별로 검증하는 통합 테스트입니다.
 * @Nested를 사용하여 '재료 확인 및 계산' 단계와 '조리 및 차감' 단계를 분리하여 테스트합니다.
 */
@SpringBootTest
@ActiveProfiles("recipe")
@Transactional
public class RecipeIntegrationTestCooked {

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

    private Long userId;
    private Long recipeId;

    @BeforeEach
    void setUp() {
        // [공통 사전조건] 테스트를 위한 사용자, 냉장고 재료, 레시피 데이터를 준비합니다.
        User user = userRepository.save(User.create("cookuser", "cook@example.com", "password", "cooker"));
        userId = user.getUserId();

        // 냉장고 재료: 양파 200g, 마늘 50g
        UserIngredient ui1 = new UserIngredient();
        ui1.setUser(user);
        ui1.setRawName("양파");
        ui1.setNormalizedNameSnapshot("Onion");
        ui1.setQuantity(BigDecimal.valueOf(200));
        ui1.setUnit("g");
        ui1.setExpiresAt(LocalDate.now().plusDays(7));

        UserIngredient ui2 = new UserIngredient();
        ui2.setUser(user);
        ui2.setRawName("마늘");
        ui2.setNormalizedNameSnapshot("Garlic");
        ui2.setQuantity(BigDecimal.valueOf(50));
        ui2.setUnit("g");
        ui2.setExpiresAt(LocalDate.now().plusDays(14));

        userIngredientRepository.saveAll(List.of(ui1, ui2));

        // 레시피: 양파 볶음 (양파 150g, 마늘 10g 필요)
        Recipe recipe = recipeRepository.save(Recipe.builder().title("양파 볶음").isActive(true).build());
        recipeId = recipe.getRecipeId();

        recipeNutritionRepository.save(RecipeNutrition.builder()
                .recipe(recipe)
                .calories(BigDecimal.valueOf(100))
                .carbs(BigDecimal.valueOf(10))
                .protein(BigDecimal.valueOf(2))
                .fat(BigDecimal.valueOf(5))
                .build());

        recipeStepRepository.save(RecipeStep.builder().recipe(recipe).stepNo(1).instructionText("볶습니다.").build());

        recipeIngredientRepository.saveAll(List.of(
                RecipeIngredient.builder().recipe(recipe).rawText("양파").normalizedNameSnapshot("Onion")
                        .amountText("150g").build(),
                RecipeIngredient.builder().recipe(recipe).rawText("마늘").normalizedNameSnapshot("Garlic")
                        .amountText("10g").build()));
    }

    @Nested
    @DisplayName("단계 1: 재료 확인 및 충분 여부 계산")
    class Step1_CheckAndCalculate {

        @Test
        @DisplayName("1-1. 레시피 재료 매칭 및 소유 여부 확인")
        void checkIngredientMapping() {
            // 사전조건: 유저가 양파와 마늘을 냉장고에 소유하고 있음.
            // API 호출: recipeService.getRecipe(recipeId, userId)
            RecipeResponse response = recipeService.getRecipe(recipeId, userId);

            // 예상 결과: 레시피의 모든 재료(양파, 마늘)에 대해 owned가 true여야 함.
            // 실제 결과:
            List<RecipeIngredientDTO> ingredients = response.getRecipeIngredients();
            assertThat(ingredients).extracting(RecipeIngredientDTO::getOwned).containsOnly(true);
            assertThat(ingredients).extracting(RecipeIngredientDTO::getIngredientName)
                    .containsExactlyInAnyOrder("Onion", "Garlic");
        }

        @Test
        @DisplayName("1-2. 재료 수량 계산 및 충분(OK) 상태 확인")
        void verifyQuantityAndSufficiency() {
            // 사전조건: 양파(보유 200g, 필요 150g), 마늘(보유 50g, 필요 10g).
            // API 호출: recipeService.getRecipe(recipeId, userId)
            RecipeResponse response = recipeService.getRecipe(recipeId, userId);

            // 예상 결과: 양파와 마늘 모두 sufficiency가 "OK"여야 하며, 계산된 수량이 정확해야 함.
            // 실제 결과:
            RecipeIngredientDTO onion = response.getRecipeIngredients().stream()
                    .filter(ri -> ri.getIngredientName().equals("Onion")).findFirst().orElseThrow();
            RecipeIngredientDTO garlic = response.getRecipeIngredients().stream()
                    .filter(ri -> ri.getIngredientName().equals("Garlic")).findFirst().orElseThrow();

            assertThat(onion.getSufficiency()).isEqualTo("OK");
            assertThat(onion.getUserQuantity()).isEqualByComparingTo(BigDecimal.valueOf(200));
            assertThat(onion.getRequiredQuantity()).isEqualByComparingTo(BigDecimal.valueOf(150));

            assertThat(garlic.getSufficiency()).isEqualTo("OK");
            assertThat(garlic.getUserQuantity()).isEqualByComparingTo(BigDecimal.valueOf(50));
            assertThat(garlic.getRequiredQuantity()).isEqualByComparingTo(BigDecimal.valueOf(10));
        }
    }

    @Nested
    @DisplayName("단계 2: 조리 완료 및 냉장고 재료 차감")
    class Step2_CookAndReduce {

        @Test
        @DisplayName("2-1. 조리 완료 시 냉장고 재료가 정확히 차감되는지 확인")
        void reduceIngredientsAfterCooking() {
            // 사전조건: 레시피 조리 전 양파 200g, 마늘 50g이 냉장고에 있음.
            // API 호출: recipeService.ateRecipe(recipeId, userId)
            recipeService.ateRecipe(recipeId, userId);

            // 예상 결과: 양파는 50g(200-150), 마늘은 40g(50-10)이 남아야 함.
            // 실제 결과:
            List<UserIngredient> remaining = userIngredientRepository.findByUserIdAndIngredientNameIn(userId,
                    List.of("Onion", "Garlic"));

            UserIngredient remainingOnion = remaining.stream()
                    .filter(ui -> ui.getNormalizedNameSnapshot().equals("Onion")).findFirst().orElseThrow();
            UserIngredient remainingGarlic = remaining.stream()
                    .filter(ui -> ui.getNormalizedNameSnapshot().equals("Garlic")).findFirst().orElseThrow();

            assertThat(remainingOnion.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(50));
            assertThat(remainingGarlic.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(40));
        }

        @Test
        @DisplayName("2-2. 재료를 모두 소진했을 때 데이터가 삭제되는지 확인")
        void deleteIngredientWhenFullyConsumed() {
            // 사전조건: 소금 5g을 추가하고, 레시피에서 소금 5g을 요구하도록 설정.
            UserIngredient salt = new UserIngredient();
            salt.setUser(userRepository.getReferenceById(userId));
            salt.setNormalizedNameSnapshot("Salt");
            salt.setQuantity(BigDecimal.valueOf(5));
            salt.setUnit("g");
            userIngredientRepository.save(salt);

            recipeIngredientRepository.save(RecipeIngredient.builder()
                    .recipe(recipeRepository.getReferenceById(recipeId))
                    .normalizedNameSnapshot("Salt")
                    .amountText("5g")
                    .build());

            // API 호출: recipeService.ateRecipe(recipeId, userId)
            recipeService.ateRecipe(recipeId, userId);

            // 예상 결과: 소금(Salt) 레코드는 수량이 0이 되어 삭제되어야 함.
            // 실제 결과:
            List<UserIngredient> remainingSalt = userIngredientRepository.findByUserIdAndIngredientNameIn(userId,
                    List.of("Salt"));
            assertThat(remainingSalt).isEmpty();
        }
    }
}
