package com.today.fridge.vision.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/** GET .../recognize-image/status/{requestId} 응답 data */
@Value
@Builder
public class VisionRecognitionStatusDto {
    Long requestId;
    String status;
    Map<String, Object> analysisResult;
    Double confidenceScore;
    Long fileId;
    String completedAt;
}
