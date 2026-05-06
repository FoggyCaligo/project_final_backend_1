package com.today.fridge.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.today.fridge.embedding.client.EmbeddingClient;
import com.today.fridge.embedding.entity.RecipeEmbedding;
import com.today.fridge.embedding.repository.RecipeEmbeddingRepository;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.recommendation.entity.ConditionCode;
import com.today.fridge.recommendation.entity.RecipeConditionMap;
import com.today.fridge.recommendation.repository.ConditionCodeRepository;
import com.today.fridge.recommendation.repository.RecipeConditionMapRepository;

@ExtendWith(MockitoExtension.class)
class RecipeConditionAnalyzeServiceTest {

    @Mock private RecipeRepository recipeRepository;
    @Mock private ConditionCodeRepository conditionCodeRepository;
    @Mock private RecipeConditionMapRepository recipeConditionMapRepository;
    @Mock private RecipeEmbeddingRepository recipeEmbeddingRepository;
    @Mock private EmbeddingClient embeddingClient;

    @InjectMocks
    private RecipeConditionAnalyzeService service;

    // -------------------------------
    // 1. RECOMMENDED 저장
    // -------------------------------
    @Test
    @DisplayName("similarity가 threshold 이상이면 RECOMMENDED 저장")
    void recommendedSaved() {
        Recipe recipe = recipe(1L, "두부 요리");

        ConditionCode condition = condition("DIET_LOW_CALORIE", "다이어트 음식");

        RecipeEmbedding embedding = embedding(recipe,"[1.0, 0.0]");

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
        when(recipeEmbeddingRepository.findByRecipe_RecipeIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(embedding));
        when(conditionCodeRepository.findByIsActiveTrue())
                .thenReturn(List.of(condition));

        when(embeddingClient.generateEmbedding(any()))
                .thenReturn(List.of(1.0, 0.0)); // similarity = 1

        service.analyzeAndSave(1L);

        verify(recipeConditionMapRepository).save(any());
    }

    // -------------------------------
    // 2. threshold 미만 → 저장 안됨
    // -------------------------------
    @Test
    @DisplayName("similarity가 threshold 미만이면 저장되지 않는다")
    void belowThreshold_notSaved() {
        Recipe recipe = recipe(1L, "두부 요리");

        ConditionCode condition = condition("DIET_LOW_CALORIE", "다이어트 음식");

        RecipeEmbedding embedding = embedding(recipe,"[1.0, 0.0]");

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
        when(recipeEmbeddingRepository.findByRecipe_RecipeIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(embedding));
        when(conditionCodeRepository.findByIsActiveTrue())
                .thenReturn(List.of(condition));

        when(embeddingClient.generateEmbedding(any()))
                .thenReturn(List.of(0.1, 0.9)); // similarity 낮음

        service.analyzeAndSave(1L);

        verify(recipeConditionMapRepository, never()).save(any());
    }

    // -------------------------------
    // 3. DIET reject (베이컨 등)
    // -------------------------------
    @Test
    @DisplayName("고칼로리 키워드 포함 시 저장되지 않는다")
    void dietRejected() {
        Recipe recipe = recipe(1L, "베이컨 요리");

        ConditionCode condition = condition("DIET_LOW_CALORIE", "다이어트");

        RecipeEmbedding embedding = embedding(recipe,"[1.0, 0.0]");

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
        when(recipeEmbeddingRepository.findByRecipe_RecipeIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(embedding));
        when(conditionCodeRepository.findByIsActiveTrue())
                .thenReturn(List.of(condition));

        when(embeddingClient.generateEmbedding(any()))
                .thenReturn(List.of(1.0, 0.0)); // similarity 높음

        service.analyzeAndSave(1L);

        verify(recipeConditionMapRepository, never()).save(any());
    }

    // -------------------------------
    // 4. 기존 데이터 update
    // -------------------------------
    @Test
    @DisplayName("이미 존재하면 update 수행")
    void updateExisting() {
        Recipe recipe = recipe(1L, "두부 요리");

        ConditionCode condition = condition("DIET_LOW_CALORIE", "다이어트");

        RecipeEmbedding embedding = embedding(recipe,"[1.0, 0.0]");

        RecipeConditionMap existing = mock(RecipeConditionMap.class);

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
        when(recipeEmbeddingRepository.findByRecipe_RecipeIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(embedding));
        when(conditionCodeRepository.findByIsActiveTrue())
                .thenReturn(List.of(condition));

        when(embeddingClient.generateEmbedding(any()))
                .thenReturn(List.of(1.0, 0.0));

        when(recipeConditionMapRepository
                .findByRecipe_RecipeIdAndConditionCode_ConditionId(any(), any()))
                .thenReturn(Optional.of(existing));

        service.analyzeAndSave(1L);

        verify(existing).updateAnalysis(any(), any(), any());
        verify(recipeConditionMapRepository).save(existing);
    }
    @Test
    @DisplayName("ALLERGY 조건은 분석에서 제외된다")
    void skipAllergyCondition() {
        Recipe recipe = recipe(1L, "두부 요리");

        ConditionCode condition = ConditionCode.create(
                "ALLERGY",
                "ALLERGY_MILK",
                "우유 알러지",
                "우유"
        );

        RecipeEmbedding embedding = embedding(recipe, "[1.0, 0.0]");

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
        when(recipeEmbeddingRepository.findByRecipe_RecipeIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(embedding));
        when(conditionCodeRepository.findByIsActiveTrue())
                .thenReturn(List.of(condition));

        service.analyzeAndSave(1L);

        verify(recipeConditionMapRepository, never()).save(any());
    }
    // -------------------------------
    // helper
    // -------------------------------
    private Recipe recipe(Long id, String title) {
        return Recipe.builder()
                .recipeId(id)
                .title(title)
                .summary("요약")
                .build();
    }

    private ConditionCode condition(String code, String desc) {
        return ConditionCode.create(
                "DIET",
                code,
                "조건",
                desc
        );
    }

    private RecipeEmbedding embedding(Recipe recipe, String vector) {
        return RecipeEmbedding.create(
                recipe,
                "test text",
                vector,
                "test-model"
        );
    }
}