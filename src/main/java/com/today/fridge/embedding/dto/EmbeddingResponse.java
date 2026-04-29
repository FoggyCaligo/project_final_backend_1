package com.today.fridge.embedding.dto;

import java.util.List;

public record EmbeddingResponse(
        Integer dimension,
        List<Double> embedding
) {}

