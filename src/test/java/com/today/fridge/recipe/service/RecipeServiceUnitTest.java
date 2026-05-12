package com.today.fridge.recipe.service;

/*
 * UT-24
 * Method: getRecipe
 * Test Name: 정상적으로 레시피를 조회
 * Purpose: 비회원 상태에서 레시피 ID로 상세 정보를 정상적으로 조회한다.
 * Input: recipeId
 * Expected Result: RecipeResponse 반환 및 제목, 영양정보, 단계, 재료 데이터 일치 확인.
 * Priority: High
 *
 * UT-25
 * Method: getRecipe
 * Test Name: RECIPE_NOT_FOUND 예외
 * Purpose: 존재하지 않는 레시피 ID로 조회 시 적절한 예외를 발생시킨다.
 * Input: recipeId (non-existent)
 * Expected Result: RECIPE_NOT_FOUND 에러 코드를 포함한 ExceptionTemplate 발생.
 * Priority: Medium
 *
 * UT-26
 * Method: getRecipe
 * Test Name: RECIPE_NUTRITION_NOT_FOUND 예외
 * Purpose: 레시피는 존재하지만 연관된 영양정보가 없을 경우 예외를 발생시킨다.
 * Input: recipeId
 * Expected Result: RECIPE_NUTRITION_NOT_FOUND 에러 코드 발생.
 * Priority: Low
 *
 * UT-27
 * Method: getRecipe
 * Test Name: RECIPE_STEP_NOT_FOUND 예외
 * Purpose: 레시피는 존재하지만 연관된 조리 단계 정보가 없을 경우 예외를 발생시킨다.
 * Input: recipeId
 * Expected Result: RECIPE_STEP_NOT_FOUND 에러 코드 발생.
 * Priority: Low
 *
 * UT-28
 * Method: getRecipe
 * Test Name: RECIPE_INGREDIENT_NOT_FOUND 예외
 * Purpose: 레시피는 존재하지만 연관된 재료 정보가 없을 경우 예외를 발생시킨다.
 * Input: recipeId
 * Expected Result: RECIPE_INGREDIENT_NOT_FOUND 에러 코드 발생.
 * Priority: Low
 *
 * UT-29
 * Method: getRecipe (회원)
 * Test Name: 회원: UserId = null 오류
 * Purpose: 회원 전용 조회 메서드에 userId가 null로 전달될 경우 비회원 조회 로직으로 위임되는지 확인한다.
 * Input: recipeId, userId (null)
 * Expected Result: 정상 응답 반환 및 냉장고 연동 로직 미수행.
 * Priority: Medium
 *
 * UT-30
 * Method: getRecipe (회원)
 * Test Name: sufficiency OK
 * Purpose: 유저가 충분한 재료를 보유하고 있을 때 sufficiency 상태가 OK로 판정되는지 확인한다.
 * Expected Result: sufficiency == "OK"
 *
 * UT-31
 * Method: getRecipe (회원)
 * Test Name: sufficiency NOT_ENOUGH
 * Purpose: 유저가 재료를 보유하고 있으나 수량이 부족할 때 sufficiency 상태가 NOT_ENOUGH로 판정되는지 확인한다.
 * Expected Result: sufficiency == "NOT_ENOUGH"
 *
 * UT-32
 * Method: getRecipe (회원)
 * Test Name: sufficiency MISSING
 * Purpose: 유저가 재료를 전혀 보유하고 있지 않을 때 sufficiency 상태가 MISSING으로 판정되는지 확인한다.
 * Expected Result: sufficiency == "MISSING"
 *
 * UT-33
 * Method: getRecipe (회원)
 * Test Name: 동일 재료 수량 합산
 * Purpose: 유저가 동일한 재료를 여러 레코드로 나누어 보유하고 있을 때 총 수량이 정확히 합산되는지 확인한다.
 * Expected Result: 합산된 수량 기준 판정
 *
 * UT-34
 * Method: getRecipe (회원)
 * Test Name: normalizedName 매칭
 * Purpose: IngredientMaster를 통해 정규화된 이름으로 재료 매칭이 정상 수행되는지 확인한다.
 * Expected Result: 매칭 성공
 *
 * UT-35
 * Method: ateRecipe
 * Test Name: 재료 충분 시 수량 차감
 * Purpose: 조리 완료 시 냉장고 재료 수량이 정확히 차감되는지 확인한다.
 * Expected Result: 수량 차감 반영
 *
 * UT-36
 * Method: ateRecipe
 * Test Name: 재료 수량 일치 시 삭제
 * Purpose: 조리 완료 시 재료를 모두 소진할 경우 레코드가 삭제되는지 확인한다.
 * Expected Result: 레코드 삭제
 *
 * UT-37
 * Method: ateRecipe
 * Test Name: 재료 미보유 시 정상 동작
 * Purpose: 레시피 재료를 보유하고 있지 않아도 예외 없이 종료되는지 확인한다.
 * Expected Result: 정상 종료
 *
 * UT-38
 * Method: ateRecipe
 * Test Name: 유통기한 임박순 차감 검증
 * Purpose: 조리 완료 시 유통기한이 가장 임박한 재료부터 우선적으로 차감되는지 확인한다.
 * Expected Result: 임박순 차감 수행
 */

