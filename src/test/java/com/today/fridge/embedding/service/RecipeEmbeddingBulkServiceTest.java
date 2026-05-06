package com.today.fridge.embedding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.today.fridge.embedding.client.EmbeddingClient;
import com.today.fridge.embedding.repository.RecipeEmbeddingRepository;
import com.today.fridge.recipe.entity.Recipe;
import com.today.fridge.recipe.repository.RecipeIngredientRepository;
import com.today.fridge.recipe.repository.RecipeRepository;

@ExtendWith(MockitoExtension.class)
class RecipeEmbeddingBulkServiceTest {

    @Mock private RecipeRepository recipeRepository;
    @Mock private RecipeEmbeddingRepository recipeEmbeddingRepository;
    @Mock private EmbeddingClient embeddingClient;
    @Mock private RecipeIngredientRepository recipeIngredientRepository;

    @InjectMocks
    private RecipeEmbeddingBulkService service;

    @Test
    @DisplayName("임베딩이 없는 레시피는 FastAPI 임베딩 생성 후 저장한다")
    void generateMissingEmbeddings_saveNewEmbedding() {
        Recipe recipe = recipe(1L, "두부볶음");

        when(recipeRepository.findAll())
                .thenReturn(List.of(recipe));

        when(recipeEmbeddingRepository.existsByRecipeRecipeIdAndModelName(
                1L,
                "all-MiniLM-L6-v2"
        )).thenReturn(false);

        when(recipeIngredientRepository.findRequiredIngredientNamesByRecipeId(1L))
                .thenReturn(List.of("두부", "대파"));

        when(embeddingClient.generateEmbedding(anyString()))
                .thenReturn(List.of(0.1, 0.2, 0.3));

        int count = service.generateMissingEmbeddings();

        assertThat(count).isEqualTo(1);

        verify(embeddingClient).generateEmbedding(anyString());

        verify(recipeEmbeddingRepository).insertEmbedding(
                eq(1L),
                anyString(),
                eq("[0.1,0.2,0.3]"),
                eq("all-MiniLM-L6-v2")
        );
    }

    @Test
    @DisplayName("이미 임베딩이 있으면 생성하지 않는다")
    void generateMissingEmbeddings_skipExistingEmbedding() {
        Recipe recipe = recipe(1L, "두부볶음");

        when(recipeRepository.findAll())
                .thenReturn(List.of(recipe));

        when(recipeEmbeddingRepository.existsByRecipeRecipeIdAndModelName(
                1L,
                "all-MiniLM-L6-v2"
        )).thenReturn(true);

        int count = service.generateMissingEmbeddings();

        assertThat(count).isEqualTo(0);

        verify(embeddingClient, never()).generateEmbedding(anyString());
        verify(recipeEmbeddingRepository, never())
                .insertEmbedding(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("여러 레시피 중 임베딩이 없는 레시피만 생성한다")
    void generateMissingEmbeddings_onlyMissingRecipes() {
        Recipe r1 = recipe(1L, "두부볶음");
        Recipe r2 = recipe(2L, "어묵국");

        when(recipeRepository.findAll())
                .thenReturn(List.of(r1, r2));

        when(recipeEmbeddingRepository.existsByRecipeRecipeIdAndModelName(
                1L,
                "all-MiniLM-L6-v2"
        )).thenReturn(true);

        when(recipeEmbeddingRepository.existsByRecipeRecipeIdAndModelName(
                2L,
                "all-MiniLM-L6-v2"
        )).thenReturn(false);

        when(recipeIngredientRepository.findRequiredIngredientNamesByRecipeId(2L))
                .thenReturn(List.of("어묵", "대파"));

        when(embeddingClient.generateEmbedding(anyString()))
                .thenReturn(List.of(0.4, 0.5));

        int count = service.generateMissingEmbeddings();

        assertThat(count).isEqualTo(1);

        verify(embeddingClient, times(1)).generateEmbedding(anyString());
        verify(recipeEmbeddingRepository, times(1))
                .insertEmbedding(
                        eq(2L),
                        anyString(),
                        eq("[0.4,0.5]"),
                        eq("all-MiniLM-L6-v2")
                );
    }

    @Test
    @DisplayName("embeddingText에 레시피명, 요약, 재료, 인분, 조리시간이 포함된다")
    void generateMissingEmbeddings_buildEmbeddingText() {
        Recipe recipe = Recipe.builder()
                .recipeId(1L)
                .title("두부볶음")
                .summary("간단한 두부 요리")
                .servingsText("2인분")
                .cookTimeText("10분")
                .build();

        when(recipeRepository.findAll())
                .thenReturn(List.of(recipe));

        when(recipeEmbeddingRepository.existsByRecipeRecipeIdAndModelName(
                1L,
                "all-MiniLM-L6-v2"
        )).thenReturn(false);

        when(recipeIngredientRepository.findRequiredIngredientNamesByRecipeId(1L))
                .thenReturn(List.of("두부", "대파"));

        when(embeddingClient.generateEmbedding(anyString()))
                .thenReturn(List.of(0.1, 0.2));

        service.generateMissingEmbeddings();

        verify(embeddingClient).generateEmbedding(argThat(text ->
                text.contains("레시피명: 두부볶음")
                        && text.contains("요약: 간단한 두부 요리")
                        && text.contains("재료: 두부, 대파")
                        && text.contains("인분: 2인분")
                        && text.contains("조리시간: 10분")
        ));
    }

    private Recipe recipe(Long id, String title) {
        return Recipe.builder()
                .recipeId(id)
                .title(title)
                .summary("요약")
                .servingsText("1인분")
                .cookTimeText("15분")
                .build();
    }
}