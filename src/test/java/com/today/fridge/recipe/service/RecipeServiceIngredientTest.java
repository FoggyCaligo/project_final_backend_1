package com.today.fridge.recipe.service;

/*
 * RecipeServiceIngredientTest는 RecipeService의 재료 매칭 및 수량 계산 로직을 테스트하기 위한 클래스입니다.
 * 
 * 주요 테스트 시나리오:
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
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
        // 레시피 1개 조회 - 회원 전용
        // ========================================================================
        @Test
        @DisplayName("Test getRecipe with User Fridge contents - Integration Test")
        void testGetRecipeWithFridge() {
                // 1. 사용자 추가
                User user = User.create("testuser1", "test1@example.com", "password", "tester1");
                User savedUser = userRepository.save(user);
                Long userId = savedUser.getUserId();
                System.out.println("\n[TEST SETUP] Created User ID: " + userId);

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
                System.out.println("[TEST SETUP] Created Ingredient Masters: 파, 마늘, 소고기");

                // 3. 사용자의 냉장고에 재료 추가
                // 충분한 파 (Need 10g, have 20g)
                UserIngredient uiGreenOnion = new UserIngredient();
                uiGreenOnion.setUser(savedUser);
                uiGreenOnion.setIngredientMaster(imGreenOnion);
                uiGreenOnion.setRawName("다진 대파");
                uiGreenOnion.setNormalizedNameSnapshot("파");
                uiGreenOnion.setQuantity(BigDecimal.valueOf(20));
                uiGreenOnion.setExpiresAt(LocalDate.now().plusDays(7));

                // 부족한 마늘 (Need 5g, have 2g)
                UserIngredient uiGarlic = new UserIngredient();
                uiGarlic.setUser(savedUser);
                uiGarlic.setIngredientMaster(imGarlic);
                uiGarlic.setRawName("다진 마늘");
                uiGarlic.setNormalizedNameSnapshot("마늘");
                uiGarlic.setQuantity(BigDecimal.valueOf(2));
                uiGarlic.setExpiresAt(LocalDate.now().plusDays(3));

                userIngredientRepository.saveAll(List.of(uiGreenOnion, uiGarlic));
                System.out.println("[TEST SETUP] Filled Fridge: 파 (20g), 마늘 (2g)");
                // 냉장고에 소고기가 없음 -> MISSING

                // 4. 레시피 추가
                Recipe recipe = Recipe.builder()
                                .title("Great Steak with Scallion")
                                .isActive(true)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
                Recipe savedRecipe = recipeRepository.save(recipe);

                // 레시피에 필요한 정보 반환
                // 레시피 영양성분
                RecipeNutrition nutrition = RecipeNutrition.builder()
                                .recipe(savedRecipe)
                                .calories(BigDecimal.valueOf(500))
                                .carbs(BigDecimal.valueOf(50))
                                .protein(BigDecimal.valueOf(40))
                                .fat(BigDecimal.valueOf(20))
                                .build();
                recipeNutritionRepository.save(nutrition);

                // 레시피 단계
                RecipeStep step = RecipeStep.builder()
                                .recipe(savedRecipe)
                                .stepNo(1)
                                .instructionText("Season the beef and grill it with scallions and garlic.")
                                .build();
                recipeStepRepository.save(step);

                // 레시피 재료 추가 (정규화된 이름으로 매칭)
                RecipeIngredient riOnion = RecipeIngredient.builder()
                                .recipe(savedRecipe)
                                .rawText("다진 대파")
                                .normalizedNameSnapshot("파")
                                .amountText("10g")
                                .build();
                RecipeIngredient riGarlic = RecipeIngredient.builder()
                                .recipe(savedRecipe)
                                .rawText("다진 마늘")
                                .normalizedNameSnapshot("마늘")
                                .amountText("5g")
                                .build();
                RecipeIngredient riBeef = RecipeIngredient.builder()
                                .recipe(savedRecipe)
                                .rawText("소고기")
                                .normalizedNameSnapshot("소고기")
                                .amountText("200g")
                                .build();
                recipeIngredientRepository.saveAll(List.of(riOnion, riGarlic, riBeef));

                // 5. 서비스 메서드 실행
                System.out.println("[TEST EXECUTION] Calling recipeService.getRecipe for recipeId: "
                                + savedRecipe.getRecipeId());
                RecipeResponse response = recipeService.getRecipe(savedRecipe.getRecipeId(), userId);

                // 6. 결과 검증
                System.out.println("[TEST RESULTS] Recipe: " + response.getTitle());
                response.getRecipeIngredients().forEach(ing -> {
                        System.out.printf(" - Ingredient: %-10s | Owned: %-5b | Sufficiency: %-10s | Amount: %s\n",
                                        ing.getIngredientName(), ing.getOwned(), ing.getSufficiency(),
                                        ing.getAmountText());
                });
                assertThat(response.getRecipeId()).isEqualTo(savedRecipe.getRecipeId());
                assertThat(response.getRecipeIngredients()).hasSize(3);

                // 파 (OK)
                RecipeIngredientDTO onionDTO = findIngredient(response, "파");
                assertThat(onionDTO.getOwned()).isTrue();
                assertThat(onionDTO.getSufficiency()).isEqualTo("OK");

                // 마늘 (NOT_ENOUGH)
                RecipeIngredientDTO garlicDTO = findIngredient(response, "마늘");
                assertThat(garlicDTO.getOwned()).isTrue();
                assertThat(garlicDTO.getSufficiency()).isEqualTo("NOT_ENOUGH");

                // 소고기 (MISSING)
                RecipeIngredientDTO beefDTO = findIngredient(response, "소고기");
                assertThat(beefDTO.getOwned()).isFalse();
                assertThat(beefDTO.getSufficiency()).isEqualTo("MISSING");
        }

        // ============================================================================================
        // Recipe/Service/RecipeService.java의 extractNumericAmount test
        // 1. 기본적으로 1컵 -> 100g, 1모 -> 300g으로 변경함.
        // 2. 단위가 g, L, mL, kg가 포함되어 있지 않는다면 기본적으로 0으로 반환함.
        // 예: 약간, 조금 등은 냉장고에 존재하기만 해도 1이상이라면 OK.
        // 3. 적절한 반올림을 통해 비교함.
        // ============================================================================================
        @Test
        @DisplayName("Test getRecipe with Special Units (컵, 모, 약간)")
        void testGetRecipeWithSpecialUnits() {
                User user = User.create("testuser2", "test2@example.com", "password", "tester2");
                User savedUser = userRepository.save(user);
                Long userId = savedUser.getUserId();
                System.out.println("\n[TEST SETUP] Created User ID: " + userId);

                // Ingredient Master에 추가함
                IngredientMaster imTofu = IngredientMaster.builder().canonicalName("두부").normalizedName("두부")
                                .isActive(true).build();
                IngredientMaster imWater = IngredientMaster.builder().canonicalName("물").normalizedName("물")
                                .isActive(true).build();
                IngredientMaster imSalt = IngredientMaster.builder().canonicalName("소금").normalizedName("소금")
                                .isActive(true).build();
                ingredientMasterRepository.saveAll(List.of(imTofu, imWater, imSalt));
                System.out.println("[TEST SETUP] Created Ingredient Masters: 두부, 물, 소금");

                // 냉장고에 추가함
                // 1. 두부: 200g (Required 1 모 = 300g) -> NOT_ENOUGH
                UserIngredient uiTofu = new UserIngredient();
                uiTofu.setUser(savedUser);
                uiTofu.setIngredientMaster(imTofu);
                uiTofu.setNormalizedNameSnapshot("두부");
                uiTofu.setQuantity(BigDecimal.valueOf(200));

                // 2. 물: 200mL (Required 1 컵 = 100mL) -> OK
                UserIngredient uiWater = new UserIngredient();
                uiWater.setUser(savedUser);
                uiWater.setIngredientMaster(imWater);
                uiWater.setNormalizedNameSnapshot("물");
                uiWater.setQuantity(BigDecimal.valueOf(200));

                // 3. 소금: 1g (Required 약간 = 0g) -> OK
                UserIngredient uiSalt = new UserIngredient();
                uiSalt.setUser(savedUser);
                uiSalt.setIngredientMaster(imSalt);
                uiSalt.setNormalizedNameSnapshot("소금");
                uiSalt.setQuantity(BigDecimal.valueOf(1));

                userIngredientRepository.saveAll(List.of(uiTofu, uiWater, uiSalt));
                System.out.println("[TEST SETUP] Filled Fridge: 두부 (200g), 물 (200mL), 소금 (1g)");

                // 레시피에 추가
                Recipe recipe = Recipe.builder().title("Tofu Soup").isActive(true).build();
                Recipe savedRecipe = recipeRepository.saveAndFlush(recipe);

                // 필요한 레시피 영양성분 추가
                RecipeNutrition nutrition = RecipeNutrition.builder()
                                .recipe(savedRecipe)
                                .calories(BigDecimal.valueOf(100))
                                .carbs(BigDecimal.valueOf(10))
                                .protein(BigDecimal.valueOf(10))
                                .fat(BigDecimal.valueOf(10))
                                .build();
                recipeNutritionRepository.saveAndFlush(nutrition);

                // 레시피에 필요한 단계 추가
                RecipeStep step = RecipeStep.builder()
                                .recipe(savedRecipe)
                                .stepNo(1)
                                .instructionText("두부와 소금을 물에 넣고 끓인다.")
                                .build();
                recipeStepRepository.saveAndFlush(step);

                // 레시피에 필요한 재료 추가
                RecipeIngredient riTofu = RecipeIngredient.builder().recipe(savedRecipe).normalizedNameSnapshot("두부")
                                .amountText("1 모").build();
                RecipeIngredient riWater = RecipeIngredient.builder().recipe(savedRecipe).normalizedNameSnapshot("물")
                                .amountText("1 컵").build();
                RecipeIngredient riSalt = RecipeIngredient.builder().recipe(savedRecipe).normalizedNameSnapshot("소금")
                                .amountText("약간").build();
                recipeIngredientRepository.saveAll(List.of(riTofu, riWater, riSalt));

                // 레시피 조회
                System.out.println("[TEST EXECUTION] Calling recipeService.getRecipe for recipeId: "
                                + savedRecipe.getRecipeId());
                RecipeResponse response = recipeService.getRecipe(savedRecipe.getRecipeId(), userId);

                // Verify
                System.out.println("[TEST RESULTS] Recipe: " + response.getTitle());
                response.getRecipeIngredients().forEach(ing -> {
                        System.out.printf(" - Ingredient: %-10s | Owned: %-5b | Sufficiency: %-10s | Amount: %s\n",
                                        ing.getIngredientName(), ing.getOwned(), ing.getSufficiency(),
                                        ing.getAmountText());
                });

                // 두부: 200g (Required 1 모 = 300g) -> NOT_ENOUGH
                RecipeIngredientDTO tofuDTO = findIngredient(response, "두부");
                assertThat(tofuDTO.getSufficiency()).isEqualTo("NOT_ENOUGH"); // 200 < 300

                // 물: 200mL (Required 1 컵 = 100mL) -> OK
                RecipeIngredientDTO waterDTO = findIngredient(response, "물");
                assertThat(waterDTO.getSufficiency()).isEqualTo("OK"); // 200 >= 100

                // 소금: 1g (Required "약간") -> OK
                RecipeIngredientDTO saltDTO = findIngredient(response, "소금");
                assertThat(saltDTO.getSufficiency()).isEqualTo("OK"); // 1 > 0
        }

        // ============================================================================================
        // 1 1/2 와 같은 믹스드 넘버(혼합 분수) 파싱 테스트
        // ============================================================================================
        @Test
        @DisplayName("Test extractNumericAmount with Mixed Numbers (1 1/2)")
        void testExtractNumericAmountWithMixedNumbers() {
                User user = User.create("testuser4", "test4@example.com", "password", "tester4");
                User savedUser = userRepository.save(user);
                Long userId = savedUser.getUserId();

                // Ingredient Master
                IngredientMaster imSugar = IngredientMaster.builder().canonicalName("설탕").normalizedName("설탕")
                                .isActive(true).build();
                ingredientMasterRepository.save(imSugar);

                // 냉장고에 100g 설탕 (Required 1 1/2 컵 = 150g) -> NOT_ENOUGH
                UserIngredient uiSugar = new UserIngredient();
                uiSugar.setUser(savedUser);
                uiSugar.setNormalizedNameSnapshot("설탕");
                uiSugar.setQuantity(BigDecimal.valueOf(100));
                uiSugar.setUnit("g");
                userIngredientRepository.save(uiSugar);

                // 레시피 추가
                Recipe recipe = Recipe.builder().title("Sweet Test").isActive(true).build();
                recipeRepository.save(recipe);
                recipeNutritionRepository.save(RecipeNutrition.builder().recipe(recipe).calories(BigDecimal.ZERO)
                                .carbs(BigDecimal.ZERO).protein(BigDecimal.ZERO).fat(BigDecimal.ZERO).build());
                recipeStepRepository.save(
                                RecipeStep.builder().recipe(recipe).stepNo(1).instructionText("Add sugar").build());

                recipeIngredientRepository.save(RecipeIngredient.builder().recipe(recipe).normalizedNameSnapshot("설탕")
                                .amountText("1 1/2 컵").build());

                // 조회
                RecipeResponse response = recipeService.getRecipe(recipe.getRecipeId(), userId);

                // 검증: 1 1/2 컵 = 1.5 * 100 = 150g. 유저가 100g 가졌으므로 NOT_ENOUGH
                RecipeIngredientDTO sugarDTO = findIngredient(response, "설탕");
                assertThat(sugarDTO.getSufficiency()).isEqualTo("NOT_ENOUGH");
                assertThat(sugarDTO.getRequiredQuantity()).isEqualByComparingTo(BigDecimal.valueOf(150));
        }

        // ============================================================================================
        // 단위 불일치 통합 테스트 (레시피 kg vs 유저 g)
        // ============================================================================================
        @Test
        @DisplayName("Test getRecipe with unit mismatch (Recipe kg vs User g)")
        void testGetRecipe_UnitMismatch_RecipeKgUserG() {
                User user = userRepository.save(User.create("testuser5", "test5@example.com", "password", "tester5"));
                Long userId = user.getUserId();

                // 유저: 밀가루 800g
                UserIngredient ui = new UserIngredient();
                ui.setUser(user);
                ui.setNormalizedNameSnapshot("밀가루");
                ui.setQuantity(BigDecimal.valueOf(800));
                ui.setUnit("g");
                userIngredientRepository.save(ui);

                // 레시피: 밀가루 1kg (1000g)
                Recipe recipe = recipeRepository.save(Recipe.builder().title("Flour Test").isActive(true).build());
                recipeNutritionRepository.save(RecipeNutrition.builder().recipe(recipe).calories(BigDecimal.ZERO)
                                .carbs(BigDecimal.ZERO).protein(BigDecimal.ZERO).fat(BigDecimal.ZERO).build());
                recipeStepRepository.save(
                                RecipeStep.builder().recipe(recipe).stepNo(1).instructionText("Use flour").build());
                recipeIngredientRepository.save(RecipeIngredient.builder().recipe(recipe).normalizedNameSnapshot("밀가루")
                                .amountText("1kg").build());

                // 조회
                RecipeResponse response = recipeService.getRecipe(recipe.getRecipeId(), userId);

                // 검증: 800g < 1000g -> NOT_ENOUGH
                RecipeIngredientDTO dto = findIngredient(response, "밀가루");
                assertThat(dto.getSufficiency()).isEqualTo("NOT_ENOUGH");
        }

        // ============================================================================================
        // Recipe/Service/RecipeService.java의 extractNumericAmount 단위 변환 테스트
        // 1. kg -> g 변환 확인 (예: 1/2kg -> 500g)
        // 2. L -> mL 변환 확인 (예: 1.5L -> 1500mL)
        // 3. mL는 변환 없이 그대로 유지 확인
        // ============================================================================================
        @Test
        @DisplayName("Test extractNumericAmount with Unit Conversion (kg->g, L->mL)")
        void testExtractNumericAmountWithUnitConversion() {
                User user = User.create("testuser3", "test3@example.com", "password", "tester3");
                User savedUser = userRepository.save(user);
                Long userId = savedUser.getUserId();
                System.out.println("\n[TEST SETUP] Created User ID: " + userId);

                // Ingredient Master 추가
                IngredientMaster imFlour = IngredientMaster.builder().canonicalName("밀가루").normalizedName("밀가루")
                                .isActive(true).build();
                IngredientMaster imMilk = IngredientMaster.builder().canonicalName("우유").normalizedName("우유")
                                .isActive(true).build();
                IngredientMaster imWater = IngredientMaster.builder().canonicalName("물").normalizedName("물")
                                .isActive(true).build();
                ingredientMasterRepository.saveAll(List.of(imFlour, imMilk, imWater));
                System.out.println("[TEST SETUP] Created Ingredient Masters: 밀가루, 우유, 물");

                // 냉장고에 g, mL 단위로 저장
                // 1. 밀가루: 600g (Required 1/2kg = 500g) -> OK
                UserIngredient uiFlour = new UserIngredient();
                uiFlour.setUser(savedUser);
                uiFlour.setIngredientMaster(imFlour);
                uiFlour.setNormalizedNameSnapshot("밀가루");
                uiFlour.setQuantity(BigDecimal.valueOf(600));

                // 2. 우유: 1000mL (Required 1.5L = 1500mL) -> NOT_ENOUGH
                UserIngredient uiMilk = new UserIngredient();
                uiMilk.setUser(savedUser);
                uiMilk.setIngredientMaster(imMilk);
                uiMilk.setNormalizedNameSnapshot("우유");
                uiMilk.setQuantity(BigDecimal.valueOf(1000));

                // 3. 물: 500mL (Required 500mL = 500mL) -> OK
                UserIngredient uiWater = new UserIngredient();
                uiWater.setUser(savedUser);
                uiWater.setIngredientMaster(imWater);
                uiWater.setNormalizedNameSnapshot("물");
                uiWater.setQuantity(BigDecimal.valueOf(500));

                userIngredientRepository.saveAll(List.of(uiFlour, uiMilk, uiWater));
                System.out.println("[TEST SETUP] Filled Fridge: 밀가루 (600g), 우유 (1000mL), 물 (500mL)");

                // 레시피 추가 (kg, L 단위 사용)
                Recipe recipe = Recipe.builder().title("Baking Test").isActive(true).build();
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

                // 레시피 재료에 kg, L 단위 설정
                RecipeIngredient riFlour = RecipeIngredient.builder().recipe(savedRecipe).normalizedNameSnapshot("밀가루")
                                .amountText("1/2kg").build();
                RecipeIngredient riMilk = RecipeIngredient.builder().recipe(savedRecipe).normalizedNameSnapshot("우유")
                                .amountText("1.5L").build();
                RecipeIngredient riWater = RecipeIngredient.builder().recipe(savedRecipe).normalizedNameSnapshot("물")
                                .amountText("500mL").build();
                recipeIngredientRepository.saveAll(List.of(riFlour, riMilk, riWater));

                // 레시피 조회 및 검증
                System.out.println("[TEST EXECUTION] Calling recipeService.getRecipe for recipeId: "
                                + savedRecipe.getRecipeId());
                RecipeResponse response = recipeService.getRecipe(savedRecipe.getRecipeId(), userId);

                // 결과 검증
                System.out.println("[TEST RESULTS] Recipe: " + response.getTitle());
                response.getRecipeIngredients().forEach(ing -> {
                        System.out.printf(" - Ingredient: %-10s | Owned: %-5b | Sufficiency: %-10s | Amount: %s\n",
                                        ing.getIngredientName(), ing.getOwned(), ing.getSufficiency(),
                                        ing.getAmountText());
                });

                // 밀가루: 600g (Required 1/2kg = 500g) -> OK
                RecipeIngredientDTO flourDTO = findIngredient(response, "밀가루");
                assertThat(flourDTO.getSufficiency()).isEqualTo("OK"); // 600 >= 500

                // 우유: 1000mL (Required 1.5L = 1500mL) -> NOT_ENOUGH
                RecipeIngredientDTO milkDTO = findIngredient(response, "우유");
                assertThat(milkDTO.getSufficiency()).isEqualTo("NOT_ENOUGH"); // 1000 < 1500

                // 물: 500mL (Required 500mL = 500mL) -> OK
                RecipeIngredientDTO waterDTO = findIngredient(response, "물");
                assertThat(waterDTO.getSufficiency()).isEqualTo("OK"); // 500 >= 500
        }

        // 시험 Helper Method
        // 레시피 재료가 제대로 추가되었는지 확인
        private RecipeIngredientDTO findIngredient(RecipeResponse response, String name) {
                return response.getRecipeIngredients().stream()
                                .filter(i -> name.equals(i.getNormalizedNameSnapshot()))
                                .findFirst()
                                .orElseThrow(() -> new AssertionError("Ingredient not found: " + name));
        }
}
