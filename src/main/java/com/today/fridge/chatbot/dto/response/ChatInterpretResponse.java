package com.today.fridge.chatbot.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatInterpretResponse {
    private String intent;
    private List<String> conditionTags;
    private List<String> includeIngredients;
    private List<String> excludeIngredients;
    private List<String> keywords;
    private String sortHint;
    private Boolean needsClarification;
    private List<String> reasonHints;
    private String parsedQuerySummary;
    private Double confidence;
}