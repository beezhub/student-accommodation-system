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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

        appUser = new AppUser();
        appUser.setEmail("test@example.com");
        appUser.setUserPassword("encodedPassword");
        appUser.setUserRole(UserRole.STUDENT);

        userData = new UserData();
        userData.setEmail("test@example.com");
        userData.setUserRole(UserRole.STUDENT.name());

        userDetails = new AppUserDetails(
                appUser.getEmail(),
                appUser.getUserPassword(),
                appUser.getUserRole().name()
        );
    }

    @Test
    void signup_ShouldCreateUserAndReturnToken() {
        // Arrange
        when(userRepository.findByEmail(signupRequest.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(signupRequest.getPassword())).thenReturn("encodedPassword");
        when(appUserMapper.toEntity(eq(signupRequest), anyString())).thenReturn(appUser);
        when(jwtUtil.generateToken(any(AppUserDetails.class))).thenReturn("jwt-token");

        // Act
        var signup = authService.signup(signupRequest);

        // Assert
        assertEquals("jwt-token", signup.getToken());
        verify(userRepository).save(appUser);
    }

    @Test
    void signup_WithExistingEmail_ShouldThrowException() {

        when(userRepository.findByEmail(signupRequest.getEmail())).thenReturn(Optional.of(appUser));

        EmailAlreadyRegisteredException exception = assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> authService.signup(signupRequest)
        );
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
        when(jwtUtil.generateToken(userDetails)).thenReturn("jwt-token");
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
        when(jwtUtil.generateToken(userDetails)).thenReturn("jwt-token");
        when(userRepository.findByEmail(userDetails.getUsername())).thenReturn(Optional.empty());

        // Act & Assert
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> authService.validateLogin("test@example.com", "password")
        );
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
                () -> authService.validateLogin("test@example.com", "wrongpassword")
        );
    }
}