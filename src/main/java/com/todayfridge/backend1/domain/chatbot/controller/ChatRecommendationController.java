package com.todayfridge.backend1.domain.chatbot.controller;

import com.todayfridge.backend1.domain.chatbot.service.ChatRecommendationService;
import com.todayfridge.backend1.global.response.ApiResponse;
import com.todayfridge.backend1.global.security.CurrentUser;
import com.todayfridge.backend1.global.security.LoginUser;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatRecommendationController {
    private final ChatRecommendationService chatRecommendationService;

    public ChatRecommendationController(ChatRecommendationService chatRecommendationService) {
        this.chatRecommendationService = chatRecommendationService;
    }

    @PostMapping("/recommend")
    public ApiResponse<Map<String, Object>> recommend(@CurrentUser LoginUser loginUser, @RequestBody ChatRequest request) {
        return ApiResponse.success(chatRecommendationService.recommend(loginUser.getUserId(), request.message()));
    }

    public record ChatRequest(String message) {}
}
