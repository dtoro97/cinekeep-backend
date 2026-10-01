package com.cinekeep.rating;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = RatingValueValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface RatingValue {
    String message() default "must be between 0.5 and 10 in steps of 0.5";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
