package com.today.fridge.chatbot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.today.fridge.chatbot.dto.request.ChatInterpretRequest;
import com.today.fridge.chatbot.dto.response.ChatInterpretResponse;
import com.today.fridge.chatbot.service.ChatbotOrchestratorService;
import com.today.fridge.chatbot.service.IntentParserService;
import com.today.fridge.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RequestMapping("/api/v1/chat")
@RestController
@RequiredArgsConstructor
public class ChatbotController {

	private final IntentParserService intentParserService;
	private final ChatbotOrchestratorService chatbotOrchestratorService;
	
	@PostMapping("/interpret")
	public ResponseEntity<ApiResponse<ChatInterpretResponse>> interpret(
	        @RequestBody ChatInterpretRequest request
	) {
	    ChatInterpretResponse response =
	            intentParserService.interpret(request);

	    return ResponseEntity.ok(
	            ApiResponse.success(response, "자연어 추천 질의 해석 성공")
	    );
	}
	
	@PostMapping("/recommend")
	public ResponseEntity<ApiResponse<?>> recommend(
	        @RequestBody ChatInterpretRequest request
	) {

	    return ResponseEntity.ok(
	            ApiResponse.success(
	                    chatbotOrchestratorService.recommendFromChat(request),
	                    "챗봇 기반 추천 성공"
	            )
	    );
	}
}
