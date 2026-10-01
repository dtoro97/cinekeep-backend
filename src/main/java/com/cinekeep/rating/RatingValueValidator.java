package com.cinekeep.rating;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RatingValueValidator implements ConstraintValidator<RatingValue, Double> {
    private static final double MIN_VALUE = 0.5;
    private static final double MAX_VALUE = 10.0;

    @Override
    public boolean isValid(Double value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        double halfSteps = value * 2;
        return value >= MIN_VALUE && value <= MAX_VALUE && halfSteps == Math.rint(halfSteps);
    }
}
