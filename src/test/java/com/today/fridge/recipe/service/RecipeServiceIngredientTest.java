package com.today.fridge.recipe.service;

/*
 * RecipeServiceIngredientTest는 RecipeService의 재료 매칭 및 수량 계산 로직을 테스트하기 위한 클래스입니다.
 * * 주요 테스트 시나리오:
 * 1. 유저의 냉장고 재료와 레시피 재료 간의 매칭 확인 (OK, NOT_ENOUGH, MISSING)
 * 2. 특수 단위(컵, 모, 약간)에 대한 수량 추출 및 변환 로직 검증
 * 3. 단위 변환(kg -> g, L -> mL)이 정확하게 이루어지는지 확인
 */

import com.today.fridge.ingredient.entity.IngredientMaster;
import com.today.fridge.ingredient.entity.UserIngredient;
import com.today.fridge.ingredient.repository.IngredientMasterRepository;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("recipe")
@Transactional
class RecipeServiceIngredientTest {

        @Autowired
        private RecipeService recipeService;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private IngredientMasterRepository ingredientMasterRepository;

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
        // 필수 필드를 채워주는 Helper Method
        // ========================================================================
        private Recipe createDummyRecipe(String title) {
                return Recipe.builder()
                                .title(title)
                                .sourceSite("test.com")
                                .sourceRecipeKey("key_" + UUID.randomUUID()) // Unique Key
                                .thumbnailUrl("test.jpg")
                                .summary("Test Summary")
                                .servingsText("1인분")
                                .cookTimeText("10분")
                                .sourceUrl("test.com/recipe")
                                .isActive(true)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
        }

        // ========================================================================
        // 레시피 1개 조회 - 회원 전용
        // ========================================================================
        @Test
        @DisplayName("UT-RECIPE-09 - 레시피 재료 매칭 및 수량 계산")
        void testGetRecipeWithFridge() {
                // 1. 사용자 추가
                User user = User.create("testuser1", "test1@example.com", "password", "tester1");
                User savedUser = userRepository.save(user);
                Long userId = savedUser.getUserId();

                // 2. Ingredient Master에 재료 추가
                IngredientMaster imGreenOnion = IngredientMaster.builder()
                                .canonicalName("다진 대파")
                                .normalizedName("파")
                                .isActive(true)
                                .build();
                IngredientMaster imGarlic = IngredientMaster.builder()
                                .canonicalName("다진 마늘")
                                .normalizedName("마늘")
                                .isActive(true)
                                .build();
                IngredientMaster imBeef = IngredientMaster.builder()
                                .canonicalName("소고기")
                                .normalizedName("소고기")
                                .isActive(true)
                                .build();

                ingredientMasterRepository.saveAll(List.of(imGreenOnion, imGarlic, imBeef));

                // 3. 사용자의 냉장고에 재료 추가
                UserIngredient uiGreenOnion = new UserIngredient();
                uiGreenOnion.setUser(savedUser);
                uiGreenOnion.setIngredientMaster(imGreenOnion);
                uiGreenOnion.setRawName("다진 대파");
                uiGreenOnion.setNormalizedNameSnapshot("파");
                uiGreenOnion.setQuantity(BigDecimal.valueOf(20));
                uiGreenOnion.setExpiresAt(LocalDate.now().plusDays(7));

                UserIngredient uiGarlic = new UserIngredient();
                uiGarlic.setUser(savedUser);
                uiGarlic.setIngredientMaster(imGarlic);
                uiGarlic.setRawName("다진 마늘");
                uiGarlic.setNormalizedNameSnapshot("마늘");
                uiGarlic.setQuantity(BigDecimal.valueOf(2));
                uiGarlic.setExpiresAt(LocalDate.now().plusDays(3));

                userIngredientRepository.saveAll(List.of(uiGreenOnion, uiGarlic));

                // 4. 레시피 추가 (Helper 사용)
                Recipe recipe = createDummyRecipe("Great Steak with Scallion");
                Recipe savedRecipe = recipeRepository.save(recipe);

                RecipeNutrition nutrition = RecipeNutrition.builder()
                                .recipe(savedRecipe)
                                .calories(BigDecimal.valueOf(500))
                                .carbs(BigDecimal.valueOf(50))
                                .protein(BigDecimal.valueOf(40))
                                .fat(BigDecimal.valueOf(20))
                                .build();
                recipeNutritionRepository.save(nutrition);

                RecipeStep step = RecipeStep.builder()
                                .recipe(savedRecipe)
                                .stepNo(1)
                                .instructionText("Season the beef and grill it with scallions and garlic.")
                                .build();
                recipeStepRepository.save(step);

                RecipeIngredient riOnion = RecipeIngredient.builder()
                                .recipe(savedRecipe)
                                .rawText("다진 대파")
                                .normalizedNameSnapshot("파")
                                .amountText("10g")
                                .isOptional(false)
                                .build();
                RecipeIngredient riGarlic = RecipeIngredient.builder()
                                .recipe(savedRecipe)
                                .rawText("다진 마늘")
                                .normalizedNameSnapshot("마늘")
                                .amountText("5g")
                                .isOptional(false)
                                .build();
                RecipeIngredient riBeef = RecipeIngredient.builder()
                                .recipe(savedRecipe)
                                .rawText("소고기")
                                .normalizedNameSnapshot("소고기")
                                .amountText("200g")
                                .isOptional(false)
                                .build();
                recipeIngredientRepository.saveAll(List.of(riOnion, riGarlic, riBeef));

                // 5. 서비스 메서드 실행
                RecipeResponse response = recipeService.getRecipe(savedRecipe.getRecipeId(), userId);

                // 6. 결과 검증
                assertThat(response.getRecipeId()).isEqualTo(savedRecipe.getRecipeId());
                assertThat(response.getRecipeIngredients()).hasSize(3);

                RecipeIngredientDTO onionDTO = findIngredient(response, "파");
                assertThat(onionDTO.getOwned()).isTrue();
                assertThat(onionDTO.getSufficiency()).isEqualTo("OK");

                RecipeIngredientDTO garlicDTO = findIngredient(response, "마늘");
                assertThat(garlicDTO.getOwned()).isTrue();
                assertThat(garlicDTO.getSufficiency()).isEqualTo("NOT_ENOUGH");

                RecipeIngredientDTO beefDTO = findIngredient(response, "소고기");
                assertThat(beefDTO.getOwned()).isFalse();
                assertThat(beefDTO.getSufficiency()).isEqualTo("MISSING");
        }

