package com.today.fridge.recipe.controller;

/*
 * UT-08
 * Method: getRecipes
 * Test Name: 페이징된 레시피 목록 반환
 * Purpose: 전체 레시피 목록 요청 시 페이징 정보와 함께 정상적으로 데이터를 반환하는지 확인한다.
 * Input: Pageable
 * Expected Result: HTTP 200 및 RecipeListResponse 목록 반환.
 * Priority: High
 *
 * UT-09
 * Method: getRecipes
 * Test Name: 필터 및 정렬 적용 조회
 * Purpose: cookingType 필터와 sort 파라미터를 적용하여 레시피 목록을 조회하는지 확인한다.
 * Input: cookingType, sort
 * Expected Result: 서비스 메서드에 해당 파라미터가 정확히 전달됨.
 * Priority: Medium
 *
 * UT-10
 * Method: getRecipe
 * Test Name: 비회원(userId 없음)으로 상세 레시피를 정상 조회한다
 * Purpose: 인증 없이 레시피 상세 API를 호출했을 때 비회원 전용 상세 정보를 반환하는지 확인한다.
 * Input: recipeId
 * Expected Result: HTTP 200 및 RecipeResponse 데이터 반환.
 * Priority: High
 *
 * UT-11
 * Method: getRecipe
 * Test Name: 회원(userId 포함)으로 상세 레시피를 조회한다
 * Purpose: 인증 헤더(X-User-Id)를 포함하여 호출했을 때 회원 전용(냉장고 연동) 상세 정보를 반환하는지 확인한다.
 * Input: recipeId, X-User-Id
 * Expected Result: HTTP 200 및 RecipeResponse 데이터 반환.
 * Priority: High
 *
 * UT-12
 * Method: getRecipe
 * Test Name: 존재하지 않는 recipeId 조회 시 예외가 발생한다
 * Purpose: 잘못된 레시피 ID로 호출 시 적절한 404 에러를 반환하는지 확인한다.
 * Input: recipeId (non-existent)
 * Expected Result: HTTP 404 반환.
 * Priority: Medium
 *
 * UT-13
 * Method: ateRecipe
 * Test Name: 레시피 조리 완료 시 성공 응답을 반환한다
 * Purpose: 조리 완료 API 호출 시 성공 메시지와 함께 재료 차감 로직이 트리거되는지 확인한다.
 * Input: recipeId, X-User-Id
 * Expected Result: HTTP 200 및 "레시피 재료 소진" 메시지 반환.
 * Priority: High
 */

