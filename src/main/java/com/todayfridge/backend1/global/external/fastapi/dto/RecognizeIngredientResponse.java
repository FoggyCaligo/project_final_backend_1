package com.todayfridge.backend1.global.external.fastapi.dto;

import java.util.List;

public record RecognizeIngredientResponse(List<Candidate> candidates) {
    public record Candidate(String name, double confidence) {}
}
