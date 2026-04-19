package com.todayfridge.backend1.domain.chatbot.service;

import com.todayfridge.backend1.global.external.fastapi.FastApiClient;
import com.todayfridge.backend1.global.external.fastapi.dto.ChatInterpretResponse;
import org.springframework.stereotype.Component;

@Component
public class ChatInterpretFacade {
    private final FastApiClient fastApiClient;

    public ChatInterpretFacade(FastApiClient fastApiClient) {
        this.fastApiClient = fastApiClient;
    }

    public ChatInterpretResponse interpret(String message) {
        return fastApiClient.interpretChat(message);
    }
}
