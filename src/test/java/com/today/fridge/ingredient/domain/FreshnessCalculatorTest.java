package com.today.fridge.ingredient.domain;

import com.today.fridge.ingredient.type.FreshnessStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FreshnessCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 24);

    @Test
    void unknownWhenNoDate() {
        assertThat(FreshnessCalculator.computeStatus(null, TODAY)).isEqualTo(FreshnessStatus.UNKNOWN);
    }

    @Test
    void expiredWhenBeforeToday() {
        assertThat(FreshnessCalculator.computeStatus(TODAY.minusDays(1), TODAY)).isEqualTo(FreshnessStatus.EXPIRED);
    }

    @Test
    void soonWhenWithinThreeDaysInclusive() {
        assertThat(FreshnessCalculator.computeStatus(TODAY, TODAY)).isEqualTo(FreshnessStatus.SOON);
        assertThat(FreshnessCalculator.computeStatus(TODAY.plusDays(3), TODAY)).isEqualTo(FreshnessStatus.SOON);
    }

    @Test
    void freshWhenAfterSoonWindow() {
        assertThat(FreshnessCalculator.computeStatus(TODAY.plusDays(4), TODAY)).isEqualTo(FreshnessStatus.FRESH);
    }
}
