package com.today.fridge.global.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(5));   // 연결
        factory.setReadTimeout(Duration.ofSeconds(120));    // 응답 대기 (LLM 때문에 길게)

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}