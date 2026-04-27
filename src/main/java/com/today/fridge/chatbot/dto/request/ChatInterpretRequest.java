package com.today.fridge.chatbot.dto.request;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatInterpretRequest {
	private Long userId;
    private String text;
    private List<String> ownedIngredients;
    private String locale;
    private Map<String, Object> sessionContext;
}