package com.today.fridge.llm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.today.fridge.llm.client.FastApiLlmClient;
import com.today.fridge.llm.dto.request.RecommendationExplanationContext;
import com.today.fridge.llm.dto.response.RecommendationExplainResponse;

@ExtendWith(MockitoExtension.class)
class RecommendationExplanationServiceTest {

    @Mock
    private FastApiLlmClient fastApiLlmClient;

    @InjectMocks
    private RecommendationExplanationService service;

    @Test
    @DisplayName("FastAPI LLM 설명 응답을 반환한다")
    void generateExplanation() {
        RecommendationExplanationContext context =
                mock(RecommendationExplanationContext.class);

        when(fastApiLlmClient.explainRecommendation(context))
                .thenReturn(new RecommendationExplainResponse("추천 설명"));

        String result = service.generateExplanation(context);

        assertThat(result).isEqualTo("추천 설명");
    }
}
