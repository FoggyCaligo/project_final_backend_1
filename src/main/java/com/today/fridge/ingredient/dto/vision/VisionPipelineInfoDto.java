package com.today.fridge.ingredient.dto.vision;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VisionPipelineInfoDto {

    private String stage;
    private String source;
    private Boolean detectMultipleRequested;
    private Boolean effectiveDetectMultiple;
    private Integer requestedTopK;
    private Integer effectiveTopK;
}
