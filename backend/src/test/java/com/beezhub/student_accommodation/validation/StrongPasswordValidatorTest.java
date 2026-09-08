package com.beezhub.student_accommodation.validation;

import com.beezhub.student_accommodation.model.dto.ChangePasswordRequest;
import com.beezhub.student_accommodation.model.dto.ResetPasswordRequest;
import com.beezhub.student_accommodation.model.dto.SignupRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class StrongPasswordValidatorTest {

    private static final String VALID = "Str0ng!Passw0rd";

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    private boolean accepts(String password) {
        return validator.isValid(password, null);
    }

    @Test
    void acceptsAPasswordMeetingEveryRule() {
        assertThat(accepts(VALID)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "a",                 // the one-character password the issue calls out
            "Short1!",           // under the minimum length
            "alllowercase1!",    // no uppercase
            "ALLUPPERCASE1!",    // no lowercase
            "NoDigitsHere!!",    // no digit
            "NoSpecials12345"    // no special character
    })
    void rejectsPasswordsThatBreakTheRules(String password) {
        assertThat(accepts(password)).isFalse();
    }

    @Test
    void rejectsPasswordsBeyondTheBcryptLimit() {
        // BCrypt ignores everything past 72 bytes, so accepting more would be misleading.
        String tooLong = "Aa1!".repeat(19); // 76 characters

        assertThat(accepts(tooLong)).isFalse();
    }

    @Test
    void acceptsExactlyTheBcryptLimit() {
        String atLimit = "Aa1!".repeat(18); // 72 characters

        assertThat(accepts(atLimit)).isTrue();
    }

    @Test
    void leavesBlankValuesToNotBlank() {
        // Returning true here keeps the "required" message rather than the policy text.
        assertThat(accepts(null)).isTrue();
        assertThat(accepts("  ")).isTrue();
    }

    // ── the constraint is actually wired onto the request objects ────────────

    @Test
    void requestObjectsRejectWeakPasswords() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator beanValidator = factory.getValidator();

            SignupRequest signup = new SignupRequest();
            signup.setEmail("student@example.com");
            signup.setFirstName("Test");
            signup.setLastName("User");
            signup.setPassword("a");
            assertThat(beanValidator.validate(signup)).isNotEmpty();

            ChangePasswordRequest change = new ChangePasswordRequest();
            change.setCurrentPassword("whatever");
            change.setNewPassword("a");
            assertThat(beanValidator.validate(change)).isNotEmpty();

            ResetPasswordRequest reset = new ResetPasswordRequest();
            reset.setNewPassword("a");
            assertThat(beanValidator.validate(reset)).isNotEmpty();
        }
    }

    @Test
    void requestObjectsAcceptAStrongPassword() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator beanValidator = factory.getValidator();

            ChangePasswordRequest change = new ChangePasswordRequest();
            change.setCurrentPassword("whatever");
            change.setNewPassword(VALID);

            assertThat(beanValidator.validate(change)).isEmpty();
        }
    }
}
