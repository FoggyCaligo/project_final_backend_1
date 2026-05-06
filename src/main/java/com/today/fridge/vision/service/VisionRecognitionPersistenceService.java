package com.today.fridge.vision.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.today.fridge.file.entity.FileAsset;
import com.today.fridge.ingredient.dto.vision.AnomalyAnalysisDto;
import com.today.fridge.ingredient.dto.vision.VisionRecognizeDataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisionRecognitionPersistenceService {

    private final JdbcTemplate jdbcTemplate;
    private final VisionFileAssetService visionFileAssetService;
    private final ObjectMapper objectMapper;

    @Value("${app.vision.persist-recognition:true}")
    private boolean persistRecognition;

    /**
     * 인식 직후 업로드 파일을 저장하고 vision_recognition_request.analysis_result(JSONB)에 전체 응답을 적재한다.
     * 파일 저장은 {@link VisionFileAssetService}(REQUIRES_NEW)에서 커밋되어 VRR INSERT 실패 시에도 file_asset 행이 유지된다.
     */
    @Transactional
    public Optional<Long> persistAfterRecognition(
            Long userId,
            byte[] imageBytes,
            String originalFilename,
            String contentType,
            VisionRecognizeDataDto data) {
        if (!persistRecognition) {
            return Optional.empty();
        }
        try {
            FileAsset fa =
                    visionFileAssetService.saveVisionFileForUser(
                            userId, imageBytes, originalFilename, contentType);

            if (data != null) {
                data.setFileId(fa.getFileId());
            }

            Map<String, Object> payload = toAnalysisPayload(data);
            String json = objectMapper.writeValueAsString(payload);
            BigDecimal conf = confidenceFrom(data);

            Long requestId = jdbcTemplate.queryForObject(
                    """
                            INSERT INTO today_fridge.vision_recognition_request
                            (user_id, file_id, status, capture_source, analysis_result, confidence_score, completed_at)
                            VALUES (?, ?, CAST(? AS today_fridge.vision_status), CAST(? AS today_fridge.capture_source), ?::jsonb, ?, CURRENT_TIMESTAMP)
                            RETURNING request_id
                            """,
                    Long.class,
                    userId,
                    fa.getFileId(),
                    "COMPLETED",
                    "USER_MANUAL",
                    json,
                    conf);
            return Optional.ofNullable(requestId);
        } catch (Exception e) {
            log.warn("[VisionRecognition] 저장 실패 userId={}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, Object> toAnalysisPayload(VisionRecognizeDataDto data) {
        Map<String, Object> m = objectMapper.convertValue(data, new TypeReference<>() {});
        m.put("schemaVersion", 1);
        m.put("kind", "recognize_image_sync");
        return new LinkedHashMap<>(m);
    }

    private static BigDecimal confidenceFrom(VisionRecognizeDataDto data) {
        if (data == null || data.getAnomalyAnalysis() == null) {
            return null;
        }
        AnomalyAnalysisDto a = data.getAnomalyAnalysis();
        if (a.getScore() == null) {
            return null;
        }
        return BigDecimal.valueOf(a.getScore()).setScale(2, RoundingMode.HALF_UP);
    }
}
