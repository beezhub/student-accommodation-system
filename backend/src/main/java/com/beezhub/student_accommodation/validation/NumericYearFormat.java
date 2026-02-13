package com.beezhub.student_accommodation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a year of study value can be parsed to a numeric year.
 * Accepts formats like: "1", "First Year", "1st Year", "Year 1", etc.
 * Rejects non-numeric values like: "undergrad", "postgrad", "senior", "junior".
 */
@Documented
@Constraint(validatedBy = NumericYearFormatValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NumericYearFormat {
    String message() default "Year of study must be in a numeric format (e.g., '1', '1st Year', 'First Year', 'Year 2')";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
