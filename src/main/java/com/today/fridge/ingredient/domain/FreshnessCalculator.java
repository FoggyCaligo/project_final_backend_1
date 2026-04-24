package com.today.fridge.ingredient.domain;

import java.time.LocalDate;

/**
 * 계획서 06_신선도규칙_확정표_초안 — 팀 승인 전 착수용.
 */
public final class FreshnessCalculator {

    private FreshnessCalculator() {
    }

    public static final int SOON_DAYS_INCLUSIVE = 3;

    public static String computeStatus(LocalDate expiresAt, LocalDate today) {
        if (expiresAt == null) {
            return "UNKNOWN";
        }
        if (expiresAt.isBefore(today)) {
            return "EXPIRED";
        }
        LocalDate soonEnd = today.plusDays(SOON_DAYS_INCLUSIVE);
        if (!expiresAt.isAfter(soonEnd)) {
            return "SOON";
        }
        return "FRESH";
    }
}
