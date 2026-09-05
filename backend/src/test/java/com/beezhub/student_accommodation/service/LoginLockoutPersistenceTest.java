package com.beezhub.student_accommodation.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GH-117: {@code validateLogin} saves the incremented failed-attempt counter and then rethrows
 * {@link BadCredentialsException}. Spring rolls back on any unchecked exception, so without
 * {@code noRollbackFor} that save was discarded and the database lockout never took effect,
 * leaving brute-force protection entirely dependent on Redis.
 *
 * <p>The behaviour cannot be observed from a Mockito test, because the rollback is performed by
 * the transaction proxy rather than by the method itself: {@code AuthServiceTest} asserts the save
 * happens and passes with or without the fix. This pins the annotation instead, so the guarantee
 * cannot be removed silently. End-to-end proof lives in the Testcontainers suite.
 */
class LoginLockoutPersistenceTest {

    @Test
    void validateLogin_doesNotRollBackOnBadCredentials() throws NoSuchMethodException {
        Method validateLogin = AuthService.class.getMethod("validateLogin", String.class, String.class);
        Transactional transactional = validateLogin.getAnnotation(Transactional.class);

        assertThat(transactional)
                .as("validateLogin must be transactional")
                .isNotNull();
        assertThat(transactional.noRollbackFor())
                .as("the failed-attempt counter must survive the rethrown BadCredentialsException")
                .contains(BadCredentialsException.class);
    }

    @Test
    void unlockAccount_remainsFullyTransactional() throws NoSuchMethodException {
        // The counter reset must still roll back as a unit if anything in it fails.
        Method unlockAccount = AuthService.class.getMethod("unlockAccount", Long.class);

        assertThat(unlockAccount.getAnnotation(Transactional.class)).isNotNull();
        assertThat(unlockAccount.getAnnotation(Transactional.class).noRollbackFor()).isEmpty();
    }
}
