package com.today.fridge.global.external.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record EstimateExpirationResponse(
        @JsonProperty("estimated_expiration_date") LocalDate estimatedExpirationDate,
        @JsonProperty("base_days") int baseDays,
        @JsonProperty("estimated_by") String estimatedBy,
        @JsonProperty("needs_review") boolean needsReview
) {}
