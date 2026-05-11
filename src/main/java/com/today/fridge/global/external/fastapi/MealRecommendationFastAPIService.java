package com.today.fridge.global.external.fastapi;

import com.today.fridge.global.exception.BusinessException;
import com.today.fridge.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@Service
public class MealRecommendationFastAPIService {

    private final RestTemplate restTemplate;

    @Value("${app.fastapi.base-url}")
    private String baseUrl;

    @Value("${app.fastapi.caller-service}")
    private String callerService;

    @Value("${app.fastapi.service-key}")
    private String serviceKey;

    public MealRecommendationFastAPIService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public MealRecommendationResponse getMealRecommendation(MealRecommendationRequest req) {
        String url = baseUrl + "/api/v1/meal-recommendation";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Service", callerService);
        headers.set("X-Internal-Token", serviceKey);
        String rid = MDC.get("requestId");
        if (rid == null || rid.isBlank()) {
            rid = "spring_" + UUID.randomUUID();
        }
        headers.set("X-Request-Id", rid);

        HttpEntity<MealRecommendationRequest> entity = new HttpEntity<>(req, headers);

        try {
            ResponseEntity<FastApiMealEnvelope> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    FastApiMealEnvelope.class
            );
            FastApiMealEnvelope env = response.getBody();
            if (env == null) {
                log.warn("[MealRecommendationFastAPIService] AI 식단 추천 응답 body null");
                throw new BusinessException(ErrorCode.AI_RECOGNITION_FAILED, "AI 식단 추천 응답이 비어 있습니다.");
            }

            if (!Boolean.TRUE.equals(env.getSuccess())) {
                String msg = env.getMessage() != null ? env.getMessage() : "AI 식단 추천 실패";
                log.warn("[MealRecommendationFastAPIService] AI 식단 추천 success=false code={} msg={}", env.getCode(), msg);
                throw new BusinessException(ErrorCode.AI_RECOGNITION_FAILED, msg);
            }

            MealRecommendationResponse data = env.getData();
            if (data == null) {
                throw new BusinessException(ErrorCode.AI_RECOGNITION_FAILED, "AI 식단 추천 데이터가 없습니다.");
            }
            return data;
        } catch (RestClientException e) {
            log.warn("[MealRecommendationFastAPIService] AI 식단 추천 호출 실패 url={} userId={}: {}", url, req.userId(), e.getMessage());
            throw new BusinessException(ErrorCode.AI_RECOGNITION_FAILED, "AI 식단 추천 서버에 연결할 수 없습니다: " + e.getMessage());
        }
    }
}
