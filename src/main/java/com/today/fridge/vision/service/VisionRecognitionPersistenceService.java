package com.today.fridge.vision.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.today.fridge.file.entity.FileAsset;
import com.today.fridge.ingredient.dto.vision.AnomalyAnalysisDto;
import com.today.fridge.ingredient.dto.vision.VisionRecognizeDataDto;
import com.today.fridge.vision.dto.VisionPersistResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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
     * {@code file_asset} 행 삽입 → 디스크({@code vision/{file_id}.ext}) 기록 → {@code vision_recognition_request} 적재.
     * 디스크 또는 VRR 단계 실패 시 물리 파일 삭제·트랜잭션 롤백으로 {@code file_asset} 행도 제거된다.
     */
    @Transactional(rollbackFor = Exception.class)
    public VisionPersistResult persistAfterRecognition(
            Long userId,
            byte[] imageBytes,
            String originalFilename,
            String contentType,
            VisionRecognizeDataDto data) {
        if (!persistRecognition) {
            return VisionPersistResult.skipped();
        }
        FileAsset fa = null;
        Path writtenAbs = null;
        try {
            fa =
                    visionFileAssetService.insertRowAndWriteVisionFile(
                            userId, imageBytes, originalFilename, contentType);
            writtenAbs = visionFileAssetService.absolutePathForStoredFile(fa);

            if (data != null) {
                data.setFileId(fa.getFileId());
            }
            Map<String, Object> payload = toAnalysisPayload(data);
            String json = objectMapper.writeValueAsString(payload);
            BigDecimal conf = confidenceFrom(data);

            Long requestId =
                    jdbcTemplate.queryForObject(
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
            return VisionPersistResult.success(
                    fa.getFileId(), Objects.requireNonNull(requestId, "request_id"));
        } catch (Exception e) {
            log.warn("[VisionRecognition] 저장 실패 userId={}: {}", userId, e.toString());
            Path toDelete = writtenAbs;
            if (toDelete == null && fa != null) {
                toDelete = visionFileAssetService.absolutePathForStoredFile(fa);
            }
            visionFileAssetService.deletePhysicalFileQuietly(toDelete);
            if (data != null) {
                data.setFileId(null);
            }
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return VisionPersistResult.failed();
        }
    }

    private Map<String, Object> toAnalysisPayload(VisionRecognizeDataDto data) {
        Map<String, Object> m =
                data == null
                        ? new LinkedHashMap<>()
                        : objectMapper.convertValue(data, new TypeReference<>() {});
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
