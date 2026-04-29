package com.today.fridge.ingredient.dto.vision;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IngredientImageCandidateDto {

    private String displayName;
    private String normalizedName;
    private String categorySuggestion;
    private Double confidence;
    private Object bbox;

    @JsonProperty("modelLabel")
    private String modelLabel;
}
