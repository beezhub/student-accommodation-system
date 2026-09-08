package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.EmailAlreadyRegisteredException;
import com.beezhub.student_accommodation.exceptions.UserNotFoundException;
import com.beezhub.student_accommodation.mapper.AppUserMapper;
import com.beezhub.student_accommodation.model.dto.LoginResponse;
import com.beezhub.student_accommodation.model.dto.SignupRequest;
import com.beezhub.student_accommodation.model.dto.UserData;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.security.jwt.JwtUtil;
import com.beezhub.student_accommodation.security.user.AppUserDetails;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import com.beezhub.student_accommodation.service.tenant.TenantService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.beezhub.student_accommodation.exceptions.AccountLockedException;
import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AppUserMapper appUserMapper;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private TenantService tenantService;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @InjectMocks
    private AuthService authService;

    private SignupRequest signupRequest;
    private AppUser appUser;
    private UserData userData;
    private AppUserDetails userDetails;

    @BeforeEach
    void setUp() {
        signupRequest = new SignupRequest();
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("password");

        appUser = AppUser.builder()
                .id(1L)
                .email("test@example.com")
                .userPassword("encodedPassword")
                .userRole(UserRole.STUDENT)
                .failedLoginAttempts(0)
                .build();

        userData = new UserData();
        userData.setEmail("test@example.com");
        userData.setUserRole(UserRole.STUDENT.name());

        userDetails = new AppUserDetails(
                appUser.getEmail(),
                appUser.getUserPassword(),
                appUser.getUserRole().name(),
                appUser.getTenantId());

        lenient().when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
    }

    @Test
    void validateLogin_AccountLockedInDB_ShouldThrowException() {
        appUser.setAccountLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));

        assertThrows(BadCredentialsException.class, () -> authService.validateLogin("test@example.com", "password"));
    }

    @Test
    void validateLogin_AccountBlockedInRedis_ShouldThrowException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));
        when(loginAttemptService.isBlocked(1L)).thenReturn(true);

        assertThrows(BadCredentialsException.class, () -> authService.validateLogin("test@example.com", "password"));
    }

    @Test
    void validateLogin_BadCredentials_ShouldIncrementAttempts() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.validateLogin("test@example.com", "password"));

        verify(loginAttemptService).loginFailed(1L);
        assertEquals(1, appUser.getFailedLoginAttempts());
        verify(userRepository).save(appUser);
    }

    @Test
    void validateLogin_4thFailedAttempt_stillAllowed() {
        appUser.setFailedLoginAttempts(3);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.validateLogin("test@example.com", "password"));

        verify(loginAttemptService).loginFailed(1L);
        assertEquals(4, appUser.getFailedLoginAttempts());
        assertNull(appUser.getAccountLockedUntil());
        verify(userRepository).save(appUser);
    }

    @Test
    void validateLogin_5thFailedAttempt_locksAccount() {
        appUser.setFailedLoginAttempts(4);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.validateLogin("test@example.com", "password"));

        verify(loginAttemptService).loginFailed(1L);
        assertEquals(5, appUser.getFailedLoginAttempts());
        assertNotNull(appUser.getAccountLockedUntil());
        verify(userRepository).save(appUser);
    }

    @Test
    void unlockAccount_lockedUser_unlocked() {
        appUser.setAccountLockedUntil(LocalDateTime.now().plusMinutes(10));
        appUser.setFailedLoginAttempts(5);
        when(userRepository.findById(1L)).thenReturn(Optional.of(appUser));

        authService.unlockAccount(1L);

        assertNull(appUser.getAccountLockedUntil());
        assertEquals(0, appUser.getFailedLoginAttempts());
        verify(loginAttemptService).loginSucceeded(1L);
        verify(userRepository).save(appUser);
    }

    @Test
    void validateLogin_lockExpired_allowsLogin() {
        appUser.setAccountLockedUntil(LocalDateTime.now().minusMinutes(1)); // Expired
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtUtil.generateToken(eq(userDetails), anyBoolean())).thenReturn("token");
        when(appUserMapper.toUserData(appUser)).thenReturn(userData);

        authService.validateLogin("test@example.com", "password");

        verify(loginAttemptService).loginSucceeded(1L);
        // Should clear the lock
        assertNull(appUser.getAccountLockedUntil());
        verify(userRepository).save(appUser);
    }

    @Test
    void validateLogin_SuccessfulLogin_ShouldResetAttempts() {
        appUser.setFailedLoginAttempts(3);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtUtil.generateToken(eq(userDetails), anyBoolean())).thenReturn("token");
        when(appUserMapper.toUserData(appUser)).thenReturn(userData);

        authService.validateLogin("test@example.com", "password");

        verify(loginAttemptService).loginSucceeded(1L);
        assertEquals(0, appUser.getFailedLoginAttempts());
        verify(userRepository).save(appUser);
    }

    @Test
    void logout_validToken_blacklisted() {
        Authentication authentication = mock(Authentication.class);
        String token = "valid.jwt.token";
        when(authentication.getCredentials()).thenReturn(token);
        when(jwtUtil.extractJti(token)).thenReturn("jti-123");
        when(jwtUtil.getRemainingTtlSeconds(token)).thenReturn(10L);

        authService.logout(authentication);

        verify(tokenBlacklistService).blacklistToken(eq("jti-123"), anyLong());
    }

    @Test
    void changePassword_existingToken_blacklisted() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("test@example.com");
        String token = "valid.jwt.token";
        when(authentication.getCredentials()).thenReturn(token);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(appUser));
        when(passwordEncoder.matches("oldPass", "encodedPassword")).thenReturn(true);
        when(jwtUtil.extractJti(token)).thenReturn("jti-123");
        when(jwtUtil.getRemainingTtlSeconds(token)).thenReturn(10L);

        com.beezhub.student_accommodation.model.dto.ChangePasswordRequest req = new com.beezhub.student_accommodation.model.dto.ChangePasswordRequest();
        req.setCurrentPassword("oldPass");
        req.setNewPassword("newPass");

        authService.changePassword(req, authentication);

        verify(tokenBlacklistService).blacklistToken(eq("jti-123"), anyLong());
        verify(userRepository).save(appUser);
    }

    @Test
    void signup_ShouldCreateUserAndReturnToken() {
        // Arrange
        when(userRepository.findByEmail(signupRequest.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(signupRequest.getPassword())).thenReturn("encodedPassword");
        when(appUserMapper.toEntity(eq(signupRequest), anyString())).thenReturn(appUser);
        when(userRepository.save(appUser)).thenReturn(appUser);
        when(jwtUtil.generateToken(any(AppUserDetails.class))).thenReturn("jwt-token");

        // Act
        var signup = authService.signup(signupRequest);

        // Assert: student signup assigns the PLATFORM sentinel (tenant_id=0) — not the default tenant
        assertEquals("jwt-token", signup.getToken());
        assertEquals(TenantContext.PLATFORM_TENANT_ID, appUser.getTenantId());
        verifyNoInteractions(tenantService);
        verify(userRepository).save(appUser);
    }

    @Test
    void signup_WithExistingEmail_ShouldThrowException() {

        when(userRepository.findByEmail(signupRequest.getEmail())).thenReturn(Optional.of(appUser));

        EmailAlreadyRegisteredException exception = assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> authService.signup(signupRequest));
        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void validateLogin_ShouldReturnLoginResponse() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtil.generateToken(eq(userDetails), anyBoolean())).thenReturn("jwt-token");
        when(userRepository.findByEmail(userDetails.getUsername())).thenReturn(Optional.of(appUser));
        when(appUserMapper.toUserData(appUser)).thenReturn(userData);

        // Act
        LoginResponse response = authService.validateLogin("test@example.com", "password");

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals(userData, response.getUser());
    }

    @Test
    void validateLogin_UserNotFound_ShouldThrowException() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByEmail(userDetails.getUsername())).thenReturn(Optional.empty());

        // Act & Assert
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> authService.validateLogin("test@example.com", "password"));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void validateLogin_InvalidCredentials_ShouldThrowException() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThrows(
                BadCredentialsException.class,
                () -> authService.validateLogin("test@example.com", "wrongpassword"));
    }
}