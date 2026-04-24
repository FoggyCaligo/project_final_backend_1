package com.today.fridge.ingredient.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FreshnessCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 24);

    @Test
    void unknownWhenNoDate() {
        assertThat(FreshnessCalculator.computeStatus(null, TODAY)).isEqualTo("UNKNOWN");
    }

    @Test
    void expiredWhenBeforeToday() {
        assertThat(FreshnessCalculator.computeStatus(TODAY.minusDays(1), TODAY)).isEqualTo("EXPIRED");
    }

    @Test
    void soonWhenWithinThreeDaysInclusive() {
        assertThat(FreshnessCalculator.computeStatus(TODAY, TODAY)).isEqualTo("SOON");
        assertThat(FreshnessCalculator.computeStatus(TODAY.plusDays(3), TODAY)).isEqualTo("SOON");
    }

    @Test
    void freshWhenAfterSoonWindow() {
        assertThat(FreshnessCalculator.computeStatus(TODAY.plusDays(4), TODAY)).isEqualTo("FRESH");
    }
}
