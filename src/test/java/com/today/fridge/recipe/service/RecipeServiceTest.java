package com.today.fridge.recipe.service;

/*
 * RecipeServiceTest는 RecipeService의 비즈니스 로직을 단위 테스트하는 클래스입니다.
 *
 * @ExtendWith(MockitoExtension.class)를 사용하여 Spring Context나 DB를 로드하지 않고,
 * Mock 객체를 주입하여 서비스 계층만 격리하여 빠르고 안정적으로 테스트합니다.
 */

import com.today.fridge.global.response.PageResult;
import com.today.fridge.ingredient.repository.UserIngredientRepository;
import com.today.fridge.recipe.dto.response.RecipeListResponse;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.repository.RecipeIngredientRepository;
import com.today.fridge.recipe.repository.RecipeNutritionRepository;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.recipe.repository.RecipeStepRepository;
import org.junit.jupiter.api.DisplayName;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

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

                PageResult<RecipeListResponse> result = recipeService.getRecipes("ALL", null, pageable);

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

                PageResult<RecipeListResponse> result = recipeService.getRecipes("ALL", "name", pageable);

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

                PageResult<RecipeListResponse> result = recipeService.getRecipes("SOUP", null, pageable);

                assertThat(result.content())
                                .extracting(RecipeListResponse::title)
                                .contains("어묵국");
        }

}
