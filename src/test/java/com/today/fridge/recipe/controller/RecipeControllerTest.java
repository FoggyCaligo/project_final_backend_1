package com.today.fridge.recipe.controller;

/*
 * RecipeControllerTest는 RecipeController의 각 엔드포인트를 MockMvc를 사용하여 단위 테스트하는 클래스입니다.
 *
 * Spring Security가 활성화되어 있으므로 @WithMockUser로 인증을 우회합니다.
 * RecipeService를 @MockBean으로 주입하여 Controller 계층만 격리 테스트합니다.
 *
 * 주요 테스트 엔드포인트:
 * 1. GET /api/v1/recipes - 전체 레시피 목록 페이징 조회
 * 2. GET /api/v1/recipes/{recipeId} - 상세 레시피 조회 (비회원/회원)
 * 3. GET /api/v1/recipes/{recipeId}/cooked - 레시피 조리 완료 (재료 차감)
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
@ActiveProfiles("test")
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
                @DisplayName("페이징된 레시피 목록을 정상적으로 반환한다")
                void 전체목록_정상조회() throws Exception {
                        // given - 서비스에서 레시피 목록을 반환하도록 설정
                        RecipeListResponse item = RecipeListResponse.builder()
                                        .recipeId(1L)
                                        .title("김치찌개")
                                        .summary("맛있는 김치찌개")
                                        .build();

                        PageResponse pageInfo = new PageResponse(1, 1, 0, 12);
                        PageResult<RecipeListResponse> pageResult = new PageResult<>(List.of(item), pageInfo);

                        given(recipeService.getRecipes(any())).willReturn(pageResult);

                        // when & then - HTTP 응답 검증
                        mockMvc.perform(get("/api/v1/recipes"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.success").value(true))
                                        .andExpect(jsonPath("$.data.content[0].recipeId").value(1))
                                        .andExpect(jsonPath("$.data.content[0].title").value("김치찌개"))
                                        .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1));
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
                @DisplayName("비회원(userId 없음)으로 상세 레시피를 정상 조회한다")
                void 비회원_상세조회() throws Exception {
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
                @DisplayName("회원(userId 포함)으로 상세 레시피를 조회한다")
                void 회원_상세조회() throws Exception {
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
                @DisplayName("존재하지 않는 recipeId 조회 시 예외가 발생한다")
                void 레시피_미존재_예외() throws Exception {
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
                @DisplayName("레시피 조리 완료 시 성공 응답을 반환한다")
                void 조리완료_성공() throws Exception {
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
