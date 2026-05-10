package com.today.fridge.embedding.client;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.today.fridge.embedding.dto.EmbeddingRequest;
import com.today.fridge.embedding.dto.EmbeddingResponse;
import com.today.fridge.llm.config.LlmClientProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmbeddingClient {

    private final RestClient restClient;
    private final LlmClientProperties properties;

    public List<Double> generateEmbedding(
            String text
    ) {

        EmbeddingResponse response =
                restClient.post()
                        .uri(properties.baseUrl()+"/api/v1/embedding")
                        .body(
                                new EmbeddingRequest(text)
                        )
                        .retrieve()
                        .body(
                                EmbeddingResponse.class
                        );

        return response.embedding();
    }
}