        // ============================================================================================
        // 특수 단위(컵, 모, 약간) 검증 테스트
        // ============================================================================================
        @Test
        @DisplayName("UT-RECIPE-10 - 특수 단위 처리")
        void testGetRecipeWithSpecialUnits() {
                User user = User.create("testuser2", "test2@example.com", "password", "tester2");
                User savedUser = userRepository.save(user);
                Long userId = savedUser.getUserId();

                IngredientMaster imTofu = IngredientMaster.builder().canonicalName("두부").normalizedName("두부")
                                .isActive(true).build();
                IngredientMaster imWater = IngredientMaster.builder().canonicalName("물").normalizedName("물")
                                .isActive(true).build();
                IngredientMaster imSalt = IngredientMaster.builder().canonicalName("소금").normalizedName("소금")
                                .isActive(true).build();
                ingredientMasterRepository.saveAll(List.of(imTofu, imWater, imSalt));

                UserIngredient uiTofu = new UserIngredient();
                uiTofu.setUser(savedUser);
                uiTofu.setIngredientMaster(imTofu);
                uiTofu.setNormalizedNameSnapshot("두부");
                uiTofu.setQuantity(BigDecimal.valueOf(200));

                UserIngredient uiWater = new UserIngredient();
                uiWater.setUser(savedUser);
                uiWater.setIngredientMaster(imWater);
                uiWater.setNormalizedNameSnapshot("물");
                uiWater.setQuantity(BigDecimal.valueOf(200));

                UserIngredient uiSalt = new UserIngredient();
                uiSalt.setUser(savedUser);
                uiSalt.setIngredientMaster(imSalt);
                uiSalt.setNormalizedNameSnapshot("소금");
                uiSalt.setQuantity(BigDecimal.valueOf(1));

                userIngredientRepository.saveAll(List.of(uiTofu, uiWater, uiSalt));

                // 레시피 추가 (Helper 사용)
                Recipe recipe = createDummyRecipe("Tofu Soup");
                Recipe savedRecipe = recipeRepository.saveAndFlush(recipe);

                RecipeNutrition nutrition = RecipeNutrition.builder()
                                .recipe(savedRecipe)
                                .calories(BigDecimal.valueOf(100))
                                .carbs(BigDecimal.valueOf(10))
                                .protein(BigDecimal.valueOf(10))
                                .fat(BigDecimal.valueOf(10))
                                .build();
                recipeNutritionRepository.saveAndFlush(nutrition);

                RecipeStep step = RecipeStep.builder()
                                .recipe(savedRecipe)
                                .stepNo(1)
                                .instructionText("두부와 소금을 물에 넣고 끓인다.")
                                .build();
                recipeStepRepository.saveAndFlush(step);

                RecipeIngredient riTofu = RecipeIngredient.builder().recipe(savedRecipe).normalizedNameSnapshot("두부")
                                .amountText("1 모").isOptional(false).build();
                RecipeIngredient riWater = RecipeIngredient.builder().recipe(savedRecipe).normalizedNameSnapshot("물")
                                .amountText("1 컵").isOptional(false).build();
                RecipeIngredient riSalt = RecipeIngredient.builder().recipe(savedRecipe).normalizedNameSnapshot("소금")
                                .amountText("약간").isOptional(false).build();
                recipeIngredientRepository.saveAll(List.of(riTofu, riWater, riSalt));

                RecipeResponse response = recipeService.getRecipe(savedRecipe.getRecipeId(), userId);

                RecipeIngredientDTO tofuDTO = findIngredient(response, "두부");
                assertThat(tofuDTO.getSufficiency()).isEqualTo("NOT_ENOUGH"); // 200 < 300

                RecipeIngredientDTO waterDTO = findIngredient(response, "물");
                assertThat(waterDTO.getSufficiency()).isEqualTo("OK"); // 200 >= 100

                RecipeIngredientDTO saltDTO = findIngredient(response, "소금");
                assertThat(saltDTO.getSufficiency()).isEqualTo("OK"); // 1 > 0
        }

