package com.today.fridge.embedding.util;

import java.util.List;
import java.util.stream.Collectors;

public final class VectorUtils {

    private VectorUtils() {
    }

    public static String toVectorLiteral(List<Double> vector) {
        if (vector == null || vector.isEmpty()) {
            throw new IllegalArgumentException("임베딩 벡터가 비어 있습니다.");
        }

        return "[" +
                vector.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","))
                + "]";
    }
}