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

    /** FastAPI가 내려준 요청 ID (내부 추적용) */
    private String fastApiRequestId;
}
