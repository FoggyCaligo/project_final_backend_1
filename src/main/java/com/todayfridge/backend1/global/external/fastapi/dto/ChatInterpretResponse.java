package com.todayfridge.backend1.global.external.fastapi.dto;

import java.util.List;
import java.util.Map;

public record ChatInterpretResponse(String intentType, List<String> ingredients, Map<String, Object> filters, List<String> excludedIngredients) {}
