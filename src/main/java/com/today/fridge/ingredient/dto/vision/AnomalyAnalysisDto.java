package com.today.fridge.ingredient.dto.vision;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 이미지 인식 파이프라인의 이상 징후 요약(FastAPI {@code anomalyAnalysis}).
 * DL 전용 모델 없이도 휴리스틱으로 채워질 수 있다.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnomalyAnalysisDto {

    /** OK | SUSPICIOUS | NEEDS_REVIEW */
    private String status;

    /** 0~1, 높을수록 파이프라인 결과 신뢰 가능 */
    private Double score;

    private List<String> signals;

    private String modelVersion;

    /** 이미지 기반 경량 DL·신호량 특성 (블러·노출 등). 선택 */
    private Map<String, Object> dlAnomaly;
}
