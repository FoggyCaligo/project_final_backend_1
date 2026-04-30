package com.today.fridge.ingredient.dto.vision;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VisionRouteInfoDto {

    private String type;
    private Double confidence;
    private Boolean needsReview;
    private String reason;
    private Map<String, Object> probabilities;
}
