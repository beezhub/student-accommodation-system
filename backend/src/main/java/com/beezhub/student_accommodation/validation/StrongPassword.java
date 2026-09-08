package com.beezhub.student_accommodation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The single password policy for every field that sets or changes a password.
 *
 * <p>Signup, password reset and admin user creation previously carried only {@code @NotBlank}, so a
 * one-character password was accepted. Applying this annotation keeps the rule in one place, which
 * is what stops the four request objects drifting apart again.
 *
 * @see StrongPasswordValidator for the rules and the reason each one is there
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "Password must be at least 12 characters and include an uppercase letter,"
            + " a lowercase letter, a digit and a special character";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
