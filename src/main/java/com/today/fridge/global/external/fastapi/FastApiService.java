package com.today.fridge.global.external.fastapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FastApiService {

    private final RestTemplate restTemplate;

    @Value("${app.fastapi.base-url}")
    private String baseUrl;

    public FastApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String health() {
        String url = baseUrl + "/api/health";
        return restTemplate.getForObject(url, String.class);
    }
}