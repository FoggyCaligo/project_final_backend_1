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
import com.today.fridge.embedding.dto.SemanticSearchResult;
import com.today.fridge.embedding.repository.RecipeEmbeddingRepository;

@ExtendWith(MockitoExtension.class)
class RecipeEmbeddingSearchServiceTest {

    @Mock private EmbeddingClient embeddingClient;
    @Mock private RecipeEmbeddingRepository recipeEmbeddingRepository;

    @InjectMocks
    private RecipeEmbeddingSearchService service;

    @Test
    @DisplayName("queryText가 null이면 빈 리스트 반환")
    void searchSimilarRecipes_nullQuery() {
        List<SemanticSearchResult> result =
                service.searchSimilarRecipes(null, 10);

        assertThat(result).isEmpty();
        verifyNoInteractions(embeddingClient, recipeEmbeddingRepository);
    }

    @Test
    @DisplayName("queryText가 blank이면 빈 리스트 반환")
    void searchSimilarRecipes_blankQuery() {
        List<SemanticSearchResult> result =
                service.searchSimilarRecipes("   ", 10);

        assertThat(result).isEmpty();
        verifyNoInteractions(embeddingClient, recipeEmbeddingRepository);
    }

    @Test
    @DisplayName("limit이 0 이하이면 기본값 10으로 검색한다")
    void searchSimilarRecipes_defaultLimit() {
        when(embeddingClient.generateEmbedding("두부 요리"))
                .thenReturn(List.of(0.1, 0.2));

        when(recipeEmbeddingRepository.findSimilarRecipes(
                "[0.1,0.2]",
                "all-MiniLM-L6-v2",
                10
        )).thenReturn(List.of());

        List<SemanticSearchResult> result =
                service.searchSimilarRecipes("두부 요리", 0);

        assertThat(result).isEmpty();

        verify(recipeEmbeddingRepository).findSimilarRecipes(
                "[0.1,0.2]",
                "all-MiniLM-L6-v2",
                10
        );
    }

    @Test
    @DisplayName("검색 성공")
    void searchSimilarRecipes_success() {
        when(embeddingClient.generateEmbedding("저염식 국 요리"))
                .thenReturn(List.of(0.3, 0.4));

        List<SemanticSearchResult> expected =
                List.of(result(1L, 0.25));

        when(recipeEmbeddingRepository.findSimilarRecipes(
                "[0.3,0.4]",
                "all-MiniLM-L6-v2",
                5
        )).thenReturn(expected);

        List<SemanticSearchResult> result =
                service.searchSimilarRecipes("저염식 국 요리", 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRecipeId()).isEqualTo(1L);
        assertThat(result.get(0).getDistance()).isEqualTo(0.25);
    }
    private SemanticSearchResult result(Long id, double distance) {
        SemanticSearchResult r = mock(SemanticSearchResult.class);
        when(r.getRecipeId()).thenReturn(id);
        when(r.getDistance()).thenReturn(distance);
        return r;
    }
}