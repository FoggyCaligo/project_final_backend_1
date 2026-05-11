package com.today.fridge.global.external.fastapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecipeBrief(
    @JsonProperty("recipe_id") Long recipeId,
    @JsonProperty("title") String title,
    @JsonProperty("thumbnail_url") String thumbnailUrl
) {
}
