package com.today.fridge.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.today.fridge.llm.client.RecipeTagClient;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.entity.RecipeTag;
import com.today.fridge.recipe.entity.RecipeTagSourceType;
import com.today.fridge.recipe.entity.RecipeTagType;
import com.today.fridge.recipe.repository.RecipeIngredientRepository;
import com.today.fridge.recipe.repository.RecipeRepository;
import com.today.fridge.recipe.repository.RecipeTagRepository;

@ExtendWith(MockitoExtension.class)
class RecipeTagBulkServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private RecipeIngredientRepository recipeIngredientRepository;

    @Mock
    private RecipeTagRepository recipeTagRepository;

    @Mock
    private RecipeTagClient recipeTagClient;

    @InjectMocks
    private RecipeTagBulkService recipeTagBulkService;

    @Test
    @DisplayName("LLM 태그가 없으면 분류 결과를 저장한다")
    void generateMissingTags_saveLlmTags() {
        Recipe recipe = recipe(1L, "초간단 어묵국", "요약");

        when(recipeRepository.findByIsActiveTrue()).thenReturn(List.of(recipe));
        when(recipeTagRepository.existsByRecipeIdAndSourceType(1L, RecipeTagSourceType.LLM))
                .thenReturn(false);
        when(recipeIngredientRepository.findRequiredIngredientNamesByRecipeId(1L))
                .thenReturn(List.of("어묵", "대파"));
        when(recipeTagClient.classify(any()))
                .thenReturn(new RecipeTagClient.RecipeTagClassifyResponse(
                        1L,
                        List.of(
                                new RecipeTagClient.RecipeTagDto("COOKING_TYPE", "SOUP", 0.9, "LLM"),
                                new RecipeTagClient.RecipeTagDto("STYLE", "REFRESHING", 0.8, "LLM")
                        )
                ));

        int savedCount = recipeTagBulkService.generateMissingTags();

        assertThat(savedCount).isEqualTo(2);
        verify(recipeTagRepository, times(2)).save(any(RecipeTag.class));
    }

    @Test
    @DisplayName("이미 LLM 태그가 있으면 분류 호출 없이 건너뛴다")
    void generateMissingTags_skipWhenLlmTagExists() {
        Recipe recipe = recipe(1L, "초간단 어묵국", "요약");

        when(recipeRepository.findByIsActiveTrue()).thenReturn(List.of(recipe));
        when(recipeTagRepository.existsByRecipeIdAndSourceType(1L, RecipeTagSourceType.LLM))
                .thenReturn(true);

        int savedCount = recipeTagBulkService.generateMissingTags();

        assertThat(savedCount).isEqualTo(0);
        verify(recipeTagClient, never()).classify(any());
        verify(recipeTagRepository, never()).save(any());
    }

    @Test
    @DisplayName("UNKNOWN 태그는 저장하지 않는다")
    void generateMissingTags_ignoreUnknownTag() {
        Recipe recipe = recipe(1L, "애매한 요리", "요약");

        when(recipeRepository.findByIsActiveTrue()).thenReturn(List.of(recipe));
        when(recipeTagRepository.existsByRecipeIdAndSourceType(1L, RecipeTagSourceType.LLM))
                .thenReturn(false);
        when(recipeIngredientRepository.findRequiredIngredientNamesByRecipeId(1L))
                .thenReturn(List.of("재료"));
        when(recipeTagClient.classify(any()))
                .thenReturn(new RecipeTagClient.RecipeTagClassifyResponse(
                        1L,
                        List.of(new RecipeTagClient.RecipeTagDto("COOKING_TYPE", "UNKNOWN", 0.5, "LLM"))
                ));

        int savedCount = recipeTagBulkService.generateMissingTags();

        assertThat(savedCount).isEqualTo(0);
        verify(recipeTagRepository, never()).save(any());
    }

    @Test
    @DisplayName("COOKING_TYPE은 기존 타입을 삭제하고 새 태그를 저장한다")
    void generateMissingTags_replaceCookingType() {
        Recipe recipe = recipe(1L, "어묵볶음", "요약");

        when(recipeRepository.findByIsActiveTrue()).thenReturn(List.of(recipe));
        when(recipeTagRepository.existsByRecipeIdAndSourceType(1L, RecipeTagSourceType.LLM))
                .thenReturn(false);
        when(recipeIngredientRepository.findRequiredIngredientNamesByRecipeId(1L))
                .thenReturn(List.of("어묵"));
        when(recipeTagRepository.existsByRecipeIdAndTagTypeAndTagCode(
                1L, RecipeTagType.COOKING_TYPE, "STIR_FRY"
        )).thenReturn(false);
        when(recipeTagClient.classify(any()))
                .thenReturn(new RecipeTagClient.RecipeTagClassifyResponse(
                        1L,
                        List.of(new RecipeTagClient.RecipeTagDto("COOKING_TYPE", "STIR_FRY", 0.9, "LLM"))
                ));

        int savedCount = recipeTagBulkService.generateMissingTags();

        assertThat(savedCount).isEqualTo(1);
        verify(recipeTagRepository).deleteByRecipeIdAndTagType(1L, RecipeTagType.COOKING_TYPE);
        verify(recipeTagRepository).save(any(RecipeTag.class));
    }

    @Test
    @DisplayName("STYLE 태그가 이미 있으면 중복 저장하지 않는다")
    void generateMissingTags_skipDuplicateStyle() {
        Recipe recipe = recipe(1L, "매운 어묵볶음", "요약");

        when(recipeRepository.findByIsActiveTrue()).thenReturn(List.of(recipe));
        when(recipeTagRepository.existsByRecipeIdAndSourceType(1L, RecipeTagSourceType.LLM))
                .thenReturn(false);
        when(recipeIngredientRepository.findRequiredIngredientNamesByRecipeId(1L))
                .thenReturn(List.of("어묵", "고추장"));
        when(recipeTagRepository.existsByRecipeIdAndTagTypeAndTagCode(
                1L, RecipeTagType.STYLE, "SPICY"
        )).thenReturn(true);
        when(recipeTagClient.classify(any()))
                .thenReturn(new RecipeTagClient.RecipeTagClassifyResponse(
                        1L,
                        List.of(new RecipeTagClient.RecipeTagDto("STYLE", "SPICY", 0.9, "LLM"))
                ));

        int savedCount = recipeTagBulkService.generateMissingTags();

        assertThat(savedCount).isEqualTo(0);
        verify(recipeTagRepository, never()).save(any());
    }

    @Test
    @DisplayName("저장되는 RecipeTag 값이 올바르다")
    void generateMissingTags_saveCorrectRecipeTagValues() {
        Recipe recipe = recipe(1L, "초간단 어묵국", "요약");

        when(recipeRepository.findByIsActiveTrue()).thenReturn(List.of(recipe));
        when(recipeTagRepository.existsByRecipeIdAndSourceType(1L, RecipeTagSourceType.LLM))
                .thenReturn(false);
        when(recipeIngredientRepository.findRequiredIngredientNamesByRecipeId(1L))
                .thenReturn(List.of("어묵"));
        when(recipeTagClient.classify(any()))
                .thenReturn(new RecipeTagClient.RecipeTagClassifyResponse(
                        1L,
                        List.of(new RecipeTagClient.RecipeTagDto("COOKING_TYPE", "SOUP", 0.9, "LLM"))
                ));

        recipeTagBulkService.generateMissingTags();

        ArgumentCaptor<RecipeTag> captor = ArgumentCaptor.forClass(RecipeTag.class);
        verify(recipeTagRepository).save(captor.capture());

        RecipeTag saved = captor.getValue();

        assertThat(saved.getRecipeId()).isEqualTo(1L);
        assertThat(saved.getTagType()).isEqualTo(RecipeTagType.COOKING_TYPE);
        assertThat(saved.getTagCode()).isEqualTo("SOUP");
        assertThat(saved.getConfidence()).isEqualTo(0.9);
        assertThat(saved.getSourceType()).isEqualTo(RecipeTagSourceType.LLM);
    }
    @Test
    @DisplayName("LLM 응답이 null이면 저장하지 않는다")
    void generateMissingTags_nullResponse() {
        Recipe recipe = recipe(1L, "요리", "요약");

        when(recipeRepository.findByIsActiveTrue()).thenReturn(List.of(recipe));
        when(recipeTagRepository.existsByRecipeIdAndSourceType(1L, RecipeTagSourceType.LLM))
                .thenReturn(false);
        when(recipeIngredientRepository.findRequiredIngredientNamesByRecipeId(1L))
                .thenReturn(List.of("재료"));
        when(recipeTagClient.classify(any())).thenReturn(null);

        int savedCount = recipeTagBulkService.generateMissingTags();

        assertThat(savedCount).isEqualTo(0);
        verify(recipeTagRepository, never()).save(any());
    }
    private Recipe recipe(Long id, String title, String summary) {
        return Recipe.builder()
                .recipeId(id)
                .title(title)
                .summary(summary)
                .build();
    }
}