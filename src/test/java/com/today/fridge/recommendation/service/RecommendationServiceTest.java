package com.today.fridge.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.today.fridge.embedding.service.RecipeEmbeddingSearchService;
import com.today.fridge.global.response.PageResult;
import com.today.fridge.ingredient.repository.UserIngredientRepository;
import com.today.fridge.llm.service.RecommendationExplanationService;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.repository.RecipeIngredientRepository;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.recipe.repository.RecipeTagRepository;
import com.today.fridge.recommendation.dto.internal.RecommendationQuery;
import com.today.fridge.recommendation.dto.response.RecipeRecommendationResponse;
import com.today.fridge.recommendation.entity.ConditionCode;
import com.today.fridge.recommendation.entity.UserCondition;
import com.today.fridge.recommendation.repository.RecipeConditionMapRepository;
import com.today.fridge.recommendation.repository.UserConditionRepository;
import com.today.fridge.substitution.service.SubstituteIngredientService;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock private RecipeRepository recipeRepository;
    @Mock private RecipeIngredientRepository recipeIngredientRepository;
    @Mock private UserConditionRepository userConditionRepository;
    @Mock private UserIngredientRepository userIngredientRepository;
    @Mock private AllergyFilterService allergyFilterService;
    @Mock private RecommendationScoreService recommendationScoreService;
    @Mock private RecommendationReasonService recommendationReasonService;
    @Mock private SubstituteIngredientService substituteIngredientService;
    @Mock private RecipeConditionMapRepository recipeConditionMapRepository;
    @Mock private RecipeEmbeddingSearchService recipeEmbeddingSearchService;
    @Mock private HybridRankingService hybridRankingService;
    @Mock private RecommendationExplanationService recommendationExplanationService;
    @Mock private RecipeTagRepository recipeTagRepository;
    @Mock private RecommendationTagScoreService recommendationTagScoreService;

    @InjectMocks
    private RecommendationService service;

    @Test
    @DisplayName("알러지 포함 레시피는 제외된다")
    void excludeAllergyRecipe() {
        // given
        Recipe r1 = recipe(1L, "우유요리");
        Recipe r2 = recipe(2L, "두부요리");

        when(recipeRepository.findByIsActiveTrue())
                .thenReturn(List.of(r1, r2));

        when(recipeIngredientRepository.findRequiredIngredientNamesByRecipeId(1L))
                .thenReturn(List.of("우유"));
        when(recipeIngredientRepository.findRequiredIngredientNamesByRecipeId(2L))
                .thenReturn(List.of("두부"));

        when(allergyFilterService.containsAllergen(any(), any()))
                .thenReturn(true)   // r1 제외
                .thenReturn(false); // r2 통과

        // 필수 mock (안 해주면 NPE)
        defaultMocks();
        when(userConditionRepository.findByUser_UserIdAndIsActiveTrue(1L))
        .thenReturn(List.of(userCondition("ALLERGY_MILK")));

        // when
        PageResult<RecipeRecommendationResponse> result =
                service.recommend(
                        RecommendationQuery.builder()
                                .userId(1L)
                                .source("HOME")
                                .useUserProfile(true)
                                .useUserFridge(false)
                                .build(),
                        Pageable.unpaged()
                );

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).getRecipeId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("CHATBOT은 상위 3개만 LLM 설명이 포함된다")
    void onlyTop3HasLlmExplanation() {
        // given
        List<Recipe> recipes = List.of(
                recipe(1L, "r1"),
                recipe(2L, "r2"),
                recipe(3L, "r3"),
                recipe(4L, "r4")
        );

        when(recipeRepository.findByIsActiveTrue()).thenReturn(recipes);

        when(recommendationExplanationService.generateExplanation(any()))
                .thenReturn("설명");

        // 필수 mock
        defaultMocks();

        // when
        PageResult<RecipeRecommendationResponse> result =
                service.recommend(
                        RecommendationQuery.builder()
                                .source("CHATBOT")
                                .keywords(List.of("볶음"))
                                .build(),
                        Pageable.unpaged()
                );

        // then
        List<RecipeRecommendationResponse> content = result.content();

        assertThat(content.get(0).getLlmExplanation()).isNotNull();
        assertThat(content.get(1).getLlmExplanation()).isNotNull();
        assertThat(content.get(2).getLlmExplanation()).isNotNull();
        assertThat(content.get(3).getLlmExplanation()).isNull();
    }
    private UserCondition userCondition(String code) {
        ConditionCode conditionCode = ConditionCode.create(
                "ALLERGY",
                code,
                "알러지",
                "테스트"
        );

        UserCondition uc = new UserCondition();
        ReflectionTestUtils.setField(uc, "conditionCode", conditionCode);

        return uc;
    }
    // -------------------------------
    // 공통 mock 세팅 (NPE 방지)
    // -------------------------------
    private void defaultMocks() {
        when(userIngredientRepository.findOwnedIngredientNamesByUserId(any()))
                .thenReturn(List.of());

        when(recipeConditionMapRepository.findByRecipe_RecipeId(any()))
                .thenReturn(List.of());

        when(recipeTagRepository.findByRecipeIdIn(any()))
                .thenReturn(List.of());

        when(recommendationScoreService.calculateIngredientScore(anyInt(), anyInt()))
                .thenReturn(10.0);

        when(recommendationScoreService.calculateMatchRate(anyInt(), anyInt()))
                .thenReturn(50.0);

        when(recommendationScoreService.calculateTotalScore(anyDouble(), anyDouble()))
                .thenReturn(20.0);

        when(recommendationScoreService.calculateConditionScoreByCodes(any(), any()))
                .thenReturn(0.0);

        when(hybridRankingService.calculateHybridScore(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(30.0);

        when(recommendationReasonService.buildReason(anyDouble(), any(), any()))
                .thenReturn("추천 이유");

        when(substituteIngredientService.suggest(any(), any(), any()))
                .thenReturn(List.of());

        when(recommendationTagScoreService.calculateTagScore(any(), any()))
                .thenReturn(0.0);

        when(recipeEmbeddingSearchService.searchSimilarRecipes(any(), anyInt()))
                .thenReturn(List.of());
    }

    private Recipe recipe(Long id, String title) {
        return Recipe.builder()
                .recipeId(id)
                .title(title)
                .summary("요약")
                .build();
    }
}