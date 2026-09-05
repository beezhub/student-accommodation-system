package com.beezhub.student_accommodation.config;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminBootstrapTest {

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminBootstrap bootstrap(String email, String password) {
        when(passwordEncoder.encode(anyString())).thenAnswer(i -> "encoded:" + i.getArgument(0));
        return new AdminBootstrap(appUserRepository, passwordEncoder, email, password);
    }

    private AppUser savedUser() {
        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void noEnvironmentVariables_doesNothing() {
        bootstrap("", "").run(null);

        verifyNoInteractions(appUserRepository);
    }

    @Test
    void emailWithoutPassword_doesNothing() {
        bootstrap("admin@example.com", "  ").run(null);

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void newAccount_createdAsPlatformAdminForcedToChangePassword() {
        when(appUserRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());

        bootstrap("  Admin@Example.com  ", "s3cret").run(null);

        AppUser created = savedUser();
        assertThat(created.getEmail()).isEqualTo("admin@example.com");
        assertThat(created.getUserRole()).isEqualTo(UserRole.PLATFORM_ADMIN);
        assertThat(created.getUserPassword()).isEqualTo("encoded:s3cret");
        assertThat(created.getMustChangePassword()).isTrue();
        assertThat(created.getEmailVerified()).isTrue();
        assertThat(created.getTenantId()).isEqualTo(TenantContext.PLATFORM_TENANT_ID);
    }

    @Test
    void existingAccount_passwordResetLockCearedAndChangeForcedAgain() {
        AppUser existing = new AppUser();
        existing.setId(4L);
        existing.setEmail("admin@example.com");
        existing.setUserRole(UserRole.PLATFORM_ADMIN);
        existing.setUserPassword("old-hash");
        existing.setMustChangePassword(false);
        existing.setAccountLockedUntil(LocalDateTime.of(9999, 12, 31, 23, 59, 59));
        existing.setFailedLoginAttempts(5);
        when(appUserRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));

        bootstrap("admin@example.com", "new-secret").run(null);

        AppUser saved = savedUser();
        assertThat(saved.getId()).isEqualTo(4L);
        assertThat(saved.getUserPassword()).isEqualTo("encoded:new-secret");
        assertThat(saved.getMustChangePassword()).isTrue();
        assertThat(saved.getAccountLockedUntil()).isNull();
        assertThat(saved.getFailedLoginAttempts()).isZero();
    }
}
