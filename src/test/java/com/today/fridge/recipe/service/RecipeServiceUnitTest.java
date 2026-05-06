package com.today.fridge.recipe.service;

/*
 * RecipeServiceUnitTest는 RecipeService의 각 메서드를 Mockito를 사용하여 단위 테스트하는 클래스입니다.
 *
 * 이 테스트는 @SpringBootTest를 사용하지 않고 순수 Mockito 기반으로 동작하므로,
 * 데이터베이스 연결 없이 빠르게 실행됩니다.
 *
 * 주요 테스트 시나리오:
 * 1. getRecipe(Long recipeId) - 비회원 전용 레시피 조회
 *    - 정상 조회 시 RecipeResponse 반환 확인
 *    - 레시피 미존재 시 RECIPE_NOT_FOUND 예외 발생 확인
 *    - 영양정보 미존재 시 RECIPE_NUTRITION_NOT_FOUND 예외 발생 확인
 *    - 단계 미존재 시 RECIPE_STEP_NOT_FOUND 예외 발생 확인
 *    - 재료 미존재 시 RECIPE_INGREDIENT_NOT_FOUND 예외 발생 확인
 * 2. getRecipe(Long recipeId, Long userId) - 회원 전용 레시피 조회
 *    - userId가 null인 경우 비회원 메서드로 위임 확인
 *    - 재료 충분 여부(OK, NOT_ENOUGH, MISSING) 판정 확인
 * 3. getRecipes(Pageable) - 페이지네이션 레시피 목록 조회
 *    - PageResult 반환 및 페이지 정보 확인
 * 4. ateRecipe(Long recipeId, Long userId) - 레시피 조리 완료 시 재료 차감
 *    - 유통기한 임박순 차감 확인
 *    - 수량 0 이하 시 삭제 확인
 */

