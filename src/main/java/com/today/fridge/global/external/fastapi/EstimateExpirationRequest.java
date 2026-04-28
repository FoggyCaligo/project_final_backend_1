package com.today.fridge.global.external.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EstimateExpirationRequest(
        @JsonProperty("name") String name,
        @JsonProperty("category_code") String categoryCode,
        @JsonProperty("storage_type") String storageType
) {}
