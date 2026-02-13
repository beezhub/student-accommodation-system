package com.beezhub.student_accommodation.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator implementation for {@link NumericYearFormat}.
 * Validates that a year of study string can be parsed to a numeric year value.
 */
public class NumericYearFormatValidator implements ConstraintValidator<NumericYearFormat, String> {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    private static final Set<String> VALID_ORDINAL_PATTERNS = Set.of(
            "first", "one", "1st",
            "second", "two", "2nd",
            "third", "three", "3rd",
            "fourth", "four", "4th",
            "fifth", "five", "5th",
            "sixth", "six", "6th",
            "seventh", "seven", "7th",
            "eighth", "eight", "8th",
            "ninth", "nine", "9th",
            "tenth", "ten", "10th"
    );

    @Override
    public void initialize(NumericYearFormat constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // Let @NotBlank handle null/empty validation
        }

        String normalized = value.toLowerCase().trim();

        // Check direct numeric parse
        try {
            int numericValue = Integer.parseInt(normalized);
            return numericValue > 0;
        } catch (NumberFormatException ignored) {
            // Not a pure number, continue with other checks
        }

        // Check if value contains any valid ordinal pattern
        for (String pattern : VALID_ORDINAL_PATTERNS) {
            if (normalized.contains(pattern)) {
                return true;
            }
        }

        // Check for embedded number (e.g., "Year 3", "Level 2")
        return NUMBER_PATTERN.matcher(normalized).find();
    }
}
