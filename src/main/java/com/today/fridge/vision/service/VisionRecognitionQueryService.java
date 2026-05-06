package com.today.fridge.vision.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.today.fridge.global.exception.BusinessException;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.vision.dto.VisionRecognitionStatusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisionRecognitionQueryService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public VisionRecognitionStatusDto getByRequestIdAndUser(Long requestId, Long userId) {
        List<VisionRecognitionStatusDto> rows =
                jdbcTemplate.query(
                        """
                                SELECT request_id,
                                       status::text AS status,
                                       analysis_result::text AS analysis_json,
                                       confidence_score,
                                       file_id,
                                       completed_at
                                FROM today_fridge.vision_recognition_request
                                WHERE request_id = ? AND user_id = ?
                                """,
                        (rs, rowNum) -> mapRow(rs),
                        requestId,
                        userId);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.VISION_RECOGNITION_NOT_FOUND);
        }
        return rows.get(0);
    }

    private VisionRecognitionStatusDto mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> analysis = parseAnalysis(rs.getString("analysis_json"));
        Timestamp completed = rs.getTimestamp("completed_at");
        return VisionRecognitionStatusDto.builder()
                .requestId(rs.getLong("request_id"))
                .status(rs.getString("status"))
                .analysisResult(analysis)
                .confidenceScore(
                        rs.getBigDecimal("confidence_score") != null
                                ? rs.getBigDecimal("confidence_score").doubleValue()
                                : null)
                .fileId(rs.getLong("file_id"))
                .completedAt(completed != null ? completed.toInstant().toString() : null)
                .build();
    }

    private Map<String, Object> parseAnalysis(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.singletonMap("raw", json);
        }
    }
}