        // ============================================================================================
        // 혼합 분수(1 1/2) 파싱 테스트
        // ============================================================================================
        @Test
        @DisplayName("UT-RECIPE-11 - 혼합 분수 처리")
        void testExtractNumericAmountWithMixedNumbers() {
                User user = User.create("testuser4", "test4@example.com", "password", "tester4");
                User savedUser = userRepository.save(user);
                Long userId = savedUser.getUserId();

                IngredientMaster imSugar = IngredientMaster.builder().canonicalName("설탕").normalizedName("설탕")
                                .isActive(true).build();
                ingredientMasterRepository.save(imSugar);

                UserIngredient uiSugar = new UserIngredient();
                uiSugar.setUser(savedUser);
                uiSugar.setNormalizedNameSnapshot("설탕");
                uiSugar.setQuantity(BigDecimal.valueOf(100));
                uiSugar.setUnit("g");
                userIngredientRepository.save(uiSugar);

                // 레시피 추가 (Helper 사용)
                Recipe recipe = createDummyRecipe("Sweet Test");
                recipeRepository.save(recipe);

                recipeNutritionRepository.save(RecipeNutrition.builder().recipe(recipe).calories(BigDecimal.ZERO)
                                .carbs(BigDecimal.ZERO).protein(BigDecimal.ZERO).fat(BigDecimal.ZERO).build());
                recipeStepRepository.save(
                                RecipeStep.builder().recipe(recipe).stepNo(1).instructionText("Add sugar").build());
                recipeIngredientRepository.save(RecipeIngredient.builder().recipe(recipe).normalizedNameSnapshot("설탕")
                                .amountText("1 1/2 컵").isOptional(false).build());

                RecipeResponse response = recipeService.getRecipe(recipe.getRecipeId(), userId);

                RecipeIngredientDTO sugarDTO = findIngredient(response, "설탕");
                assertThat(sugarDTO.getSufficiency()).isEqualTo("NOT_ENOUGH");
                assertThat(sugarDTO.getRequiredQuantity()).isEqualByComparingTo(BigDecimal.valueOf(150));
        }

        // ============================================================================================
        // 단위 불일치 통합 테스트 (레시피 kg vs 유저 g)
        // ============================================================================================
        @Test
        @DisplayName("UT-RECIPE-12 - 단위 불일치")
        void testGetRecipe_UnitMismatch_RecipeKgUserG() {
                User user = userRepository.save(User.create("testuser5", "test5@example.com", "password", "tester5"));
                Long userId = user.getUserId();

                UserIngredient ui = new UserIngredient();
                ui.setUser(user);
                ui.setNormalizedNameSnapshot("밀가루");
                ui.setQuantity(BigDecimal.valueOf(800));
                ui.setUnit("g");
                userIngredientRepository.save(ui);

                // 레시피 추가 (Helper 사용)
                Recipe recipe = recipeRepository.save(createDummyRecipe("Flour Test"));

                recipeNutritionRepository.save(RecipeNutrition.builder().recipe(recipe).calories(BigDecimal.ZERO)
                                .carbs(BigDecimal.ZERO).protein(BigDecimal.ZERO).fat(BigDecimal.ZERO).build());
                recipeStepRepository.save(
                                RecipeStep.builder().recipe(recipe).stepNo(1).instructionText("Use flour").build());
                recipeIngredientRepository.save(RecipeIngredient.builder().recipe(recipe).normalizedNameSnapshot("밀가루")
                                .amountText("1kg").isOptional(false).build());

                RecipeResponse response = recipeService.getRecipe(recipe.getRecipeId(), userId);

                RecipeIngredientDTO dto = findIngredient(response, "밀가루");
                assertThat(dto.getSufficiency()).isEqualTo("NOT_ENOUGH");
        }

