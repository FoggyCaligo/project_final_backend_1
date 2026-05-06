package com.today.fridge.recipe.service;

/*
 * RecipeServiceTest는 RecipeService의 비즈니스 로직을 단위 테스트하는 클래스입니다.
 *
 * @ExtendWith(MockitoExtension.class)를 사용하여 Spring Context나 DB를 로드하지 않고,
 * Mock 객체를 주입하여 서비스 계층만 격리하여 빠르고 안정적으로 테스트합니다.
 */

import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.global.response.PageResult;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.data.domain.PageRequest;
import com.today.fridge.recipe.repository.RecipeTagRepository;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;


import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

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

    @Mock
    private RecipeTagRepository recipeTagRepository;

    @Nested
    @DisplayName("getRecipe() - 레시피 상세 조회")
    class GetRecipeTest {

        @Test
        @DisplayName("비회원: 정상적으로 레시피를 조회")
        void getRecipe_NonMember_Success() {
            // given
            Long recipeId = 1L;
            Recipe recipe = Recipe.builder().recipeId(recipeId).title("김치찌개").build();
            RecipeNutrition nutrition = RecipeNutrition.builder().calories(new BigDecimal("500")).build();
            RecipeStep step = RecipeStep.builder().stepNo(1).instructionText("김치를 썬다").build();
            RecipeIngredient ingredient = RecipeIngredient.builder().rawText("김치").amountText("200g").build();

            given(recipeRepository.findById(recipeId)).willReturn(Optional.of(recipe));
            given(recipeNutritionRepository.findByRecipe_RecipeId(recipeId)).willReturn(Optional.of(nutrition));
            given(recipeStepRepository.findByRecipe_RecipeId(recipeId)).willReturn(List.of(step));
            given(recipeIngredientRepository.findByRecipe_RecipeId(recipeId)).willReturn(List.of(ingredient));

            // when
            RecipeResponse response = recipeService.getRecipe(recipeId);

            // then
            assertThat(response.getTitle()).isEqualTo("김치찌개");
            assertThat(response.getCalories()).isEqualByComparingTo(new BigDecimal("500"));
            assertThat(response.getRecipeSteps()).hasSize(1);
            assertThat(response.getRecipeIngredients()).hasSize(1);
        }

        @Test
        @DisplayName("회원: 소유한 재료(충분함)와 함께 레시피를 조회")
        void getRecipe_Member_Success_WithIngredients() {
            // given
            Long recipeId = 1L;
            Long userId = 100L;

            Recipe recipe = Recipe.builder().recipeId(recipeId).title("양파볶음").build();
            RecipeNutrition nutrition = RecipeNutrition.builder().calories(new BigDecimal("200")).build();
            RecipeStep step = RecipeStep.builder().stepNo(1).instructionText("양파를 볶는다").build();

            // 레시피에는 양파 100g이 필요함
            RecipeIngredient ingredient = RecipeIngredient.builder()
                    .rawText("양파")
                    .normalizedNameSnapshot("양파")
                    .amountText("100g")
                    .build();

            // 유저는 냉장고에 양파 150g을 가지고 있음
            UserIngredient userIngredient = new UserIngredient();
            userIngredient.setRawName("양파");
            userIngredient.setQuantity(new BigDecimal("150"));
            userIngredient.setUnit("g");

            given(recipeRepository.findById(recipeId)).willReturn(Optional.of(recipe));
            given(recipeNutritionRepository.findByRecipe_RecipeId(recipeId)).willReturn(Optional.of(nutrition));
            given(recipeStepRepository.findByRecipe_RecipeId(recipeId)).willReturn(List.of(step));
            given(recipeIngredientRepository.findByRecipe_RecipeId(recipeId)).willReturn(List.of(ingredient));
            given(userIngredientRepository.findByUserIdAndIngredientNameIn(eq(userId), any()))
                    .willReturn(List.of(userIngredient));

            // when
            RecipeResponse response = recipeService.getRecipe(recipeId, userId);

            // then
            assertThat(response.getRecipeIngredients().get(0).getOwned()).isTrue();
            assertThat(response.getRecipeIngredients().get(0).getSufficiency()).isEqualTo("OK");
            assertThat(response.getRecipeIngredients().get(0).getUserQuantity()).isEqualByComparingTo("150");
        }

        @Test
        @DisplayName("레시피가 존재하지 않으면 예외가 발생")
        void getRecipe_NotFound_ThrowsException() {
            // given
            Long recipeId = 999L;
            given(recipeRepository.findById(recipeId)).willReturn(Optional.empty());

            // when & then
            ExceptionTemplate exception = assertThrows(ExceptionTemplate.class, () -> {
                recipeService.getRecipe(recipeId);
            });
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RECIPE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("ateRecipe() - 레시피 조리 완료 (재료 차감)")
    class AteRecipeTest {

        @Test
        @DisplayName("레시피 조리 완료 시 냉장고에서 필요한 만큼 재료가 차감/삭제")
        void ateRecipe_Success() {
            // given
            Long recipeId = 1L;
            Long userId = 100L;

            // 레시피에는 양파 100g이 필요함
            RecipeIngredient recipeIngredient = RecipeIngredient.builder()
                    .rawText("양파")
                    .normalizedNameSnapshot("양파")
                    .amountText("100g")
                    .build();

            // 유저는 냉장고에 양파 150g을 가지고 있음
            UserIngredient userIngredient = new UserIngredient();
            userIngredient.setUserIngredientId(10L);
            userIngredient.setRawName("양파");
            userIngredient.setQuantity(new BigDecimal("150"));
            userIngredient.setUnit("g");

            given(recipeIngredientRepository.findByRecipe_RecipeId(recipeId))
                    .willReturn(List.of(recipeIngredient));
            given(userIngredientRepository.findByUserIdAndIngredientNameIn(eq(userId), any()))
                    .willReturn(List.of(userIngredient));

            // when
            recipeService.ateRecipe(recipeId, userId);

            // then
            // 150g 중 100g 차감되어 50g이 남아야 함
            assertThat(userIngredient.getQuantity()).isEqualByComparingTo("50");

            // 수량이 남았으므로 delete는 호출되지 않아야 함
            then(userIngredientRepository).should(times(0)).delete(any());
        }
    }
    @Test
    @DisplayName("전체 레시피를 페이징으로 조회한다")
    void testGetRecipes_All() {
        Pageable pageable = PageRequest.of(0, 10);

        Recipe recipe = Recipe.builder()
                .recipeId(1L)
                .title("A Recipe")
                .thumbnailUrl("test.jpg")
                .cookTimeText("10분")
                .isActive(true)
                .build();

        Page<Recipe> recipePage = new PageImpl<>(List.of(recipe), pageable, 1);

        given(recipeRepository.findByIsActiveTrue(any(Pageable.class)))
                .willReturn(recipePage);

        PageResult<RecipeListResponse> result =
                recipeService.getRecipes("ALL", null, pageable);

        assertThat(result.content()).isNotEmpty();
        assertThat(result.pageInfo().page()).isEqualTo(0);
        assertThat(result.pageInfo().size()).isEqualTo(10);
    }

    @Test
    @DisplayName("이름순 정렬을 적용한다")
    void testGetRecipes_SortByName() {
        Pageable pageable = PageRequest.of(0, 10);

        Recipe recipeA = Recipe.builder()
                .recipeId(1L)
                .title("A Recipe")
                .thumbnailUrl("a.jpg")
                .cookTimeText("10분")
                .isActive(true)
                .build();

        Recipe recipeB = Recipe.builder()
                .recipeId(2L)
                .title("B Recipe")
                .thumbnailUrl("b.jpg")
                .cookTimeText("20분")
                .isActive(true)
                .build();

        Page<Recipe> recipePage = new PageImpl<>(List.of(recipeA, recipeB), pageable, 2);

        given(recipeRepository.findByIsActiveTrue(any(Pageable.class)))
                .willReturn(recipePage);

        PageResult<RecipeListResponse> result =
                recipeService.getRecipes("ALL", "name", pageable);

        assertThat(result.content())
                .extracting(RecipeListResponse::title)
                .containsSubsequence("A Recipe", "B Recipe");
    }

    @Test
    @DisplayName("cookingType 필터를 적용한다")
    void testGetRecipes_FilterByCookingType() {
        Pageable pageable = PageRequest.of(0, 10);

        Recipe recipe = Recipe.builder()
                .recipeId(1L)
                .title("어묵국")
                .thumbnailUrl("soup.jpg")
                .cookTimeText("10분")
                .isActive(true)
                .build();

        Page<Recipe> recipePage = new PageImpl<>(List.of(recipe), pageable, 1);

        given(recipeRepository.findActiveRecipesByCookingType(eq("SOUP"), any(Pageable.class)))
                .willReturn(recipePage);

        PageResult<RecipeListResponse> result =
                recipeService.getRecipes("SOUP", null, pageable);

        assertThat(result.content())
                .extracting(RecipeListResponse::title)
                .contains("어묵국");
    }
    
}
