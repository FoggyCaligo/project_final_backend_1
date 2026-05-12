package com.today.fridge.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.today.fridge.chatbot.dto.request.ChatInterpretRequest;
import com.today.fridge.chatbot.dto.response.ChatInterpretResponse;
import com.today.fridge.global.response.PageResponse;
import com.today.fridge.global.response.PageResult;
import com.today.fridge.recommendation.dto.internal.RecommendationQuery;
import com.today.fridge.recommendation.dto.response.RecipeRecommendationResponse;
import com.today.fridge.recommendation.entity.ConditionCode;
import com.today.fridge.recommendation.entity.UserCondition;
import com.today.fridge.recommendation.repository.UserConditionRepository;
import com.today.fridge.recommendation.service.RecommendationService;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class ChatbotOrchestratorServiceTest {

    @Mock private IntentParserService intentParserService;
    @Mock private RecommendationService recommendationService;
    @Mock private UserConditionRepository userConditionRepository;

    @InjectMocks
    private ChatbotOrchestratorService service;

    @Test
    @DisplayName("비회원 채팅 추천은 사용자 프로필과 냉장고를 사용하지 않는다")
    void recommendFromChat_guest() {
        ChatInterpretRequest request = request(null, "두부 볶음 추천해줘");

        when(intentParserService.interpret(request))
                .thenReturn(parsed(
                        List.of("두부"),
                        List.of("볶음"),
                        List.of("LOW_SODIUM")
                ));

        when(recommendationService.recommend(any(RecommendationQuery.class), any(Pageable.class)))
                .thenReturn(pageResult(List.of(response(1L), response(2L))));

        service.recommendFromChat(request);

        ArgumentCaptor<RecommendationQuery> captor =
                ArgumentCaptor.forClass(RecommendationQuery.class);

        verify(recommendationService).recommend(captor.capture(), any(Pageable.class));

        RecommendationQuery query = captor.getValue();

        assertThat(query.getUserId()).isNull();
        assertThat(query.isUseUserProfile()).isFalse();
        assertThat(query.isUseUserFridge()).isFalse();
        assertThat(query.getIncludeIngredients()).containsExactly("두부");
        assertThat(query.getConditionCodes()).containsExactly("LOW_SODIUM");
        assertThat(query.getKeywords()).contains("두부 볶음 추천해줘", "볶음");
        assertThat(query.getSource()).isEqualTo("CHATBOT");
    }

    @Test
    @DisplayName("회원 채팅 추천은 프로필 조건을 파싱 조건과 병합한다")
    void recommendFromChat_memberMergeProfileConditions() {
        ChatInterpretRequest request = request(1L, "저염식 국 추천해줘");

        when(intentParserService.interpret(request))
                .thenReturn(parsed(
                        List.of(),
                        List.of("국"),
                        List.of("LOW_SODIUM")
                ));

        when(userConditionRepository.findByUser_UserIdAndIsActiveTrue(1L))
                .thenReturn(List.of(userCondition("ALLERGY_MILK")));

        when(recommendationService.recommend(any(RecommendationQuery.class), any(Pageable.class)))
                .thenReturn(pageResult(List.of(response(1L))));

        service.recommendFromChat(request);

        ArgumentCaptor<RecommendationQuery> captor =
                ArgumentCaptor.forClass(RecommendationQuery.class);

        verify(recommendationService).recommend(captor.capture(), any(Pageable.class));

        RecommendationQuery query = captor.getValue();

        assertThat(query.getUserId()).isEqualTo(1L);
        assertThat(query.isUseUserProfile()).isTrue();
        assertThat(query.isUseUserFridge()).isTrue();
        assertThat(query.getConditionCodes())
                .containsExactlyInAnyOrder("LOW_SODIUM", "ALLERGY_MILK");
        assertThat(query.getSource()).isEqualTo("CHATBOT");
    }

    @Test
    @DisplayName("챗봇 추천 결과는 최대 3개만 반환한다")
    void recommendFromChat_limit3() {
        ChatInterpretRequest request = request(null, "볶음 추천해줘");

        when(intentParserService.interpret(request))
                .thenReturn(parsed(
                        List.of(),
                        List.of("볶음"),
                        List.of()
                ));

        when(recommendationService.recommend(any(RecommendationQuery.class), any(Pageable.class)))
                .thenReturn(pageResult(List.of(
                        response(1L),
                        response(2L),
                        response(3L),
                        response(4L)
                )));

        List<RecipeRecommendationResponse> result =
                service.recommendFromChat(request);

        assertThat(result).hasSize(3);
        assertThat(result)
                .extracting(RecipeRecommendationResponse::getRecipeId)
                .containsExactly(1L, 2L, 3L);
    }

    private ChatInterpretRequest request(Long userId, String text) {
        ChatInterpretRequest request = new ChatInterpretRequest();
        ReflectionTestUtils.setField(request, "userId", userId);
        ReflectionTestUtils.setField(request, "text", text);
        return request;
    }

    private ChatInterpretResponse parsed(
            List<String> includeIngredients,
            List<String> keywords,
            List<String> conditionTags
    ) {
        return ChatInterpretResponse.builder()
                .includeIngredients(includeIngredients)
                .keywords(keywords)
                .conditionTags(conditionTags)
                .excludeIngredients(List.of())
                .sortHint("hybrid")
                .build();
    }

    private PageResult<RecipeRecommendationResponse> pageResult(
            List<RecipeRecommendationResponse> content
    ) {
        return new PageResult<>(
                content,
                new PageResponse(
                        content.size(),
                        1,
                        0,
                        content.size()
                )
        );
    }

    private RecipeRecommendationResponse response(Long recipeId) {
        return RecipeRecommendationResponse.builder()
                .recipeId(recipeId)
                .title("레시피" + recipeId)
                .build();
    }

    private UserCondition userCondition(String code) {
        ConditionCode conditionCode = ConditionCode.create(
                "ALLERGY",
                code,
                "알러지",
                "테스트"
        );

        UserCondition userCondition = new UserCondition();
        ReflectionTestUtils.setField(userCondition, "conditionCode", conditionCode);

        return userCondition;
    }
}