        // ============================================================================================
        // 단위 변환 테스트 (kg->g, L->mL)
        // ============================================================================================
        @Test
        @DisplayName("UT-RECIPE-13 - 단위 변환")
        void testExtractNumericAmountWithUnitConversion() {
                User user = User.create("testuser3", "test3@example.com", "password", "tester3");
                User savedUser = userRepository.save(user);
                Long userId = savedUser.getUserId();

                IngredientMaster imFlour = IngredientMaster.builder().canonicalName("밀가루").normalizedName("밀가루")
                                .isActive(true).build();
                IngredientMaster imMilk = IngredientMaster.builder().canonicalName("우유").normalizedName("우유")
                                .isActive(true).build();
                IngredientMaster imWater = IngredientMaster.builder().canonicalName("물").normalizedName("물")
                                .isActive(true).build();
                ingredientMasterRepository.saveAll(List.of(imFlour, imMilk, imWater));

                UserIngredient uiFlour = new UserIngredient();
                uiFlour.setUser(savedUser);
                uiFlour.setIngredientMaster(imFlour);
                uiFlour.setNormalizedNameSnapshot("밀가루");
                uiFlour.setQuantity(BigDecimal.valueOf(600));

                UserIngredient uiMilk = new UserIngredient();
                uiMilk.setUser(savedUser);
                uiMilk.setIngredientMaster(imMilk);
                uiMilk.setNormalizedNameSnapshot("우유");
                uiMilk.setQuantity(BigDecimal.valueOf(1000));

                UserIngredient uiWater = new UserIngredient();
                uiWater.setUser(savedUser);
                uiWater.setIngredientMaster(imWater);
                uiWater.setNormalizedNameSnapshot("물");
                uiWater.setQuantity(BigDecimal.valueOf(500));

                userIngredientRepository.saveAll(List.of(uiFlour, uiMilk, uiWater));

                // 레시피 추가 (Helper 사용)
                Recipe recipe = createDummyRecipe("Baking Test");
                Recipe savedRecipe = recipeRepository.saveAndFlush(recipe);

                RecipeNutrition nutrition = RecipeNutrition.builder()
                                .recipe(savedRecipe)
                                .calories(BigDecimal.valueOf(200))
                                .carbs(BigDecimal.valueOf(20))
                                .protein(BigDecimal.valueOf(10))
                                .fat(BigDecimal.valueOf(5))
                                .build();
                recipeNutritionRepository.saveAndFlush(nutrition);

                RecipeStep step = RecipeStep.builder()
                                .recipe(savedRecipe)
                                .stepNo(1)
                                .instructionText("Mix flour, milk and water.")
                                .build();
                recipeStepRepository.saveAndFlush(step);

                RecipeIngredient riFlour = RecipeIngredient.builder().recipe(savedRecipe).normalizedNameSnapshot("밀가루")
                                .amountText("1/2kg").isOptional(false).build();
                RecipeIngredient riMilk = RecipeIngredient.builder().recipe(savedRecipe).normalizedNameSnapshot("우유")
                                .amountText("1.5L").isOptional(false).build();
                RecipeIngredient riWater = RecipeIngredient.builder().recipe(savedRecipe).normalizedNameSnapshot("물")
                                .amountText("500mL").isOptional(false).build();
                recipeIngredientRepository.saveAll(List.of(riFlour, riMilk, riWater));

                RecipeResponse response = recipeService.getRecipe(savedRecipe.getRecipeId(), userId);

                RecipeIngredientDTO flourDTO = findIngredient(response, "밀가루");
                assertThat(flourDTO.getSufficiency()).isEqualTo("OK"); // 600 >= 500

                RecipeIngredientDTO milkDTO = findIngredient(response, "우유");
                assertThat(milkDTO.getSufficiency()).isEqualTo("NOT_ENOUGH"); // 1000 < 1500

                RecipeIngredientDTO waterDTO = findIngredient(response, "물");
                assertThat(waterDTO.getSufficiency()).isEqualTo("OK"); // 500 >= 500
        }

        private RecipeIngredientDTO findIngredient(RecipeResponse response, String name) {
                return response.getRecipeIngredients().stream()
                                .filter(i -> name.equals(i.getNormalizedNameSnapshot()))
                                .findFirst()
                                .orElseThrow(() -> new AssertionError("Ingredient not found: " + name));
        }
}
