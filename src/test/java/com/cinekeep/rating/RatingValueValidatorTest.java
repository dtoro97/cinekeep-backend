package com.cinekeep.rating;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RatingValueValidatorTest {
    private final RatingValueValidator validator = new RatingValueValidator();

    @ParameterizedTest
    @ValueSource(doubles = {0.5, 1.0, 6.5, 9.5, 10.0})
    void acceptsHalfStepsBetweenHalfAndTen(double value) {
        assertThat(validator.isValid(value, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0, 7.3, 10.5, 11.0})
    void rejectsValuesOutsideRangeOrStep(double value) {
        assertThat(validator.isValid(value, null)).isFalse();
    }

    @Test
    void leavesNullToNotNull() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