import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.global.response.PageResult;
import com.today.fridge.ingredient.entity.IngredientMaster;
import com.today.fridge.ingredient.entity.UserIngredient;
import com.today.fridge.ingredient.repository.UserIngredientRepository;
import com.today.fridge.recipe.dto.response.RecipeListResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

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
        @DisplayName("UT-RECIPE-14 - 정상적으로 레시피를 조회")
        void getRecipe_Success() {
            // given - 모든 Repository에서 정상 데이터를 반환하도록 설정
            given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
            given(recipeNutritionRepository.findByRecipe_RecipeId(1L)).willReturn(Optional.of(testNutrition));
            given(recipeStepRepository.findByRecipe_RecipeId(1L)).willReturn(List.of(testStep));
            given(recipeIngredientRepository.findByRecipe_RecipeId(1L)).willReturn(List.of(testIngredient));

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
        @DisplayName("UT-RECIPE-15 - RECIPE_NOT_FOUND 예외")
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
        @DisplayName("UT-RECIPE-16 - RECIPE_NUTRITION_NOT_FOUND 예외")
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
        @DisplayName("UT-RECIPE-17 - RECIPE_STEP_NOT_FOUND 예외")
        void getRecipe_StepNotFound_ThrowsException() {
            // given - 레시피, 영양정보는 있지만 단계 정보 없음
            given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
            given(recipeNutritionRepository.findByRecipe_RecipeId(1L)).willReturn(Optional.of(testNutrition));
            given(recipeStepRepository.findByRecipe_RecipeId(1L)).willReturn(Collections.emptyList());

            // when & then
            assertThatThrownBy(() -> recipeService.getRecipe(1L))
                    .isInstanceOf(ExceptionTemplate.class)
                    .satisfies(e -> assertThat(((ExceptionTemplate) e).getErrorCode())
                            .isEqualTo(ErrorCode.RECIPE_STEP_NOT_FOUND));
        }

        @Test
        @DisplayName("UT-RECIPE-18 - RECIPE_INGREDIENT_NOT_FOUND 예외")
        void getRecipe_IngredientNotFound_ThrowsException() {
            // given - 레시피, 영양정보, 단계 정보는 있지만 재료 정보 없음
            given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
            given(recipeNutritionRepository.findByRecipe_RecipeId(1L)).willReturn(Optional.of(testNutrition));
            given(recipeStepRepository.findByRecipe_RecipeId(1L)).willReturn(List.of(testStep));
            given(recipeIngredientRepository.findByRecipe_RecipeId(1L)).willReturn(Collections.emptyList());

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
        @DisplayName("UT-RECIPE-19 - 회원: UserId = null 오류")
        void getRecipe_NullUserId_CallsGuestMethod() {
            // given - 비회원 경로의 Repository 호출 설정
            given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
            given(recipeNutritionRepository.findByRecipe_RecipeId(1L)).willReturn(Optional.of(testNutrition));
            given(recipeStepRepository.findByRecipe_RecipeId(1L)).willReturn(List.of(testStep));
            given(recipeIngredientRepository.findByRecipe_RecipeId(1L)).willReturn(List.of(testIngredient));

            // when - userId를 null로 전달
            RecipeResponse response = recipeService.getRecipe(1L, null);

            // then - 비회원용 응답 검증 (owned, sufficiency 필드가 설정되지 않음)
            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo("김치찌개");
            // 비회원이므로 userIngredientRepository가 호출되지 않아야 함
            then(userIngredientRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("UT-RECIPE-09 - sufficiency OK")
        void getRecipe_SufficiencyOk() {
            // given - 레시피 재료: 김치 200g, 유저 보유: 300g
            given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
            given(recipeNutritionRepository.findByRecipe_RecipeId(1L)).willReturn(Optional.of(testNutrition));
            given(recipeStepRepository.findByRecipe_RecipeId(1L)).willReturn(List.of(testStep));
            given(recipeIngredientRepository.findByRecipe_RecipeId(1L)).willReturn(List.of(testIngredient));

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
        @DisplayName("UT-RECIPE-09 - sufficiency NOT_ENOUGH")
        void getRecipe_SufficiencyNotEnough() {
            // given - 레시피 재료: 김치 200g, 유저 보유: 50g
            given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
            given(recipeNutritionRepository.findByRecipe_RecipeId(1L)).willReturn(Optional.of(testNutrition));
            given(recipeStepRepository.findByRecipe_RecipeId(1L)).willReturn(List.of(testStep));
            given(recipeIngredientRepository.findByRecipe_RecipeId(1L)).willReturn(List.of(testIngredient));

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
        @DisplayName("UT-RECIPE-09 - sufficiency MISSING")
        void getRecipe_SufficiencyMissing() {
            // given - 레시피 재료: 김치 200g, 유저 보유: 없음
            given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
            given(recipeNutritionRepository.findByRecipe_RecipeId(1L)).willReturn(Optional.of(testNutrition));
            given(recipeStepRepository.findByRecipe_RecipeId(1L)).willReturn(List.of(testStep));
            given(recipeIngredientRepository.findByRecipe_RecipeId(1L)).willReturn(List.of(testIngredient));

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
        @DisplayName("UT-RECIPE-09 - 동일 재료 수량 합산")
        void getRecipe_SumQuantities() {
            // given - 레시피: 김치 200g, 유저: 김치 100g + 김치 150g = 250g → OK
            given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
            given(recipeNutritionRepository.findByRecipe_RecipeId(1L)).willReturn(Optional.of(testNutrition));
            given(recipeStepRepository.findByRecipe_RecipeId(1L)).willReturn(List.of(testStep));
            given(recipeIngredientRepository.findByRecipe_RecipeId(1L)).willReturn(List.of(testIngredient));

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
        @DisplayName("UT-RECIPE-09 - normalizedName 매칭")
        void getRecipe_MasterNameMatch() {
            // given - 재료 마스터 테이블을 통한 매칭 시나리오
            given(recipeRepository.findById(1L)).willReturn(Optional.of(testRecipe));
            given(recipeNutritionRepository.findByRecipe_RecipeId(1L)).willReturn(Optional.of(testNutrition));
            given(recipeStepRepository.findByRecipe_RecipeId(1L)).willReturn(List.of(testStep));
            given(recipeIngredientRepository.findByRecipe_RecipeId(1L)).willReturn(List.of(testIngredient));

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
    // getRecipes(Pageable) - 전체 레시피 목록 페이징 조회 테스트
    // ========================================================================
    @Nested
    @DisplayName("RecipeServiceUnitTest - getRecipes - 전체 레시피 목록 조회")
    class GetRecipesList {

        @Test
        @DisplayName("RecipeServiceUnitTest - getRecipes - 페이징된 레시피 목록을 정상 반환한다")
        void getRecipes_Success() {
            // given - 2개의 레시피가 포함된 페이지 생성
            Recipe r1 = Recipe.builder().recipeId(1L).title("김치찌개").build();
            Recipe r2 = Recipe.builder().recipeId(2L).title("된장찌개").build();

            Pageable pageable = PageRequest.of(0, 12);
            Page<Recipe> recipePage = new PageImpl<>(List.of(r1, r2), pageable, 2);

            given(recipeRepository.findByIsActiveTrue(pageable)).willReturn(recipePage);

            // when
            PageResult<RecipeListResponse> result = recipeService.getRecipes(null, null, pageable);

            // then - 페이지 정보 및 콘텐츠 검증
            assertThat(result.content()).hasSize(2);
            assertThat(result.content().get(0).title()).isEqualTo("김치찌개");
            assertThat(result.content().get(1).title()).isEqualTo("된장찌개");
            assertThat(result.pageInfo().totalElements()).isEqualTo(2);
            assertThat(result.pageInfo().totalPages()).isEqualTo(1);
            assertThat(result.pageInfo().page()).isEqualTo(0);
        }

        @Test
        @DisplayName("RecipeServiceUnitTest - getRecipes - 레시피가 없을 때 빈 목록을 반환한다")
        void getRecipes_EmptyList() {
            // given - 빈 페이지
            Pageable pageable = PageRequest.of(0, 12);
            Page<Recipe> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            given(recipeRepository.findByIsActiveTrue(pageable)).willReturn(emptyPage);

            // when
            PageResult<RecipeListResponse> result = recipeService.getRecipes(null, null, pageable);

            // then
            assertThat(result.content()).isEmpty();
            assertThat(result.pageInfo().totalElements()).isEqualTo(0);
        }
    }

    // ========================================================================
    // ateRecipe(Long recipeId, Long userId) - 레시피 조리 완료 시 재료 차감 테스트
    // ========================================================================
    @Nested
    @DisplayName("RecipeServiceUnitTest - ateRecipe - 냉장고에서 재료가 차감/삭제")
    class AteRecipe {

        @Test
        @DisplayName("UT-RECIPE-20 - 재료가 충분할 때 수량 차감")
        void ateRecipe_DeductQuantity() {
            // given - 레시피: 김치 200g, 유저: 김치 300g
            given(recipeIngredientRepository.findByRecipe_RecipeId(1L)).willReturn(List.of(testIngredient));

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
        @DisplayName("UT-RECIPE-20 - 재료 수량 일치 시 삭제")
        void ateRecipe_DeleteWhenZero() {
            // given - 레시피: 김치 200g, 유저: 김치 200g → 차감 후 0 → 삭제
            given(recipeIngredientRepository.findByRecipe_RecipeId(1L)).willReturn(List.of(testIngredient));

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
        @DisplayName("UT-RECIPE-20 - 재료가 아예 없는 경우 정상 동작")
        void ateRecipe_NoIngredients_Success() {
            // given - 레시피에 필요한 재료가 있지만 유저가 보유하지 않음
            given(recipeIngredientRepository.findByRecipe_RecipeId(1L)).willReturn(List.of(testIngredient));
            given(userIngredientRepository.findByUserIdAndIngredientNameIn(eq(10L), anyList()))
                    .willReturn(Collections.emptyList());

            // when & then - 예외가 발생하지 않아야 함
            assertThatNoException().isThrownBy(() -> recipeService.ateRecipe(1L, 10L));
        }

        @Test
        @DisplayName("UT-RECIPE-20 - 유통기한 임박순으로 차감")
        void ateRecipe_SortByExpiry() {
            // given - 레시피: 김치 150g
            // 유저: 김치 100g(오늘 만료) + 김치 200g(7일 후 만료)
            // 예상: 100g 전량 소진(삭제) + 50g 추가 차감 → 150g 남음
            given(recipeIngredientRepository.findByRecipe_RecipeId(1L)).willReturn(List.of(
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
