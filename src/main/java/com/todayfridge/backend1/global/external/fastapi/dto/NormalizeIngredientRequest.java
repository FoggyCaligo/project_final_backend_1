package com.todayfridge.backend1.global.external.fastapi.dto;

import java.util.Map;

public record NormalizeIngredientRequest(String rawName, Map<String, Object> context) {}
