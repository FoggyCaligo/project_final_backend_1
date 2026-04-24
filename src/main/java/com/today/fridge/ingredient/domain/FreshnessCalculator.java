package com.today.fridge.ingredient.domain;

import com.today.fridge.ingredient.type.FreshnessStatus;

import java.time.LocalDate;

/**
 * 냉장고 CRUD API 작업 시작 문서 v1.0 §6.
 */
public final class FreshnessCalculator {

    private FreshnessCalculator() {
    }

    public static final int SOON_DAYS_INCLUSIVE = 3;

    public static FreshnessStatus computeStatus(LocalDate expiresAt, LocalDate today) {
        if (expiresAt == null) {
            return FreshnessStatus.UNKNOWN;
        }
        if (expiresAt.isBefore(today)) {
            return FreshnessStatus.EXPIRED;
        }
        LocalDate soonEnd = today.plusDays(SOON_DAYS_INCLUSIVE);
        if (!expiresAt.isAfter(soonEnd)) {
            return FreshnessStatus.SOON;
        }
        return FreshnessStatus.FRESH;
    }
}
