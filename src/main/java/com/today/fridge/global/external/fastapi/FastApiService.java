package com.today.fridge.global.external.fastapi;

import com.today.fridge.global.exception.BusinessException;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.ingredient.dto.vision.VisionRecognizeDataDto;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FastApiService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final RestTemplate restTemplate;

    @Value("${app.fastapi.base-url}")
    private String baseUrl;

    @Value("${app.fastapi.caller-service}")
    private String callerService;

    @Value("${app.fastapi.service-key}")
    private String serviceKey;

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

    /**
     * 식재료 이미지 인식 — FastAPI {@code POST /internal/v1/vision/recognize-ingredient-image} 프록시.
     * HTTP 200 + body.success=false 인 경우에도 {@link BusinessException} 처리.
     */
    public VisionRecognizeDataDto recognizeIngredientImage(MultipartFile file, int topK) {
        int k = Math.min(10, Math.max(1, topK));
        String url = baseUrl + "/internal/v1/vision/recognize-ingredient-image";

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.warn("[FastApiService] 이미지 읽기 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-Internal-Service", callerService);
        headers.set("X-Internal-Token", serviceKey);
        String rid = MDC.get("requestId");
        if (rid == null || rid.isBlank()) {
            rid = "spring_" + UUID.randomUUID();
        }
        headers.set("X-Request-Id", rid);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                String n = file.getOriginalFilename();
                return (n != null && !n.isBlank()) ? n : "upload.jpg";
            }
        };
        body.add("file", resource);
        body.add("topK", Integer.toString(k));
        body.add("detectMultiple", "false");
        body.add("source", "spring-fridge-api");

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<FastApiVisionEnvelope> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<FastApiVisionEnvelope>() {
                    }
            );
            FastApiVisionEnvelope env = response.getBody();
            if (env == null) {
                log.warn("[FastApiService] vision 응답 body null");
                throw new BusinessException(ErrorCode.AI_RECOGNITION_FAILED, "이미지 인식 응답이 비어 있습니다.");
            }
            if (!Boolean.TRUE.equals(env.getSuccess())) {
                String msg = env.getMessage() != null ? env.getMessage() : "vision failed";
                log.warn("[FastApiService] vision success=false code={} msg={}", env.getCode(), msg);
                throw new BusinessException(ErrorCode.AI_RECOGNITION_FAILED, msg);
            }
            VisionRecognizeDataDto data = env.getData();
            if (data == null) {
                throw new BusinessException(ErrorCode.AI_RECOGNITION_FAILED, "이미지 인식 data 없음");
            }
            data.setFastApiRequestId(env.getRequestId());
            return data;
        } catch (RestClientException e) {
            log.warn("[FastApiService] vision 호출 HTTP 실패: {}", e.getMessage());
            throw new BusinessException(
                    ErrorCode.AI_RECOGNITION_FAILED,
                    "이미지 인식 서버에 연결할 수 없습니다: " + e.getMessage());
        }
    }

    /** FastAPI vision 과 동일한 이미지 타입 허용 목록 검사 */
    public static void validateVisionImageContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE, "이미지 Content-Type 이 필요합니다.");
        }
        String ct = contentType.toLowerCase(Locale.ROOT).trim();
        if (!ALLOWED_IMAGE_TYPES.contains(ct)) {
            throw new BusinessException(
                    ErrorCode.INVALID_FILE_TYPE,
                    "지원 형식: JPEG, PNG, WebP 만 허용됩니다.");
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
