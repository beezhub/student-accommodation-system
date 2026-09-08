package com.beezhub.student_accommodation.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Enforces the password policy described by {@link StrongPassword}.
 *
 * <p>Twelve characters with four character classes is the floor recommended in the UAT-gate review
 * (GH-118). A maximum is also enforced: BCrypt silently ignores anything past 72 bytes, so a longer
 * password would give the user a false sense of strength and make the tail meaningless.
 *
 * <p>A null or blank value is accepted here so the field's own {@code @NotBlank} produces the
 * "required" message rather than the policy text.
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    static final int MIN_LENGTH = 12;
    /** BCrypt truncates beyond 72 bytes, so anything longer is not actually protecting the account. */
    static final int MAX_LENGTH = 72;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            return true;
        }
        if (password.length() < MIN_LENGTH || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_LENGTH) {
            return false;
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else {
                special = true;
            }
        }
        return upper && lower && digit && special;
    }
}
