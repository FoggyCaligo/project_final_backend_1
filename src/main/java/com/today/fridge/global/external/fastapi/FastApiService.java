package com.today.fridge.global.external.fastapi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Slf4j
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

    /**
     * 유통기한 미입력 식재료에 대해 카테고리+보관방식 기반 추정일 반환.
     * FastAPI 호출 실패 시 null 반환 (Spring 규칙엔진이 fallback 처리).
     */
    public EstimateExpirationResponse estimateExpiration(String name, String categoryCode, String storageType) {
        String url = baseUrl + "/api/v1/ingredient/estimate-expiration";
        EstimateExpirationRequest req = new EstimateExpirationRequest(name, categoryCode, storageType);
        try {
            return restTemplate.postForObject(url, req, EstimateExpirationResponse.class);
        } catch (RestClientException e) {
            log.warn("[FastApiService] 유통기한 추정 호출 실패 - name={}, categoryCode={}: {}", name, categoryCode, e.getMessage());
            return null;
        }
    }

    /** FastAPI 응답 없을 때 Spring 자체 fallback (보관방식 기반 보수 기본값) */
    public static LocalDate fallbackExpiration(String storageType) {
        int days = switch (storageType == null ? "" : storageType.toUpperCase()) {
            case "FROZEN" -> 30;
            case "REFRIGERATED" -> 5;
            default -> 2;
        };
        return LocalDate.now().plusDays(days);
    }
}