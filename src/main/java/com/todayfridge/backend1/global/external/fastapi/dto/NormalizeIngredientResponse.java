package com.todayfridge.backend1.global.external.fastapi.dto;

import java.util.List;

public record NormalizeIngredientResponse(String normalizedName, double confidence, List<String> candidates) {}
