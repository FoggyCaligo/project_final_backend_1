package com.today.fridge.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.today.fridge.chatbot.dto.request.ChatInterpretRequest;
import com.today.fridge.chatbot.dto.response.ChatInterpretResponse;
import com.today.fridge.recipe.repository.RecipeIngredientRepository;
import com.today.fridge.recommendation.entity.ConditionCode;
import com.today.fridge.recommendation.repository.ConditionCodeRepository;

@ExtendWith(MockitoExtension.class)
class IntentParserServiceTest {

    @Mock private ConditionCodeRepository conditionCodeRepository;
    @Mock private RecipeIngredientRepository recipeIngredientRepository;

    @InjectMocks
    private IntentParserService service;

    @Test
    @DisplayName("저염식 요청 시 LOW_SODIUM condition 추출")
    void extractCondition_lowSodium() {
        ChatInterpretRequest request = request("저염식 국 추천해줘");

        when(conditionCodeRepository.findByIsActiveTrue())
                .thenReturn(List.of(
                        condition("LOW_SODIUM", "저염식")
                ));

        when(recipeIngredientRepository.findDistinctIngredientNames())
                .thenReturn(List.of());

        ChatInterpretResponse result = service.interpret(request);

        assertThat(result.getConditionTags())
                .contains("LOW_SODIUM");
    }

    @Test
    @DisplayName("재료 포함 요청 시 includeIngredients 추출")
    void extractIngredients() {
        ChatInterpretRequest request = request("두부 요리 추천해줘");

        when(conditionCodeRepository.findByIsActiveTrue())
                .thenReturn(List.of());

        when(recipeIngredientRepository.findDistinctIngredientNames())
                .thenReturn(List.of("두부", "계란"));

        ChatInterpretResponse result = service.interpret(request);

        assertThat(result.getIncludeIngredients())
                .contains("두부");
    }

    @Test
    @DisplayName("키워드 추출")
    void extractKeywords() {
        ChatInterpretRequest request = request("간단한 저녁 볶음 요리");

        when(conditionCodeRepository.findByIsActiveTrue())
                .thenReturn(List.of());

        when(recipeIngredientRepository.findDistinctIngredientNames())
                .thenReturn(List.of());

        ChatInterpretResponse result = service.interpret(request);

        assertThat(result.getKeywords())
                .contains("간단", "저녁", "볶음");
    }

    @Test
    @DisplayName("검색 키워드 포함 시 intent=search")
    void resolveIntent_search() {
        ChatInterpretRequest request = request("레시피 검색");

        when(conditionCodeRepository.findByIsActiveTrue())
                .thenReturn(List.of());

        when(recipeIngredientRepository.findDistinctIngredientNames())
                .thenReturn(List.of());

        ChatInterpretResponse result = service.interpret(request);

        assertThat(result.getIntent()).isEqualTo("search");
    }

    @Test
    @DisplayName("조건 + 재료 모두 있을 때 reasonHints 생성")
    void reasonHints_both() {
        ChatInterpretRequest request = request("저염식 두부 요리");

        when(conditionCodeRepository.findByIsActiveTrue())
                .thenReturn(List.of(
                        condition("LOW_SODIUM", "저염식")
                ));

        when(recipeIngredientRepository.findDistinctIngredientNames())
                .thenReturn(List.of("두부"));

        ChatInterpretResponse result = service.interpret(request);

        assertThat(result.getReasonHints())
                .contains("사용자 조건과 보유 재료를 함께 고려");
    }

    // ------------------------
    // helper
    // ------------------------
    private ChatInterpretRequest request(String text) {
        ChatInterpretRequest req = new ChatInterpretRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(req, "text", text);
        return req;
    }

    private ConditionCode condition(String code, String name) {
        return ConditionCode.create(
                "DIET",
                code,
                name,
                "설명"
        );
    }
}