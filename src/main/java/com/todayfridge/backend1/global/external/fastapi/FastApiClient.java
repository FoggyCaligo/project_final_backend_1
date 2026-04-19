package com.todayfridge.backend1.global.external.fastapi;

import com.todayfridge.backend1.global.external.fastapi.dto.*;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FastApiClient {
    private final FastApiProperties properties;

    public FastApiClient(FastApiProperties properties) {
        this.properties = properties;
    }

    public NormalizeIngredientResponse normalizeIngredient(String rawName) {
        String normalized = rawName == null ? "" : rawName.trim();
        return new NormalizeIngredientResponse(normalized, 0.0, List.of(normalized));
    }

    public CategorizeIngredientResponse categorizeIngredient(String normalizedName) {
        return new CategorizeIngredientResponse("기타", 0.0);
    }

    public RecognizeIngredientResponse recognizeIngredient(MultipartFile file) {
        return new RecognizeIngredientResponse(List.of());
    }

    public ChatInterpretResponse interpretChat(String message) {
        return new ChatInterpretResponse("RECIPE_RECOMMENDATION", List.of(), Map.of(), List.of());
    }

    public String getBaseUrl() { return properties.getBaseUrl(); }
}
