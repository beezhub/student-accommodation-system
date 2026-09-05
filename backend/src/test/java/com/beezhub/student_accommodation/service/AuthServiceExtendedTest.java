package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.*;
import com.beezhub.student_accommodation.mapper.AppUserMapper;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.security.jwt.JwtUtil;
import com.beezhub.student_accommodation.security.user.AppUserDetails;
import com.beezhub.student_accommodation.service.tenant.TenantService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Extended unit tests for {@link AuthService} covering methods not exercised by
 * {@link AuthServiceTest}: password-reset flow, email-verification flow, me/refreshToken,
 * getCurrentUser, and anonymizeAccount.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceExtendedTest {

    // ─── core deps already present in AuthServiceTest ───────────────────────────
    @Mock private AppUserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private AppUserMapper appUserMapper;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private LoginAttemptService loginAttemptService;
    @Mock private TenantService tenantService;
    @Mock private MeterRegistry meterRegistry;
    @Mock private Counter counter;

    // ─── additional deps required by the new methods ─────────────────────────────
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private EmailService emailService;
    @Mock private StudentRepository studentRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private RoomAllocationRepository roomAllocationRepository;
    @Mock private WaitingListRepository waitingListRepository;
    @Mock private UserNotificationRepository userNotificationRepository;
    @Mock private ActivityLogRepository activityLogRepository;
    @Mock private UserAnonymizationService userAnonymizationService;

    @InjectMocks
    private AuthService authService;

    private AppUser user;
    private AppUserDetails userDetails;

    @BeforeEach
    void setUp() {
        user = AppUser.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("Bob")
                .lastName("Jones")
                .userPassword("encoded")
                .userRole(UserRole.STUDENT)
                .emailVerified(false)
                .failedLoginAttempts(0)
                .build();

        userDetails = new AppUserDetails(
                user.getEmail(), user.getUserPassword(), user.getUserRole().name(), 0L);

        lenient().when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);

        // Inject @Value fields that Spring would normally populate
        ReflectionTestUtils.setField(authService, "baseUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(authService, "passwordResetExpiryHours", 24);
        ReflectionTestUtils.setField(authService, "emailVerificationExpiryHours", 24);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ─────────────────────────────────────────────────────────────
    // me
    // ─────────────────────────────────────────────────────────────

    @Test
    void me_authenticatedUser_returnsUserData() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        UserData expected = new UserData();
        when(appUserMapper.toUserData(user)).thenReturn(expected);

        UserData result = authService.me(auth);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void me_userNotFound_throwsUserNotFoundException() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("missing@example.com");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me(auth))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ─────────────────────────────────────────────────────────────
    // refreshToken
    // ─────────────────────────────────────────────────────────────

    @Test
    void refreshToken_reissuesToKenForPrincipal() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(jwtUtil.generateToken(eq(userDetails), anyBoolean())).thenReturn("new-token");
        when(userRepository.findByEmail(userDetails.getUsername())).thenReturn(Optional.of(user));
        UserData userData = new UserData();
        when(appUserMapper.toUserData(user)).thenReturn(userData);

        LoginResponse response = authService.refreshToken(auth);

        assertThat(response.getToken()).isEqualTo("new-token");
        assertThat(response.getUser()).isSameAs(userData);
    }

    // ─────────────────────────────────────────────────────────────
    // logout (null auth)
    // ─────────────────────────────────────────────────────────────

    @Test
    void logout_nullAuthentication_returnsWithoutError() {
        authService.logout(null);

        verifyNoInteractions(tokenBlacklistService);
    }

    // ─────────────────────────────────────────────────────────────
    // getCurrentUser
    // ─────────────────────────────────────────────────────────────

    @Test
    void getCurrentUser_authenticated_returnsUser() {
        // 3-arg constructor sets isAuthenticated()=true
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test@example.com", null, Collections.emptyList()));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        AppUser result = authService.getCurrentUser();

        assertThat(result).isSameAs(user);
    }

    @Test
    void getCurrentUser_notAuthenticated_throwsUserNotFoundException() {
        // No authentication set in SecurityContextHolder
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> authService.getCurrentUser())
                .isInstanceOf(UserNotFoundException.class);
    }

    // ─────────────────────────────────────────────────────────────
    // anonymizeAccount
    // ─────────────────────────────────────────────────────────────

    @Test
    void anonymizeAccount_delegatesToAnonymizationService() {
        authService.anonymizeAccount(1L);

        verify(userAnonymizationService).anonymizeUser(1L);
    }

    // ─────────────────────────────────────────────────────────────
    // requestPasswordReset
    // ─────────────────────────────────────────────────────────────

    @Test
    void requestPasswordReset_userNotFound_returnsGenericMessageWithoutToken() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        // Anti user-enumeration: unknown emails get the same success message
        // as known ones, and no reset token is created.
        var response = authService.requestPasswordReset("unknown@test.com");

        assertThat(response.getMessage())
                .isEqualTo("Password reset instructions have been sent to your email");
        verifyNoInteractions(passwordResetTokenRepository);
    }

    @Test
    void requestPasswordReset_success_savesTokenAndSendsEmail() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MessageResponse result = authService.requestPasswordReset("test@example.com");

        assertThat(result.getMessage()).contains("sent");
        verify(passwordResetTokenRepository).deleteByUserId(user.getId());
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendSimpleEmail(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    void requestPasswordReset_emailFails_throwsRuntimeException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP error"))
                .when(emailService).sendSimpleEmail(any(), any(), any());

        assertThatThrownBy(() -> authService.requestPasswordReset("test@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send password reset email");
    }

    // ─────────────────────────────────────────────────────────────
    // verifyResetToken
    // ─────────────────────────────────────────────────────────────

    @Test
    void verifyResetToken_tokenNotFound_returnsInvalid() {
        when(passwordResetTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        TokenValidationResponse result = authService.verifyResetToken("bad-token");

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void verifyResetToken_tokenAlreadyUsed_returnsInvalid() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("used-token").user(user).expiryDate(LocalDateTime.now().plusHours(1)).used(true).build();
        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        TokenValidationResponse result = authService.verifyResetToken("used-token");

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void verifyResetToken_tokenExpired_returnsInvalid() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("expired").user(user).expiryDate(LocalDateTime.now().minusHours(1)).used(false).build();
        when(passwordResetTokenRepository.findByToken("expired")).thenReturn(Optional.of(token));

        TokenValidationResponse result = authService.verifyResetToken("expired");

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void verifyResetToken_validToken_returnsValid() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("good").user(user).expiryDate(LocalDateTime.now().plusHours(24)).used(false).build();
        when(passwordResetTokenRepository.findByToken("good")).thenReturn(Optional.of(token));

        TokenValidationResponse result = authService.verifyResetToken("good");

        assertThat(result.isValid()).isTrue();
    }

    // ─────────────────────────────────────────────────────────────
    // resetPassword
    // ─────────────────────────────────────────────────────────────

    @Test
    void resetPassword_tokenNotFound_throwsInvalidTokenException() {
        when(passwordResetTokenRepository.findByToken("none")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("none", "newPass"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void resetPassword_tokenAlreadyUsed_throwsInvalidTokenException() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("used").user(user).expiryDate(LocalDateTime.now().plusHours(1)).used(true).build();
        when(passwordResetTokenRepository.findByToken("used")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.resetPassword("used", "newPass"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void resetPassword_tokenExpired_throwsTokenExpiredException() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("exp").user(user).expiryDate(LocalDateTime.now().minusHours(1)).used(false).build();
        when(passwordResetTokenRepository.findByToken("exp")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.resetPassword("exp", "newPass"))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void resetPassword_validToken_resetsPasswordAndMarksTokenUsed() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("valid").user(user).expiryDate(LocalDateTime.now().plusHours(1)).used(false).build();
        when(passwordResetTokenRepository.findByToken("valid")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPass")).thenReturn("encoded-new");
        when(userRepository.save(user)).thenReturn(user);
        when(passwordResetTokenRepository.save(token)).thenReturn(token);

        MessageResponse result = authService.resetPassword("valid", "newPass");

        assertThat(result.getMessage()).contains("reset");
        assertThat(user.getUserPassword()).isEqualTo("encoded-new");
        assertThat(token.isUsed()).isTrue();
    }

    // ─────────────────────────────────────────────────────────────
    // sendVerificationEmail
    // ─────────────────────────────────────────────────────────────

    @Test
    void sendVerificationEmail_userNotFound_throwsUserNotFoundException() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.sendVerificationEmail("nobody@example.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void sendVerificationEmail_alreadyVerified_throwsEmailAlreadyVerifiedException() {
        user.setEmailVerified(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.sendVerificationEmail("test@example.com"))
                .isInstanceOf(EmailAlreadyVerifiedException.class);
    }

    @Test
    void sendVerificationEmail_success_savesTokenAndSendsEmail() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MessageResponse result = authService.sendVerificationEmail("test@example.com");

        assertThat(result.getMessage()).contains("sent");
        verify(emailVerificationTokenRepository).deleteByUserId(user.getId());
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendSimpleEmail(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    void sendVerificationEmail_emailFails_throwsRuntimeException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP down"))
                .when(emailService).sendSimpleEmail(any(), any(), any());

        assertThatThrownBy(() -> authService.sendVerificationEmail("test@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send verification email");
    }

    // ─────────────────────────────────────────────────────────────
    // verifyEmail
    // ─────────────────────────────────────────────────────────────

    @Test
    void verifyEmail_tokenNotFound_throwsInvalidTokenException() {
        when(emailVerificationTokenRepository.findByToken("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("bad"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void verifyEmail_tokenAlreadyUsed_throwsInvalidTokenException() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("used").user(user).expiryDate(LocalDateTime.now().plusHours(1)).used(true).build();
        when(emailVerificationTokenRepository.findByToken("used")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyEmail("used"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void verifyEmail_tokenExpired_throwsTokenExpiredException() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("exp").user(user).expiryDate(LocalDateTime.now().minusHours(1)).used(false).build();
        when(emailVerificationTokenRepository.findByToken("exp")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyEmail("exp"))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void verifyEmail_alreadyVerified_throwsEmailAlreadyVerifiedException() {
        user.setEmailVerified(true);
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("tok").user(user).expiryDate(LocalDateTime.now().plusHours(1)).used(false).build();
        when(emailVerificationTokenRepository.findByToken("tok")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyEmail("tok"))
                .isInstanceOf(EmailAlreadyVerifiedException.class);
    }

    @Test
    void verifyEmail_validToken_verifiesEmailAndMarksTokenUsed() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("good").user(user).expiryDate(LocalDateTime.now().plusHours(1)).used(false).build();
        when(emailVerificationTokenRepository.findByToken("good")).thenReturn(Optional.of(token));
        when(userRepository.save(user)).thenReturn(user);
        when(emailVerificationTokenRepository.save(token)).thenReturn(token);

        MessageResponse result = authService.verifyEmail("good");

        assertThat(result.getMessage()).contains("verified");
        assertThat(user.getEmailVerified()).isTrue();
        assertThat(token.isUsed()).isTrue();
    }
}