import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.exception.ExceptionTemplate;
import com.today.fridge.global.response.PageResponse;
import com.today.fridge.global.response.PageResult;
import com.today.fridge.recipe.dto.intermediate.RecipeIngredientDTO;
import com.today.fridge.recipe.dto.intermediate.RecipeStepDTO;
import com.today.fridge.recipe.dto.response.RecipeListResponse;
import com.today.fridge.recipe.dto.response.RecipeResponse;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeNutrition;
import com.today.fridge.recipe.service.RecipeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("recipe")
@AutoConfigureMockMvc
class RecipeControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private RecipeService recipeService;

        // ========================================================================
        // GET /api/v1/recipes - 전체 레시피 목록 조회 테스트
        // ========================================================================
        @Nested
        @DisplayName("GET /api/v1/recipes - 전체 레시피 목록 조회")
        class GetRecipesList {

                @Test
                @WithMockUser
                @DisplayName("UT-08 - 페이징된 레시피 목록을 정상적으로 반환한다")
                void getRecipes_Success() throws Exception {
                        // given - 서비스에서 레시피 목록을 반환하도록 설정
                        RecipeListResponse item = RecipeListResponse.builder()
                                        .recipeId(1L)
                                        .title("김치찌개")
                                        .summary("맛있는 김치찌개")
                                        .build();

                        PageResponse pageInfo = new PageResponse(1, 1, 0, 12);
                        PageResult<RecipeListResponse> pageResult = new PageResult<>(List.of(item), pageInfo);

                        given(recipeService.getRecipes(any(), any(), any())).willReturn(pageResult);

                        // when & then - HTTP 응답 검증
                        mockMvc.perform(get("/api/v1/recipes"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.content[0].recipeId").value(1))
                                        .andExpect(jsonPath("$.data.content[0].title").value("김치찌개"))
                                        .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1));
                }
                @Test
                @WithMockUser
                @DisplayName("UT-09 - cookingType과 sort 파라미터를 전달하여 조회한다")
                void getRecipes_WithFilterAndSort() throws Exception {

                    PageResult<RecipeListResponse> pageResult =
                            new PageResult<>(List.of(), new PageResponse(0,0,0,12));

                    given(recipeService.getRecipes(any(), any(), any()))
                            .willReturn(pageResult);

                    mockMvc.perform(
                                    get("/api/v1/recipes")
                                            .param("cookingType", "SOUP")
                                            .param("sort", "time_asc")
                            )
                            .andExpect(status().isOk());

                    then(recipeService)
                            .should()
                            .getRecipes(
                                    eq("SOUP"),
                                    eq("time_asc"),
                                    any()
                            );
                }
        }

        // ========================================================================
        // GET /api/v1/recipes/{recipeId} - 상세 레시피 조회 테스트
        // ========================================================================
        @Nested
        @DisplayName("GET /api/v1/recipes/{recipeId} - 상세 레시피 조회")
        class GetRecipeDetail {

                @Test
                @WithMockUser
                @DisplayName("UT-10 - 비회원(userId 없음)으로 상세 레시피를 정상 조회한다")
                void getRecipe_Guest_Success() throws Exception {
                        // given - RecipeResponse 생성
                        RecipeResponse response = createTestRecipeResponse();
                        given(recipeService.getRecipe(eq(1L), isNull())).willReturn(response);

                        // when & then
                        mockMvc.perform(get("/api/v1/recipes/1"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.recipeId").value(1))
                                        .andExpect(jsonPath("$.data.title").value("김치찌개"));
                }

                @Test
                @WithMockUser
                @DisplayName("UT-11 - 회원(userId 포함)으로 상세 레시피를 조회한다")
                void getRecipe_Member_Success() throws Exception {
                        // given
                        RecipeResponse response = createTestRecipeResponse();
                        given(recipeService.getRecipe(eq(1L), eq(10L))).willReturn(response);

                        // when & then - userId를 헤더로 전달
                        mockMvc.perform(get("/api/v1/recipes/1").header("X-User-Id", "10"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.title").value("김치찌개"));
                }

                @Test
                @WithMockUser
                @DisplayName("UT-12 - 존재하지 않는 recipeId 조회 시 예외가 발생한다")
                void getRecipe_NotFound_ThrowsException() throws Exception {
                        // given
                        given(recipeService.getRecipe(eq(999L), isNull()))
                                        .willThrow(new ExceptionTemplate(ErrorCode.RECIPE_NOT_FOUND));

                        // when & then - 404 또는 에러 응답 반환 (GlobalExceptionHandler에 의존)
                        mockMvc.perform(get("/api/v1/recipes/999"))
                                        .andExpect(status().isNotFound());
                }
        }

        // ========================================================================
        // POST /api/v1/recipes/{recipeId}/cooked - 레시피 조리 완료 테스트
        // ========================================================================
        @Nested
        @DisplayName("POST /api/v1/recipes/{recipeId}/cooked - 레시피 조리 완료")
        class AteRecipe {

                @Test
                @WithMockUser
                @DisplayName("UT-13 - 레시피 조리 완료 시 성공 응답을 반환한다")
                void ateRecipe_Success() throws Exception {
                        // given - ateRecipe는 void 메서드이므로 아무 설정 필요 없음
                        willDoNothing().given(recipeService).ateRecipe(eq(1L), eq(10L));

                        // when & then
                        mockMvc.perform(post("/api/v1/recipes/1/cooked").header("X-User-Id", "10"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.message").value("레시피 재료 소진"));

                        // 서비스 메서드가 호출되었는지 검증
                        then(recipeService).should().ateRecipe(1L, 10L);
                }
        }

        // ========================================================================
        // 테스트 헬퍼 메서드
        // ========================================================================

        /**
         * 테스트용 RecipeResponse 객체를 생성하는 헬퍼 메서드
         */
        private RecipeResponse createTestRecipeResponse() {
                Recipe recipe = Recipe.builder()
                                .recipeId(1L)
                                .title("김치찌개")
                                .isActive(true)
                                .build();

                RecipeNutrition nutrition = RecipeNutrition.builder()
                                .recipe(recipe)
                                .calories(BigDecimal.valueOf(350))
                                .carbs(BigDecimal.valueOf(30))
                                .protein(BigDecimal.valueOf(25))
                                .fat(BigDecimal.valueOf(15))
                                .build();

                RecipeStepDTO step = RecipeStepDTO.builder()
                                .stepNo(1)
                                .instructionText("김치를 볶는다.")
                                .build();

                RecipeIngredientDTO ingredient = RecipeIngredientDTO.builder()
                                .rawText("김치")
                                .normalizedNameSnapshot("김치")
                                .amountText("200g")
                                .build();

                return RecipeResponse.of(recipe, nutrition, List.of(step), List.of(ingredient));
        }
}
