package com.today.fridge.ingredient.dto.vision;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VisionRecognizeDataDto {

    private List<IngredientImageCandidateDto> recognizedCandidates;
    private VisionRouteInfoDto route;
    private VisionPipelineInfoDto pipeline;
    private Boolean needsReview;

    /** DL/휴리스틱 이상 분석 요약 — 인식 응답에 포함 */
    private AnomalyAnalysisDto anomalyAnalysis;

    /** FastAPI가 내려준 요청 ID (내부 추적용) */
    private String fastApiRequestId;

    /** DB vision_recognition_request.request_id (저장 성공 시) */
    private Long recognitionRequestId;

    /** 인식 시 저장된 {@code file_asset.file_id} (persist 시 설정) */
    private Long fileId;
}
