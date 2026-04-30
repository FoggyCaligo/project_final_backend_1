package com.today.fridge.global.external.fastapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.today.fridge.ingredient.dto.vision.VisionRecognizeDataDto;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FastApiVisionEnvelope {

    private Boolean success;
    private String code;
    private String message;
    private VisionRecognizeDataDto data;
    private String requestId;
}