import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.ingredient.entity.IngredientMaster;
import com.today.fridge.ingredient.entity.UserIngredient;
import com.today.fridge.ingredient.repository.UserIngredientRepository;
import com.today.fridge.recipe.dto.response.RecipeResponse;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeIngredient;
import com.today.fridge.recipe.entity.RecipeNutrition;
import com.today.fridge.recipe.entity.RecipeStep;
import com.today.fridge.recipe.repository.RecipeIngredientRepository;
import com.today.fridge.recipe.repository.RecipeNutritionRepository;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.recipe.repository.RecipeStepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RecipeServiceUnitTest {

        @Mock
        private RecipeRepository recipeRepository;

        @Mock
        private RecipeNutritionRepository recipeNutritionRepository;

        @Mock
        private RecipeStepRepository recipeStepRepository;

        @Mock
        private RecipeIngredientRepository recipeIngredientRepository;

        @Mock
        private UserIngredientRepository userIngredientRepository;

        @InjectMocks
        private RecipeService recipeService;

        // ========================================================================
        // 테스트에서 공통으로 사용하는 공통 데이터
        // ========================================================================
        private Recipe testRecipe;
        private RecipeNutrition testNutrition;
        private RecipeStep testStep;
        private RecipeIngredient testIngredient;

        @BeforeEach
        void setUp() {
                // 기본 레시피 데이터 세팅
                testRecipe = Recipe.builder()
                                .recipeId(1L)
                                .title("김치찌개")
                                .sourceSite("test.com")
                                .isActive(true)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                testNutrition = RecipeNutrition.builder()
                                .recipe(testRecipe)
                                .calories(BigDecimal.valueOf(350))
                                .carbs(BigDecimal.valueOf(30))
                                .protein(BigDecimal.valueOf(25))
                                .fat(BigDecimal.valueOf(15))
                                .build();

                testStep = RecipeStep.builder()
                                .recipe(testRecipe)
                                .stepNo(1)
                                .instructionText("김치를 볶는다.")
                                .build();

                testIngredient = RecipeIngredient.builder()
                                .recipe(testRecipe)
                                .rawText("김치")
                                .normalizedNameSnapshot("김치")
                                .amountText("200g")
                                .build();
        }

        // ========================================================================
        // getRecipe(Long recipeId) - 비회원 전용 레시피 1개 조회 테스트
        // ========================================================================
        @Nested
        @DisplayName("getRecipe(Long recipeId) - 비회원 전용 레시피 1개 조회 테스트")
        class GetRecipeForGuest {

                @Test
                @DisplayName("UT-24 - 정상적으로 레시피를 조회")
                void getRecipe_Success() {
                        // given - 모든 Repository에서 정상 데이터를 반환하도록 설정
                        given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
                        given(recipeNutritionRepository.findByRecipe_RecipeId(1L))
                                        .willReturn(Optional.of(testNutrition));
                        given(recipeStepRepository.findByRecipe_RecipeIdOrderByStepNoAsc(1L)).willReturn(List.of(testStep));
                        given(recipeIngredientRepository.findByRecipe_RecipeIdOrderBySortOrderAsc(1L)).willReturn(List.of(testIngredient));

                        // when - 서비스 메서드 호출
                        RecipeResponse response = recipeService.getRecipe(1L);

                        // then - 응답 데이터 검증
                        assertThat(response).isNotNull();
                        assertThat(response.getRecipeId()).isEqualTo(1L);
                        assertThat(response.getTitle()).isEqualTo("김치찌개");
                        assertThat(response.getCalories()).isEqualByComparingTo(BigDecimal.valueOf(350));
                        assertThat(response.getRecipeSteps()).hasSize(1);
                        assertThat(response.getRecipeIngredients()).hasSize(1);
                }

                @Test
                @DisplayName("UT-25 - RECIPE_NOT_FOUND 예외")
                void getRecipe_NotFound_ThrowsException() {
                        // given - 레시피 없음
                        given(recipeRepository.findById(999L)).willReturn(Optional.empty());

                        // when & then - 예외 검증
                        assertThatThrownBy(() -> recipeService.getRecipe(999L))
                                        .isInstanceOf(ExceptionTemplate.class)
                                        .satisfies(e -> assertThat(((ExceptionTemplate) e).getErrorCode())
                                                        .isEqualTo(ErrorCode.RECIPE_NOT_FOUND));
                }

                @Test
                @DisplayName("UT-26 - RECIPE_NUTRITION_NOT_FOUND 예외")
                void getRecipe_NutritionNotFound_ThrowsException() {
                        // given - 레시피는 있지만 영양정보 없음
                        given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
                        given(recipeNutritionRepository.findByRecipe_RecipeId(1L)).willReturn(Optional.empty());

                        // when & then
                        assertThatThrownBy(() -> recipeService.getRecipe(1L))
                                        .isInstanceOf(ExceptionTemplate.class)
                                        .satisfies(e -> assertThat(((ExceptionTemplate) e).getErrorCode())
                                                        .isEqualTo(ErrorCode.RECIPE_NUTRITION_NOT_FOUND));
                }

                @Test
                @DisplayName("UT-27 - RECIPE_STEP_NOT_FOUND 예외")
                void getRecipe_StepNotFound_ThrowsException() {
                        // given - 레시피, 영양정보는 있지만 단계 정보 없음
                        given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
                        given(recipeNutritionRepository.findByRecipe_RecipeId(1L))
                                        .willReturn(Optional.of(testNutrition));
                        given(recipeStepRepository.findByRecipe_RecipeIdOrderByStepNoAsc(1L)).willReturn(Collections.emptyList());

                        // when & then
                        assertThatThrownBy(() -> recipeService.getRecipe(1L))
                                        .isInstanceOf(ExceptionTemplate.class)
                                        .satisfies(e -> assertThat(((ExceptionTemplate) e).getErrorCode())
                                                        .isEqualTo(ErrorCode.RECIPE_STEP_NOT_FOUND));
                }

                @Test
                @DisplayName("UT-28 - RECIPE_INGREDIENT_NOT_FOUND 예외")
                void getRecipe_IngredientNotFound_ThrowsException() {
                        // given - 레시피, 영양정보, 단계 정보는 있지만 재료 정보 없음
                        given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
                        given(recipeNutritionRepository.findByRecipe_RecipeId(1L))
                                        .willReturn(Optional.of(testNutrition));
                        given(recipeStepRepository.findByRecipe_RecipeIdOrderByStepNoAsc(1L)).willReturn(List.of(testStep));
                        given(recipeIngredientRepository.findByRecipe_RecipeIdOrderBySortOrderAsc(1L)).willReturn(Collections.emptyList());

                        // when & then
                        assertThatThrownBy(() -> recipeService.getRecipe(1L))
                                        .isInstanceOf(ExceptionTemplate.class)
                                        .satisfies(e -> assertThat(((ExceptionTemplate) e).getErrorCode())
                                                        .isEqualTo(ErrorCode.RECIPE_INGREDIENT_NOT_FOUND));
                }
        }

        // ========================================================================
        // getRecipe(Long recipeId, Long userId) - 회원 전용 레시피 1개 조회 테스트
        // ========================================================================
        @Nested
        @DisplayName("getRecipe(Long recipeId, Long userId) - 회원 전용 레시피 1개 조회 테스트")
        class GetRecipeForMember {

                @Test
                @DisplayName("UT-29 - 회원: UserId = null 오류")
                void getRecipe_NullUserId_CallsGuestMethod() {
                        // given - 비회원 경로의 Repository 호출 설정
                        given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
                        given(recipeNutritionRepository.findByRecipe_RecipeId(1L))
                                        .willReturn(Optional.of(testNutrition));
                        given(recipeStepRepository.findByRecipe_RecipeIdOrderByStepNoAsc(1L)).willReturn(List.of(testStep));
                        given(recipeIngredientRepository.findByRecipe_RecipeIdOrderBySortOrderAsc(1L)).willReturn(List.of(testIngredient));

                        // when - userId를 null로 전달
                        RecipeResponse response = recipeService.getRecipe(1L, null);

                        // then - 비회원용 응답 검증 (owned, sufficiency 필드가 설정되지 않음)
                        assertThat(response).isNotNull();
                        assertThat(response.getTitle()).isEqualTo("김치찌개");
                        // 비회원이므로 userIngredientRepository가 호출되지 않아야 함
                        then(userIngredientRepository).shouldHaveNoInteractions();
                }

                @Test
                @DisplayName("UT-30 - sufficiency OK")
                void getRecipe_SufficiencyOk() {
                        // given - 레시피 재료: 김치 200g, 유저 보유: 300g
                        given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
                        given(recipeNutritionRepository.findByRecipe_RecipeId(1L))
                                        .willReturn(Optional.of(testNutrition));
                        given(recipeStepRepository.findByRecipe_RecipeIdOrderByStepNoAsc(1L)).willReturn(List.of(testStep));
                        given(recipeIngredientRepository.findByRecipe_RecipeIdOrderBySortOrderAsc(1L)).willReturn(List.of(testIngredient));

                        // 유저가 충분한 재료를 가지고 있는 경우
                        UserIngredient owned = new UserIngredient();
                        owned.setNormalizedNameSnapshot("김치");
                        owned.setQuantity(BigDecimal.valueOf(300));
                        given(userIngredientRepository.findByUserIdAndIngredientNameIn(eq(10L), anyList()))
                                        .willReturn(List.of(owned));

                        // when
                        RecipeResponse response = recipeService.getRecipe(1L, 10L);

                        // then - 충분하므로 OK
                        assertThat(response.getRecipeIngredients().get(0).getSufficiency()).isEqualTo("OK");
                        assertThat(response.getRecipeIngredients().get(0).getOwned()).isTrue();
                }

                @Test
                @DisplayName("UT-31 - sufficiency NOT_ENOUGH")
                void getRecipe_SufficiencyNotEnough() {
                        // given - 레시피 재료: 김치 200g, 유저 보유: 50g
                        given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
                        given(recipeNutritionRepository.findByRecipe_RecipeId(1L))
                                        .willReturn(Optional.of(testNutrition));
                        given(recipeStepRepository.findByRecipe_RecipeIdOrderByStepNoAsc(1L)).willReturn(List.of(testStep));
                        given(recipeIngredientRepository.findByRecipe_RecipeIdOrderBySortOrderAsc(1L)).willReturn(List.of(testIngredient));

                        UserIngredient owned = new UserIngredient();
                        owned.setNormalizedNameSnapshot("김치");
                        owned.setQuantity(BigDecimal.valueOf(50));
                        given(userIngredientRepository.findByUserIdAndIngredientNameIn(eq(10L), anyList()))
                                        .willReturn(List.of(owned));

                        // when
                        RecipeResponse response = recipeService.getRecipe(1L, 10L);

                        // then - 부족하므로 NOT_ENOUGH
                        assertThat(response.getRecipeIngredients().get(0).getSufficiency()).isEqualTo("NOT_ENOUGH");
                        assertThat(response.getRecipeIngredients().get(0).getOwned()).isTrue();
                }

                @Test
                @DisplayName("UT-32 - sufficiency MISSING")
                void getRecipe_SufficiencyMissing() {
                        // given - 레시피 재료: 김치 200g, 유저 보유: 없음
                        given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
                        given(recipeNutritionRepository.findByRecipe_RecipeId(1L))
                                        .willReturn(Optional.of(testNutrition));
                        given(recipeStepRepository.findByRecipe_RecipeIdOrderByStepNoAsc(1L)).willReturn(List.of(testStep));
                        given(recipeIngredientRepository.findByRecipe_RecipeIdOrderBySortOrderAsc(1L)).willReturn(List.of(testIngredient));

                        // 유저가 재료를 보유하지 않은 경우
                        given(userIngredientRepository.findByUserIdAndIngredientNameIn(eq(10L), anyList()))
                                        .willReturn(Collections.emptyList());

                        // when
                        RecipeResponse response = recipeService.getRecipe(1L, 10L);

                        // then - 미보유이므로 MISSING
                        assertThat(response.getRecipeIngredients().get(0).getSufficiency()).isEqualTo("MISSING");
                        assertThat(response.getRecipeIngredients().get(0).getOwned()).isFalse();
                }

                @Test
                @DisplayName("UT-33 - 동일 재료 수량 합산")
                void getRecipe_SumQuantities() {
                        // given - 레시피: 김치 200g, 유저: 김치 100g + 김치 150g = 250g → OK
                        given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
                        given(recipeNutritionRepository.findByRecipe_RecipeId(1L))
                                        .willReturn(Optional.of(testNutrition));
                        given(recipeStepRepository.findByRecipe_RecipeIdOrderByStepNoAsc(1L)).willReturn(List.of(testStep));
                        given(recipeIngredientRepository.findByRecipe_RecipeIdOrderBySortOrderAsc(1L)).willReturn(List.of(testIngredient));

                        UserIngredient ui1 = new UserIngredient();
                        ui1.setNormalizedNameSnapshot("김치");
                        ui1.setQuantity(BigDecimal.valueOf(100));

                        UserIngredient ui2 = new UserIngredient();
                        ui2.setNormalizedNameSnapshot("김치");
                        ui2.setQuantity(BigDecimal.valueOf(150));

                        given(userIngredientRepository.findByUserIdAndIngredientNameIn(eq(10L), anyList()))
                                        .willReturn(List.of(ui1, ui2));

                        // when
                        RecipeResponse response = recipeService.getRecipe(1L, 10L);

                        // then - 합산 250g >= 200g → OK
                        assertThat(response.getRecipeIngredients().get(0).getSufficiency()).isEqualTo("OK");
                        assertThat(response.getRecipeIngredients().get(0).getUserQuantity())
                                        .isEqualByComparingTo(BigDecimal.valueOf(250));
                }

                @Test
                @DisplayName("UT-34 - normalizedName 매칭")
                void getRecipe_MasterNameMatch() {
                        // given - 재료 마스터 테이블을 통한 매칭 시나리오
                        given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
                        given(recipeNutritionRepository.findByRecipe_RecipeId(1L))
                                        .willReturn(Optional.of(testNutrition));
                        given(recipeStepRepository.findByRecipe_RecipeIdOrderByStepNoAsc(1L)).willReturn(List.of(testStep));
                        given(recipeIngredientRepository.findByRecipe_RecipeIdOrderBySortOrderAsc(1L)).willReturn(List.of(testIngredient));

                        // 유저 재료의 이름은 다르지만 마스터의 정규화된 이름이 레시피 재료와 일치
                        IngredientMaster master = IngredientMaster.builder()
                                        .normalizedName("김치")
                                        .build();
                        UserIngredient owned = new UserIngredient();
                        owned.setRawName("포기김치");
                        owned.setNormalizedNameSnapshot("포기김치");
                        owned.setIngredientMaster(master);
                        owned.setQuantity(BigDecimal.valueOf(500));

                        given(userIngredientRepository.findByUserIdAndIngredientNameIn(eq(10L), anyList()))
                                        .willReturn(List.of(owned));

                        // when
                        RecipeResponse response = recipeService.getRecipe(1L, 10L);

                        // then - 마스터 이름으로 매칭되어 OK
                        assertThat(response.getRecipeIngredients().get(0).getSufficiency()).isEqualTo("OK");
                }
        }

        // ========================================================================
        // ateRecipe(Long recipeId, Long userId) - 레시피 조리 완료 시 재료 차감 테스트
        // ========================================================================
        @Nested
        @DisplayName("RecipeServiceUnitTest - ateRecipe - 냉장고에서 재료가 차감/삭제")
        class AteRecipe {

                @Test
                @DisplayName("UT-35 - 재료가 충분할 때 수량 차감")
                void ateRecipe_DeductQuantity() {
                        // given - 레시피: 김치 200g, 유저: 김치 300g
                        given(recipeIngredientRepository.findByRecipe_RecipeIdOrderBySortOrderAsc(1L)).willReturn(List.of(testIngredient));

                        UserIngredient owned = new UserIngredient();
                        owned.setNormalizedNameSnapshot("김치");
                        owned.setQuantity(BigDecimal.valueOf(300));
                        owned.setExpiresAt(LocalDate.now().plusDays(5));

                        given(userIngredientRepository.findByUserIdAndIngredientNameIn(eq(10L), anyList()))
                                        .willReturn(List.of(owned));

                        // when
                        recipeService.ateRecipe(1L, 10L);

                        // then - 300 - 200 = 100g 남음, 삭제되지 않아야 함
                        assertThat(owned.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(100));
                        then(userIngredientRepository).should(never()).delete(any());
                }

                @Test
                @DisplayName("UT-36 - 재료 수량 일치 시 삭제")
                void ateRecipe_DeleteWhenZero() {
                        // given - 레시피: 김치 200g, 유저: 김치 200g → 차감 후 0 → 삭제
                        given(recipeIngredientRepository.findByRecipe_RecipeIdOrderBySortOrderAsc(1L)).willReturn(List.of(testIngredient));

                        UserIngredient owned = new UserIngredient();
                        owned.setNormalizedNameSnapshot("김치");
                        owned.setQuantity(BigDecimal.valueOf(200));
                        owned.setExpiresAt(LocalDate.now().plusDays(3));

                        given(userIngredientRepository.findByUserIdAndIngredientNameIn(eq(10L), anyList()))
                                        .willReturn(List.of(owned));

                        // when
                        recipeService.ateRecipe(1L, 10L);

                        // then - 수량 0이므로 삭제 호출
                        then(userIngredientRepository).should().delete(owned);
                }

                @Test
                @DisplayName("UT-37 - 재료가 아예 없는 경우 정상 동작")
                void ateRecipe_NoIngredients_Success() {
                        // given - 레시피에 필요한 재료가 있지만 유저가 보유하지 않음
                        given(recipeIngredientRepository.findByRecipe_RecipeIdOrderBySortOrderAsc(1L)).willReturn(List.of(testIngredient));
                        given(userIngredientRepository.findByUserIdAndIngredientNameIn(eq(10L), anyList()))
                                        .willReturn(Collections.emptyList());

                        // when & then - 예외가 발생하지 않아야 함
                        assertThatNoException().isThrownBy(() -> recipeService.ateRecipe(1L, 10L));
                }

                @Test
                @DisplayName("UT-38 - 유통기한 임박순으로 차감")
                void ateRecipe_SortByExpiry() {
                        // given - 레시피: 김치 150g
                        // 유저: 김치 100g(오늘 만료) + 김치 200g(7일 후 만료)
                        // 예상: 100g 전량 소진(삭제) + 50g 추가 차감 → 150g 남음
                        given(recipeIngredientRepository.findByRecipe_RecipeIdOrderBySortOrderAsc(1L)).willReturn(List.of(
                                        RecipeIngredient.builder()
                                                        .recipe(testRecipe)
                                                        .normalizedNameSnapshot("김치")
                                                        .amountText("150g")
                                                        .build()));

                        UserIngredient early = new UserIngredient();
                        early.setNormalizedNameSnapshot("김치");
                        early.setQuantity(BigDecimal.valueOf(100));
                        early.setExpiresAt(LocalDate.now());

                        UserIngredient later = new UserIngredient();
                        later.setNormalizedNameSnapshot("김치");
                        later.setQuantity(BigDecimal.valueOf(200));
                        later.setExpiresAt(LocalDate.now().plusDays(7));

                        given(userIngredientRepository.findByUserIdAndIngredientNameIn(eq(10L), anyList()))
                                        .willReturn(List.of(early, later));

                        // when
                        recipeService.ateRecipe(1L, 10L);

                        // then - 유통기한이 빠른 early는 전량 소진 및 삭제
                        then(userIngredientRepository).should().delete(early);
                        // later는 200 - 50 = 150g 남음
                        assertThat(later.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(150));
                }
        }
}
