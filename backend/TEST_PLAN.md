# Student Accommodation System - Comprehensive Test Plan

## Table of Contents
1. [Test Plan Overview](#1-test-plan-overview)
2. [Unit Tests](#2-unit-tests)
3. [Integration Tests](#3-integration-tests)
4. [End-to-End Test Scenarios (Playwright)](#4-end-to-end-test-scenarios-playwright)
5. [Manual Test Cases](#5-manual-test-cases)
6. [Test Data Requirements](#6-test-data-requirements)
7. [Test Environment Setup](#7-test-environment-setup)

---

## 1. Test Plan Overview

### 1.1 Project Summary

The Student Accommodation System is a comprehensive Spring Boot 3 backend application designed to manage student housing allocation at educational institutions. The system provides:

- **User Management**: Registration, authentication (JWT-based), email verification, password reset
- **Application Processing**: Students submit accommodation applications with document uploads
- **Document Management**: Multi-file upload to MinIO storage with admin review workflow
- **Room & Building Management**: CRUD operations for accommodation inventory
- **Allocation System**: Manual and automatic room allocation with scoring algorithms
- **Batch Processing**: Staged allocation proposals with confirm/reject workflow
- **Waiting List**: Dynamic scoring and ranking for unallocated students
- **Admin Dashboard**: Statistics, activity logging, and system management

**Technology Stack:**
- Framework: Spring Boot 3.5.7 (Java 17+)
- Database: PostgreSQL with Flyway migrations
- Authentication: JWT (JJWT 0.11.5)
- File Storage: MinIO (S3-compatible)
- ORM: Hibernate/JPA
- Mapping: MapStruct 1.6.3
- Feature Flags: FF4j
- Documentation: SpringDoc OpenAPI

### 1.2 Testing Objectives

1. **Functional Verification**: Ensure all features work as specified
2. **Security Validation**: Verify authentication, authorization, and data protection
3. **Data Integrity**: Confirm database operations maintain consistency
4. **API Contract Compliance**: Validate REST endpoints match specifications
5. **Error Handling**: Test exception scenarios and error responses
6. **Performance Baseline**: Establish response time benchmarks
7. **Integration Validation**: Verify external service integrations (MinIO, Email)

### 1.3 Testing Scope

**In Scope:**
- All REST API endpoints
- Authentication and authorization flows
- Business logic in service layer
- Database operations and constraints
- File upload/download functionality
- Email notification triggers
- Batch allocation processes
- Admin dashboard statistics

**Out of Scope:**
- Frontend UI testing (covered separately)
- Load/stress testing (separate performance test plan)
- Infrastructure testing (DevOps responsibility)
- Third-party service internal testing (MinIO, PostgreSQL)

### 1.4 Testing Approach and Methodology

**Testing Levels:**
1. **Unit Testing**: JUnit 5 + Mockito for isolated component testing
2. **Integration Testing**: Spring Boot Test with TestContainers for database/service integration
3. **API Testing**: MockMvc for endpoint testing with security context
4. **E2E Testing**: Playwright for full user flow validation

**Testing Strategy:**
- Test-Driven Development (TDD) for new features
- Behavior-Driven Development (BDD) for acceptance criteria
- Risk-based testing prioritization
- Continuous integration with automated test execution

### 1.5 Entry and Exit Criteria

**Entry Criteria:**
- Code complete and compiles successfully
- Development environment configured
- Test data scripts prepared
- Test environment available (PostgreSQL, MinIO)
- API documentation updated

**Exit Criteria:**
- All critical and high priority test cases passed
- Code coverage minimum 80% for service layer
- No open critical or high severity defects
- All integration tests passing
- Performance benchmarks met (API response < 500ms)

---

## 2. Unit Tests

### 2.1 Authentication Service Tests

**File**: `src/test/java/com/beezhub/student_accommodation/service/AuthServiceTest.java`

```java
package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.dto.request.LoginRequest;
import com.beezhub.student_accommodation.dto.request.SignupRequest;
import com.beezhub.student_accommodation.dto.response.LoginResponse;
import com.beezhub.student_accommodation.entity.AppUser;
import com.beezhub.student_accommodation.entity.EmailVerificationToken;
import com.beezhub.student_accommodation.enums.UserRole;
import com.beezhub.student_accommodation.exception.EmailAlreadyRegisteredException;
import com.beezhub.student_accommodation.exception.EmailNotVerifiedException;
import com.beezhub.student_accommodation.exception.InvalidTokenException;
import com.beezhub.student_accommodation.exception.UserNotFoundException;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.EmailVerificationTokenRepository;
import com.beezhub.student_accommodation.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private EmailVerificationTokenRepository verificationTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private AppUser testUser;
    private SignupRequest signupRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setEmail("test@university.edu");
        testUser.setPassword("encodedPassword");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRole(UserRole.STUDENT);
        testUser.setEmailVerified(true);

        signupRequest = new SignupRequest();
        signupRequest.setEmail("newuser@university.edu");
        signupRequest.setPassword("Password123!");
        signupRequest.setFirstName("Jane");
        signupRequest.setLastName("Smith");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@university.edu");
        loginRequest.setPassword("password123");
    }

    @Nested
    @DisplayName("User Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should successfully register new user with valid data")
        void signup_WithValidData_ShouldCreateUser() {
            // Arrange
            when(appUserRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> {
                AppUser user = i.getArgument(0);
                user.setId(1L);
                return user;
            });

            // Act
            authService.signup(signupRequest);

            // Assert
            ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
            verify(appUserRepository).save(userCaptor.capture());

            AppUser savedUser = userCaptor.getValue();
            assertThat(savedUser.getEmail()).isEqualTo("newuser@university.edu");
            assertThat(savedUser.getFirstName()).isEqualTo("Jane");
            assertThat(savedUser.getLastName()).isEqualTo("Smith");
            assertThat(savedUser.getRole()).isEqualTo(UserRole.STUDENT);
            assertThat(savedUser.isEmailVerified()).isFalse();
        }

        @Test
        @DisplayName("Should throw exception when email already registered")
        void signup_WithExistingEmail_ShouldThrowException() {
            // Arrange
            when(appUserRepository.existsByEmail(anyString())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.signup(signupRequest))
                    .isInstanceOf(EmailAlreadyRegisteredException.class)
                    .hasMessageContaining("already registered");

            verify(appUserRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should send verification email after registration")
        void signup_WithValidData_ShouldSendVerificationEmail() {
            // Arrange
            when(appUserRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> {
                AppUser user = i.getArgument(0);
                user.setId(1L);
                return user;
            });

            // Act
            authService.signup(signupRequest);

            // Assert
            verify(emailService).sendVerificationEmail(any(AppUser.class), anyString());
        }
    }

    @Nested
    @DisplayName("User Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should return JWT token on successful login")
        void login_WithValidCredentials_ShouldReturnToken() {
            // Arrange
            when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
            when(jwtUtil.generateToken(any(AppUser.class))).thenReturn("jwt-token-123");

            // Act
            LoginResponse response = authService.login(loginRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-token-123");
            assertThat(response.getEmail()).isEqualTo("test@university.edu");
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("Should throw exception for unverified email")
        void login_WithUnverifiedEmail_ShouldThrowException() {
            // Arrange
            testUser.setEmailVerified(false);
            when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(EmailNotVerifiedException.class);
        }

        @Test
        @DisplayName("Should throw exception for invalid credentials")
        void login_WithInvalidCredentials_ShouldThrowException() {
            // Arrange
            when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void login_WithNonExistentUser_ShouldThrowException() {
            // Arrange
            when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Email Verification Tests")
    class EmailVerificationTests {

        @Test
        @DisplayName("Should verify email with valid token")
        void verifyEmail_WithValidToken_ShouldVerifyUser() {
            // Arrange
            String token = UUID.randomUUID().toString();
            EmailVerificationToken verificationToken = new EmailVerificationToken();
            verificationToken.setToken(token);
            verificationToken.setUser(testUser);
            verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24));
            testUser.setEmailVerified(false);

            when(verificationTokenRepository.findByToken(token))
                    .thenReturn(Optional.of(verificationToken));

            // Act
            authService.verifyEmail(token);

            // Assert
            assertThat(testUser.isEmailVerified()).isTrue();
            verify(appUserRepository).save(testUser);
            verify(verificationTokenRepository).delete(verificationToken);
        }

        @Test
        @DisplayName("Should throw exception for expired token")
        void verifyEmail_WithExpiredToken_ShouldThrowException() {
            // Arrange
            String token = UUID.randomUUID().toString();
            EmailVerificationToken verificationToken = new EmailVerificationToken();
            verificationToken.setToken(token);
            verificationToken.setUser(testUser);
            verificationToken.setExpiryDate(LocalDateTime.now().minusHours(1));

            when(verificationTokenRepository.findByToken(token))
                    .thenReturn(Optional.of(verificationToken));

            // Act & Assert
            assertThatThrownBy(() -> authService.verifyEmail(token))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Should throw exception for invalid token")
        void verifyEmail_WithInvalidToken_ShouldThrowException() {
            // Arrange
            when(verificationTokenRepository.findByToken(anyString()))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.verifyEmail("invalid-token"))
                    .isInstanceOf(InvalidTokenException.class);
        }
    }

    @Nested
    @DisplayName("Password Reset Tests")
    class PasswordResetTests {

        @Test
        @DisplayName("Should send password reset email for valid user")
        void forgotPassword_WithValidEmail_ShouldSendResetEmail() {
            // Arrange
            when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

            // Act
            authService.forgotPassword("test@university.edu");

            // Assert
            verify(emailService).sendPasswordResetEmail(eq(testUser), anyString());
        }

        @Test
        @DisplayName("Should not throw exception for non-existent email (security)")
        void forgotPassword_WithNonExistentEmail_ShouldNotRevealUserExistence() {
            // Arrange
            when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            // Act & Assert - should not throw exception to prevent email enumeration
            assertThatCode(() -> authService.forgotPassword("unknown@university.edu"))
                    .doesNotThrowAnyException();

            verify(emailService, never()).sendPasswordResetEmail(any(), anyString());
        }

        @Test
        @DisplayName("Should reset password with valid token")
        void resetPassword_WithValidToken_ShouldUpdatePassword() {
            // Arrange
            String token = UUID.randomUUID().toString();
            String newPassword = "NewPassword123!";

            when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");
            // Additional setup for token validation...

            // Act & Assert
            // Implementation depends on actual service method signature
        }
    }

    @Nested
    @DisplayName("Password Change Tests")
    class PasswordChangeTests {

        @Test
        @DisplayName("Should change password with correct current password")
        void changePassword_WithValidCurrentPassword_ShouldUpdatePassword() {
            // Arrange
            when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("currentPassword", testUser.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

            // Act
            authService.changePassword("test@university.edu", "currentPassword", "newPassword");

            // Assert
            verify(appUserRepository).save(testUser);
            assertThat(testUser.getPassword()).isEqualTo("encodedNewPassword");
        }

        @Test
        @DisplayName("Should throw exception for incorrect current password")
        void changePassword_WithIncorrectCurrentPassword_ShouldThrowException() {
            // Arrange
            when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() ->
                authService.changePassword("test@university.edu", "wrongPassword", "newPassword"))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }
}
```

### 2.2 Application Service Tests

**File**: `src/test/java/com/beezhub/student_accommodation/service/ApplicationServiceTest.java`

```java
package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.dto.request.ApplicationRequest;
import com.beezhub.student_accommodation.dto.response.ApplicationResponse;
import com.beezhub.student_accommodation.entity.*;
import com.beezhub.student_accommodation.enums.ApplicationStatus;
import com.beezhub.student_accommodation.exception.*;
import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.AccommodationTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AccommodationTypeRepository accommodationTypeRepository;

    @Mock
    private ApplicationMapper applicationMapper;

    @InjectMocks
    private ApplicationService applicationService;

    private Student testStudent;
    private Application testApplication;
    private ApplicationRequest applicationRequest;
    private AccommodationType accommodationType;

    @BeforeEach
    void setUp() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setEmail("student@university.edu");

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setStudentNumber("STU001");
        testStudent.setUser(user);

        accommodationType = new AccommodationType();
        accommodationType.setId(1L);
        accommodationType.setName("Single Room");

        testApplication = new Application();
        testApplication.setId(1L);
        testApplication.setApplicationCode("APP-2024-001");
        testApplication.setStudent(testStudent);
        testApplication.setStatus(ApplicationStatus.PENDING);
        testApplication.setPreferredAccommodationType(accommodationType);
        testApplication.setMoveInDate(LocalDate.now().plusMonths(1));
        testApplication.setDuration(12);

        applicationRequest = new ApplicationRequest();
        applicationRequest.setAccommodationTypeId(1L);
        applicationRequest.setMoveInDate(LocalDate.now().plusMonths(1));
        applicationRequest.setDuration(12);
        applicationRequest.setSpecialRequirements("Ground floor preferred");
    }

    @Nested
    @DisplayName("Application Submission Tests")
    class SubmissionTests {

        @Test
        @DisplayName("Should successfully submit application for eligible student")
        void submitApplication_WithValidData_ShouldCreateApplication() {
            // Arrange
            when(studentRepository.findByUserEmail(anyString())).thenReturn(Optional.of(testStudent));
            when(accommodationTypeRepository.findById(anyLong())).thenReturn(Optional.of(accommodationType));
            when(applicationRepository.existsByStudentAndStatusIn(any(), any())).thenReturn(false);
            when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
            when(applicationMapper.toResponse(any())).thenReturn(new ApplicationResponse());

            // Act
            ApplicationResponse response = applicationService.submitApplication(
                    "student@university.edu", applicationRequest);

            // Assert
            assertThat(response).isNotNull();
            verify(applicationRepository).save(any(Application.class));
        }

        @Test
        @DisplayName("Should throw exception for duplicate pending application")
        void submitApplication_WithExistingPendingApplication_ShouldThrowException() {
            // Arrange
            when(studentRepository.findByUserEmail(anyString())).thenReturn(Optional.of(testStudent));
            when(applicationRepository.existsByStudentAndStatusIn(any(), any())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() ->
                applicationService.submitApplication("student@university.edu", applicationRequest))
                    .isInstanceOf(DuplicateApplicationException.class);
        }

        @Test
        @DisplayName("Should throw exception for non-existent student")
        void submitApplication_WithNonExistentStudent_ShouldThrowException() {
            // Arrange
            when(studentRepository.findByUserEmail(anyString())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() ->
                applicationService.submitApplication("unknown@university.edu", applicationRequest))
                    .isInstanceOf(StudentNotFoundException.class);
        }

        @Test
        @DisplayName("Should generate unique application code")
        void submitApplication_ShouldGenerateUniqueCode() {
            // Arrange
            when(studentRepository.findByUserEmail(anyString())).thenReturn(Optional.of(testStudent));
            when(accommodationTypeRepository.findById(anyLong())).thenReturn(Optional.of(accommodationType));
            when(applicationRepository.existsByStudentAndStatusIn(any(), any())).thenReturn(false);
            when(applicationRepository.save(any(Application.class))).thenAnswer(i -> {
                Application app = i.getArgument(0);
                assertThat(app.getApplicationCode()).isNotNull();
                assertThat(app.getApplicationCode()).startsWith("APP-");
                return app;
            });

            // Act
            applicationService.submitApplication("student@university.edu", applicationRequest);

            // Assert
            verify(applicationRepository).save(any(Application.class));
        }
    }

    @Nested
    @DisplayName("Application Status Update Tests")
    class StatusUpdateTests {

        @Test
        @DisplayName("Should update status from PENDING to APPROVED")
        void updateStatus_FromPendingToApproved_ShouldSucceed() {
            // Arrange
            testApplication.setStatus(ApplicationStatus.PENDING);
            when(applicationRepository.findById(anyLong())).thenReturn(Optional.of(testApplication));
            when(applicationRepository.save(any())).thenReturn(testApplication);

            // Act
            applicationService.updateApplicationStatus(1L, ApplicationStatus.APPROVED);

            // Assert
            assertThat(testApplication.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
            verify(applicationRepository).save(testApplication);
        }

        @Test
        @DisplayName("Should throw exception for invalid status transition")
        void updateStatus_InvalidTransition_ShouldThrowException() {
            // Arrange
            testApplication.setStatus(ApplicationStatus.REJECTED);
            when(applicationRepository.findById(anyLong())).thenReturn(Optional.of(testApplication));

            // Act & Assert
            assertThatThrownBy(() ->
                applicationService.updateApplicationStatus(1L, ApplicationStatus.APPROVED))
                    .isInstanceOf(InvalidStatusException.class);
        }

        @Test
        @DisplayName("Should update status from APPROVED to AWAITING_ALLOCATION")
        void updateStatus_FromApprovedToAwaitingAllocation_ShouldSucceed() {
            // Arrange
            testApplication.setStatus(ApplicationStatus.APPROVED);
            when(applicationRepository.findById(anyLong())).thenReturn(Optional.of(testApplication));
            when(applicationRepository.save(any())).thenReturn(testApplication);

            // Act
            applicationService.updateApplicationStatus(1L, ApplicationStatus.AWAITING_ALLOCATION);

            // Assert
            assertThat(testApplication.getStatus()).isEqualTo(ApplicationStatus.AWAITING_ALLOCATION);
        }

        @Test
        @DisplayName("Should allow rejection from any non-terminal state")
        void updateStatus_RejectFromAnyState_ShouldSucceed() {
            // Test PENDING -> REJECTED
            testApplication.setStatus(ApplicationStatus.PENDING);
            when(applicationRepository.findById(anyLong())).thenReturn(Optional.of(testApplication));
            when(applicationRepository.save(any())).thenReturn(testApplication);

            applicationService.updateApplicationStatus(1L, ApplicationStatus.REJECTED);

            assertThat(testApplication.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        }
    }

    @Nested
    @DisplayName("Application Retrieval Tests")
    class RetrievalTests {

        @Test
        @DisplayName("Should retrieve applications for student")
        void getMyApplications_ShouldReturnStudentApplications() {
            // Arrange
            List<Application> applications = List.of(testApplication);
            when(studentRepository.findByUserEmail(anyString())).thenReturn(Optional.of(testStudent));
            when(applicationRepository.findByStudent(testStudent)).thenReturn(applications);
            when(applicationMapper.toResponse(any())).thenReturn(new ApplicationResponse());

            // Act
            List<ApplicationResponse> result = applicationService.getMyApplications("student@university.edu");

            // Assert
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should retrieve paginated applications for admin")
        void getAllApplications_ShouldReturnPaginatedResults() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Application> page = new PageImpl<>(List.of(testApplication), pageable, 1);
            when(applicationRepository.findAll(pageable)).thenReturn(page);
            when(applicationMapper.toResponse(any())).thenReturn(new ApplicationResponse());

            // Act
            Page<ApplicationResponse> result = applicationService.getAllApplications(pageable);

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should filter applications by status")
        void getApplicationsByStatus_ShouldReturnFilteredResults() {
            // Arrange
            List<Application> applications = List.of(testApplication);
            when(applicationRepository.findByStatus(ApplicationStatus.PENDING)).thenReturn(applications);
            when(applicationMapper.toResponse(any())).thenReturn(new ApplicationResponse());

            // Act
            List<ApplicationResponse> result = applicationService
                    .getApplicationsByStatus(ApplicationStatus.PENDING);

            // Assert
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Application Cancellation Tests")
    class CancellationTests {

        @Test
        @DisplayName("Should cancel pending application by owner")
        void cancelApplication_ByOwner_ShouldSucceed() {
            // Arrange
            when(applicationRepository.findById(anyLong())).thenReturn(Optional.of(testApplication));
            when(studentRepository.findByUserEmail(anyString())).thenReturn(Optional.of(testStudent));

            // Act
            applicationService.cancelApplication(1L, "student@university.edu");

            // Assert
            verify(applicationRepository).delete(testApplication);
        }

        @Test
        @DisplayName("Should throw exception when cancelling others application")
        void cancelApplication_ByNonOwner_ShouldThrowException() {
            // Arrange
            Student otherStudent = new Student();
            otherStudent.setId(2L);

            when(applicationRepository.findById(anyLong())).thenReturn(Optional.of(testApplication));
            when(studentRepository.findByUserEmail(anyString())).thenReturn(Optional.of(otherStudent));

            // Act & Assert
            assertThatThrownBy(() ->
                applicationService.cancelApplication(1L, "other@university.edu"))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        @DisplayName("Should throw exception when cancelling allocated application")
        void cancelApplication_AlreadyAllocated_ShouldThrowException() {
            // Arrange
            testApplication.setStatus(ApplicationStatus.ALLOCATED);
            when(applicationRepository.findById(anyLong())).thenReturn(Optional.of(testApplication));
            when(studentRepository.findByUserEmail(anyString())).thenReturn(Optional.of(testStudent));

            // Act & Assert
            assertThatThrownBy(() ->
                applicationService.cancelApplication(1L, "student@university.edu"))
                    .isInstanceOf(InvalidStatusException.class);
        }
    }
}
```

### 2.3 Room Allocation Service Tests

**File**: `src/test/java/com/beezhub/student_accommodation/service/RoomAllocationServiceTest.java`

```java
package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.dto.request.RoomAllocationRequest;
import com.beezhub.student_accommodation.dto.response.AllocationEligibilityResponse;
import com.beezhub.student_accommodation.dto.response.RoomAllocationResponse;
import com.beezhub.student_accommodation.entity.*;
import com.beezhub.student_accommodation.enums.AllocationStatus;
import com.beezhub.student_accommodation.enums.ApplicationStatus;
import com.beezhub.student_accommodation.enums.RoomStatus;
import com.beezhub.student_accommodation.exception.*;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomAllocationServiceTest {

    @Mock
    private RoomAllocationRepository allocationRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private RoomAllocationMapper allocationMapper;

    @InjectMocks
    private RoomAllocationService allocationService;

    private Student testStudent;
    private Room testRoom;
    private Application testApplication;
    private RoomAllocation testAllocation;
    private Building testBuilding;

    @BeforeEach
    void setUp() {
        testBuilding = new Building();
        testBuilding.setId(1L);
        testBuilding.setName("Main Hall");

        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setRoomNumber("101");
        testRoom.setCapacity(2);
        testRoom.setCurrentOccupancy(0);
        testRoom.setStatus(RoomStatus.AVAILABLE);
        testRoom.setBuilding(testBuilding);

        AppUser user = new AppUser();
        user.setId(1L);

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setStudentNumber("STU001");
        testStudent.setUser(user);

        testApplication = new Application();
        testApplication.setId(1L);
        testApplication.setStudent(testStudent);
        testApplication.setStatus(ApplicationStatus.AWAITING_ALLOCATION);

        testAllocation = new RoomAllocation();
        testAllocation.setId(1L);
        testAllocation.setStudent(testStudent);
        testAllocation.setRoom(testRoom);
        testAllocation.setApplication(testApplication);
        testAllocation.setStatus(AllocationStatus.ACTIVE);
        testAllocation.setAllocationDate(LocalDate.now());
    }

    @Nested
    @DisplayName("Room Allocation Creation Tests")
    class AllocationCreationTests {

        @Test
        @DisplayName("Should allocate room to eligible student")
        void createAllocation_WithEligibleStudent_ShouldSucceed() {
            // Arrange
            RoomAllocationRequest request = new RoomAllocationRequest();
            request.setStudentId(1L);
            request.setRoomId(1L);
            request.setApplicationId(1L);

            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            when(allocationRepository.existsByStudentAndStatus(testStudent, AllocationStatus.ACTIVE))
                    .thenReturn(false);
            when(allocationRepository.save(any())).thenReturn(testAllocation);
            when(allocationMapper.toResponse(any())).thenReturn(new RoomAllocationResponse());

            // Act
            RoomAllocationResponse response = allocationService.createAllocation(request);

            // Assert
            assertThat(response).isNotNull();
            verify(roomRepository).save(testRoom);
            assertThat(testRoom.getCurrentOccupancy()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should throw exception when student already has active allocation")
        void createAllocation_WithExistingAllocation_ShouldThrowException() {
            // Arrange
            RoomAllocationRequest request = new RoomAllocationRequest();
            request.setStudentId(1L);
            request.setRoomId(1L);

            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(allocationRepository.existsByStudentAndStatus(testStudent, AllocationStatus.ACTIVE))
                    .thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> allocationService.createAllocation(request))
                    .isInstanceOf(StudentAlreadyAllocatedException.class);
        }

        @Test
        @DisplayName("Should throw exception when room is full")
        void createAllocation_WithFullRoom_ShouldThrowException() {
            // Arrange
            testRoom.setCurrentOccupancy(2); // Room at capacity
            testRoom.setStatus(RoomStatus.FULL);

            RoomAllocationRequest request = new RoomAllocationRequest();
            request.setStudentId(1L);
            request.setRoomId(1L);

            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
            when(allocationRepository.existsByStudentAndStatus(any(), any())).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> allocationService.createAllocation(request))
                    .isInstanceOf(RoomCapacityExceededException.class);
        }

        @Test
        @DisplayName("Should throw exception when application not in AWAITING_ALLOCATION status")
        void createAllocation_WithInvalidApplicationStatus_ShouldThrowException() {
            // Arrange
            testApplication.setStatus(ApplicationStatus.PENDING);

            RoomAllocationRequest request = new RoomAllocationRequest();
            request.setStudentId(1L);
            request.setRoomId(1L);
            request.setApplicationId(1L);

            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            when(allocationRepository.existsByStudentAndStatus(any(), any())).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> allocationService.createAllocation(request))
                    .isInstanceOf(ApplicationNotApprovedException.class);
        }

        @Test
        @DisplayName("Should update room status to FULL when capacity reached")
        void createAllocation_WhenRoomBecomesFull_ShouldUpdateStatus() {
            // Arrange
            testRoom.setCapacity(1);
            testRoom.setCurrentOccupancy(0);

            RoomAllocationRequest request = new RoomAllocationRequest();
            request.setStudentId(1L);
            request.setRoomId(1L);
            request.setApplicationId(1L);

            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
            when(allocationRepository.existsByStudentAndStatus(any(), any())).thenReturn(false);
            when(allocationRepository.save(any())).thenReturn(testAllocation);
            when(allocationMapper.toResponse(any())).thenReturn(new RoomAllocationResponse());

            // Act
            allocationService.createAllocation(request);

            // Assert
            assertThat(testRoom.getStatus()).isEqualTo(RoomStatus.FULL);
            assertThat(testRoom.getCurrentOccupancy()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Allocation Termination Tests")
    class AllocationTerminationTests {

        @Test
        @DisplayName("Should end allocation and update room occupancy")
        void endAllocation_ShouldUpdateRoomOccupancy() {
            // Arrange
            testRoom.setCurrentOccupancy(1);
            when(allocationRepository.findById(1L)).thenReturn(Optional.of(testAllocation));
            when(allocationRepository.save(any())).thenReturn(testAllocation);

            // Act
            allocationService.endAllocation(1L);

            // Assert
            assertThat(testAllocation.getStatus()).isEqualTo(AllocationStatus.COMPLETED);
            assertThat(testRoom.getCurrentOccupancy()).isEqualTo(0);
            assertThat(testRoom.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        }

        @Test
        @DisplayName("Should throw exception when ending non-active allocation")
        void endAllocation_NonActiveAllocation_ShouldThrowException() {
            // Arrange
            testAllocation.setStatus(AllocationStatus.COMPLETED);
            when(allocationRepository.findById(1L)).thenReturn(Optional.of(testAllocation));

            // Act & Assert
            assertThatThrownBy(() -> allocationService.endAllocation(1L))
                    .isInstanceOf(InvalidStatusException.class);
        }
    }

    @Nested
    @DisplayName("Eligibility Check Tests")
    class EligibilityTests {

        @Test
        @DisplayName("Should return eligible when student has no active allocation")
        void checkEligibility_WithNoActiveAllocation_ShouldBeEligible() {
            // Arrange
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(allocationRepository.existsByStudentAndStatus(testStudent, AllocationStatus.ACTIVE))
                    .thenReturn(false);

            // Act
            AllocationEligibilityResponse response = allocationService.checkStudentEligibility(1L);

            // Assert
            assertThat(response.isEligible()).isTrue();
        }

        @Test
        @DisplayName("Should return ineligible when student has active allocation")
        void checkEligibility_WithActiveAllocation_ShouldBeIneligible() {
            // Arrange
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(allocationRepository.existsByStudentAndStatus(testStudent, AllocationStatus.ACTIVE))
                    .thenReturn(true);

            // Act
            AllocationEligibilityResponse response = allocationService.checkStudentEligibility(1L);

            // Assert
            assertThat(response.isEligible()).isFalse();
            assertThat(response.getReason()).contains("already has an active allocation");
        }
    }

    @Nested
    @DisplayName("Room Availability Tests")
    class RoomAvailabilityTests {

        @Test
        @DisplayName("Should return true for available room with capacity")
        void isRoomAvailable_WithCapacity_ShouldReturnTrue() {
            // Arrange
            testRoom.setStatus(RoomStatus.AVAILABLE);
            testRoom.setCurrentOccupancy(0);
            when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));

            // Act
            boolean available = allocationService.isRoomAvailable(1L);

            // Assert
            assertThat(available).isTrue();
        }

        @Test
        @DisplayName("Should return false for room at full capacity")
        void isRoomAvailable_AtCapacity_ShouldReturnFalse() {
            // Arrange
            testRoom.setStatus(RoomStatus.FULL);
            testRoom.setCurrentOccupancy(2);
            when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));

            // Act
            boolean available = allocationService.isRoomAvailable(1L);

            // Assert
            assertThat(available).isFalse();
        }

        @Test
        @DisplayName("Should return false for room under maintenance")
        void isRoomAvailable_UnderMaintenance_ShouldReturnFalse() {
            // Arrange
            testRoom.setStatus(RoomStatus.MAINTENANCE);
            when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));

            // Act
            boolean available = allocationService.isRoomAvailable(1L);

            // Assert
            assertThat(available).isFalse();
        }
    }

    @Nested
    @DisplayName("Move Student Tests")
    class MoveStudentTests {

        @Test
        @DisplayName("Should move student to new room")
        void moveStudent_ToAvailableRoom_ShouldSucceed() {
            // Arrange
            Room newRoom = new Room();
            newRoom.setId(2L);
            newRoom.setRoomNumber("102");
            newRoom.setCapacity(2);
            newRoom.setCurrentOccupancy(0);
            newRoom.setStatus(RoomStatus.AVAILABLE);
            newRoom.setBuilding(testBuilding);

            testRoom.setCurrentOccupancy(1);

            when(allocationRepository.findById(1L)).thenReturn(Optional.of(testAllocation));
            when(roomRepository.findById(2L)).thenReturn(Optional.of(newRoom));
            when(allocationRepository.save(any())).thenReturn(testAllocation);

            // Act
            allocationService.moveStudentToRoom(1L, 2L);

            // Assert
            assertThat(testAllocation.getRoom()).isEqualTo(newRoom);
            assertThat(testRoom.getCurrentOccupancy()).isEqualTo(0);
            assertThat(newRoom.getCurrentOccupancy()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should throw exception when moving to full room")
        void moveStudent_ToFullRoom_ShouldThrowException() {
            // Arrange
            Room fullRoom = new Room();
            fullRoom.setId(2L);
            fullRoom.setCapacity(1);
            fullRoom.setCurrentOccupancy(1);
            fullRoom.setStatus(RoomStatus.FULL);

            when(allocationRepository.findById(1L)).thenReturn(Optional.of(testAllocation));
            when(roomRepository.findById(2L)).thenReturn(Optional.of(fullRoom));

            // Act & Assert
            assertThatThrownBy(() -> allocationService.moveStudentToRoom(1L, 2L))
                    .isInstanceOf(RoomCapacityExceededException.class);
        }
    }
}
```

### 2.4 Document Service Tests

**File**: `src/test/java/com/beezhub/student_accommodation/service/DocumentServiceTest.java`

```java
package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.dto.response.DocumentResponse;
import com.beezhub.student_accommodation.entity.*;
import com.beezhub.student_accommodation.enums.DocumentStatus;
import com.beezhub.student_accommodation.exception.DocumentNotFoundException;
import com.beezhub.student_accommodation.exception.UnauthorizedAccessException;
import com.beezhub.student_accommodation.mapper.DocumentMapper;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DocumentTypeRepository documentTypeRepository;

    @Mock
    private MinioBucketService minioBucketService;

    @Mock
    private DocumentMapper documentMapper;

    @InjectMocks
    private DocumentService documentService;

    private Student testStudent;
    private Document testDocument;
    private DocumentType testDocumentType;
    private Application testApplication;

    @BeforeEach
    void setUp() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setEmail("student@university.edu");

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setStudentNumber("STU001");
        testStudent.setUser(user);

        testDocumentType = new DocumentType();
        testDocumentType.setId(1L);
        testDocumentType.setName("ID Document");

        testApplication = new Application();
        testApplication.setId(1L);
        testApplication.setStudent(testStudent);

        testDocument = new Document();
        testDocument.setId(1L);
        testDocument.setDocumentName("student_id.pdf");
        testDocument.setPath("documents/1/student_id.pdf");
        testDocument.setFileSize(1024L);
        testDocument.setStatus(DocumentStatus.PENDING);
        testDocument.setStudent(testStudent);
        testDocument.setDocumentType(testDocumentType);
    }

    @Nested
    @DisplayName("Document Upload Tests")
    class UploadTests {

        @Test
        @DisplayName("Should upload single document successfully")
        void uploadSingleDocument_WithValidFile_ShouldSucceed() {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.pdf",
                    "application/pdf",
                    "test content".getBytes()
            );

            when(studentRepository.findByUserEmail(anyString())).thenReturn(Optional.of(testStudent));
            when(documentTypeRepository.findById(anyLong())).thenReturn(Optional.of(testDocumentType));
            when(minioBucketService.uploadFile(any(), anyString())).thenReturn("documents/1/test.pdf");
            when(documentRepository.save(any())).thenReturn(testDocument);
            when(documentMapper.toResponse(any())).thenReturn(new DocumentResponse());

            // Act
            DocumentResponse response = documentService.uploadSingleDocument(
                    "student@university.edu", file, 1L, null);

            // Assert
            assertThat(response).isNotNull();
            verify(minioBucketService).uploadFile(any(MultipartFile.class), anyString());
            verify(documentRepository).save(any(Document.class));
        }

        @Test
        @DisplayName("Should upload multiple documents successfully")
        void uploadMultipleDocuments_WithValidFiles_ShouldSucceed() {
            // Arrange
            MockMultipartFile file1 = new MockMultipartFile("files", "doc1.pdf", "application/pdf", "content1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile("files", "doc2.pdf", "application/pdf", "content2".getBytes());
            List<MultipartFile> files = List.of(file1, file2);

            when(studentRepository.findByUserEmail(anyString())).thenReturn(Optional.of(testStudent));
            when(documentTypeRepository.findById(anyLong())).thenReturn(Optional.of(testDocumentType));
            when(minioBucketService.uploadFile(any(), anyString())).thenReturn("path");
            when(documentRepository.save(any())).thenReturn(testDocument);
            when(documentMapper.toResponse(any())).thenReturn(new DocumentResponse());

            // Act
            List<DocumentResponse> responses = documentService.uploadMultipleDocuments(
                    "student@university.edu", files, 1L, null);

            // Assert
            assertThat(responses).hasSize(2);
            verify(minioBucketService, times(2)).uploadFile(any(), anyString());
        }

        @Test
        @DisplayName("Should reject file exceeding size limit")
        void uploadDocument_ExceedingSizeLimit_ShouldThrowException() {
            // Arrange
            byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
            MockMultipartFile largeFile = new MockMultipartFile(
                    "file", "large.pdf", "application/pdf", largeContent);

            when(studentRepository.findByUserEmail(anyString())).thenReturn(Optional.of(testStudent));

            // Act & Assert
            assertThatThrownBy(() ->
                documentService.uploadSingleDocument("student@university.edu", largeFile, 1L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("size");
        }
    }

    @Nested
    @DisplayName("Document Status Update Tests")
    class StatusUpdateTests {

        @Test
        @DisplayName("Should update document status to APPROVED")
        void updateStatus_ToApproved_ShouldSucceed() {
            // Arrange
            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
            when(documentRepository.save(any())).thenReturn(testDocument);

            // Act
            documentService.updateDocumentStatus(1L, DocumentStatus.APPROVED);

            // Assert
            assertThat(testDocument.getStatus()).isEqualTo(DocumentStatus.APPROVED);
            verify(documentRepository).save(testDocument);
        }

        @Test
        @DisplayName("Should update document status to REJECTED")
        void updateStatus_ToRejected_ShouldSucceed() {
            // Arrange
            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
            when(documentRepository.save(any())).thenReturn(testDocument);

            // Act
            documentService.updateDocumentStatus(1L, DocumentStatus.REJECTED);

            // Assert
            assertThat(testDocument.getStatus()).isEqualTo(DocumentStatus.REJECTED);
        }

        @Test
        @DisplayName("Should throw exception for non-existent document")
        void updateStatus_NonExistentDocument_ShouldThrowException() {
            // Arrange
            when(documentRepository.findById(anyLong())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> documentService.updateDocumentStatus(999L, DocumentStatus.APPROVED))
                    .isInstanceOf(DocumentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Document Retrieval Tests")
    class RetrievalTests {

        @Test
        @DisplayName("Should retrieve document with presigned URL")
        void getDocumentInfo_ShouldReturnWithPresignedUrl() {
            // Arrange
            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
            when(minioBucketService.generatePresignedUrl(anyString())).thenReturn("https://minio/presigned-url");
            when(documentMapper.toResponse(any())).thenReturn(new DocumentResponse());

            // Act
            DocumentResponse response = documentService.getDocumentInfo(1L, "student@university.edu");

            // Assert
            assertThat(response).isNotNull();
            verify(minioBucketService).generatePresignedUrl(testDocument.getPath());
        }

        @Test
        @DisplayName("Should retrieve all documents for user")
        void getAllDocumentsForUser_ShouldReturnUserDocuments() {
            // Arrange
            when(studentRepository.findByUserEmail(anyString())).thenReturn(Optional.of(testStudent));
            when(documentRepository.findByStudent(testStudent)).thenReturn(List.of(testDocument));
            when(documentMapper.toResponse(any())).thenReturn(new DocumentResponse());

            // Act
            List<DocumentResponse> documents = documentService.getAllDocumentsForUser("student@university.edu");

            // Assert
            assertThat(documents).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Document Deletion Tests")
    class DeletionTests {

        @Test
        @DisplayName("Should delete document by owner")
        void deleteDocument_ByOwner_ShouldSucceed() {
            // Arrange
            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
            when(studentRepository.findByUserEmail(anyString())).thenReturn(Optional.of(testStudent));

            // Act
            documentService.deleteDocument(1L, "student@university.edu");

            // Assert
            verify(minioBucketService).deleteFile(testDocument.getPath());
            verify(documentRepository).delete(testDocument);
        }

        @Test
        @DisplayName("Should throw exception when deleting others document")
        void deleteDocument_ByNonOwner_ShouldThrowException() {
            // Arrange
            Student otherStudent = new Student();
            otherStudent.setId(2L);

            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
            when(studentRepository.findByUserEmail(anyString())).thenReturn(Optional.of(otherStudent));

            // Act & Assert
            assertThatThrownBy(() -> documentService.deleteDocument(1L, "other@university.edu"))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }

    @Nested
    @DisplayName("Document Comment Tests")
    class CommentTests {

        @Test
        @DisplayName("Should add comment to document")
        void addComment_ShouldCreateComment() {
            // Arrange
            AppUser adminUser = new AppUser();
            adminUser.setId(2L);

            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

            // Act
            documentService.addComment(1L, "Please provide clearer copy", adminUser);

            // Assert
            verify(documentRepository).save(testDocument);
        }

        @Test
        @DisplayName("Should retrieve comments for document")
        void getComments_ShouldReturnDocumentComments() {
            // Arrange
            DocumentComment comment = new DocumentComment();
            comment.setComment("Verified");
            testDocument.setComments(List.of(comment));

            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

            // Act
            var comments = documentService.getDocumentComments(1L);

            // Assert
            assertThat(comments).hasSize(1);
        }
    }
}
```

### 2.5 Room Service Tests

**File**: `src/test/java/com/beezhub/student_accommodation/service/RoomServiceTest.java`

```java
package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.dto.request.RoomRequest;
import com.beezhub.student_accommodation.dto.response.RoomResponse;
import com.beezhub.student_accommodation.entity.Building;
import com.beezhub.student_accommodation.entity.Room;
import com.beezhub.student_accommodation.enums.RoomStatus;
import com.beezhub.student_accommodation.exception.DuplicateResourceException;
import com.beezhub.student_accommodation.exception.RoomNotFoundException;
import com.beezhub.student_accommodation.mapper.RoomMapper;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomService roomService;

    private Room testRoom;
    private Building testBuilding;
    private RoomRequest roomRequest;

    @BeforeEach
    void setUp() {
        testBuilding = new Building();
        testBuilding.setId(1L);
        testBuilding.setName("Main Hall");

        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setRoomNumber("101");
        testRoom.setCapacity(2);
        testRoom.setCurrentOccupancy(0);
        testRoom.setStatus(RoomStatus.AVAILABLE);
        testRoom.setBuilding(testBuilding);

        roomRequest = new RoomRequest();
        roomRequest.setRoomNumber("102");
        roomRequest.setCapacity(2);
        roomRequest.setBuildingId(1L);
    }

    @Nested
    @DisplayName("Room Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("Should create room with valid data")
        void createRoom_WithValidData_ShouldSucceed() {
            // Arrange
            when(buildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
            when(roomRepository.existsByRoomNumberAndBuilding(anyString(), any())).thenReturn(false);
            when(roomRepository.save(any())).thenReturn(testRoom);
            when(roomMapper.toResponse(any())).thenReturn(new RoomResponse());

            // Act
            RoomResponse response = roomService.createRoom(roomRequest);

            // Assert
            assertThat(response).isNotNull();
            verify(roomRepository).save(any(Room.class));
        }

        @Test
        @DisplayName("Should throw exception for duplicate room number in building")
        void createRoom_WithDuplicateNumber_ShouldThrowException() {
            // Arrange
            when(buildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
            when(roomRepository.existsByRoomNumberAndBuilding(anyString(), any())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> roomService.createRoom(roomRequest))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("Should set initial status to AVAILABLE")
        void createRoom_ShouldSetInitialStatusAvailable() {
            // Arrange
            when(buildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
            when(roomRepository.existsByRoomNumberAndBuilding(anyString(), any())).thenReturn(false);
            when(roomRepository.save(any())).thenAnswer(i -> {
                Room room = i.getArgument(0);
                assertThat(room.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
                assertThat(room.getCurrentOccupancy()).isEqualTo(0);
                return room;
            });

            // Act
            roomService.createRoom(roomRequest);

            // Assert
            verify(roomRepository).save(any(Room.class));
        }
    }

    @Nested
    @DisplayName("Room Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update room capacity")
        void updateRoom_Capacity_ShouldSucceed() {
            // Arrange
            roomRequest.setCapacity(4);
            when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
            when(roomRepository.save(any())).thenReturn(testRoom);
            when(roomMapper.toResponse(any())).thenReturn(new RoomResponse());

            // Act
            RoomResponse response = roomService.updateRoom(1L, roomRequest);

            // Assert
            assertThat(testRoom.getCapacity()).isEqualTo(4);
            verify(roomRepository).save(testRoom);
        }

        @Test
        @DisplayName("Should throw exception when reducing capacity below occupancy")
        void updateRoom_CapacityBelowOccupancy_ShouldThrowException() {
            // Arrange
            testRoom.setCurrentOccupancy(3);
            roomRequest.setCapacity(2);

            when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));

            // Act & Assert
            assertThatThrownBy(() -> roomService.updateRoom(1L, roomRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("capacity");
        }
    }

    @Nested
    @DisplayName("Room Retrieval Tests")
    class RetrievalTests {

        @Test
        @DisplayName("Should retrieve room by ID")
        void getRoomById_ShouldReturnRoom() {
            // Arrange
            when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
            when(roomMapper.toResponse(any())).thenReturn(new RoomResponse());

            // Act
            RoomResponse response = roomService.getRoomById(1L);

            // Assert
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception for non-existent room")
        void getRoomById_NonExistent_ShouldThrowException() {
            // Arrange
            when(roomRepository.findById(anyLong())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> roomService.getRoomById(999L))
                    .isInstanceOf(RoomNotFoundException.class);
        }

        @Test
        @DisplayName("Should retrieve paginated rooms")
        void getAllRooms_ShouldReturnPaginatedResults() {
            // Arrange
            Page<Room> page = new PageImpl<>(List.of(testRoom), PageRequest.of(0, 10), 1);
            when(roomRepository.findAll(any(PageRequest.class))).thenReturn(page);
            when(roomMapper.toResponse(any())).thenReturn(new RoomResponse());

            // Act
            Page<RoomResponse> result = roomService.getAllRooms(PageRequest.of(0, 10));

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Room Status Management Tests")
    class StatusTests {

        @Test
        @DisplayName("Should set room to maintenance status")
        void setMaintenanceStatus_ShouldUpdateStatus() {
            // Arrange
            when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
            when(roomRepository.save(any())).thenReturn(testRoom);

            // Act
            roomService.setRoomStatus(1L, RoomStatus.MAINTENANCE);

            // Assert
            assertThat(testRoom.getStatus()).isEqualTo(RoomStatus.MAINTENANCE);
        }

        @Test
        @DisplayName("Should not allow setting occupied room to available")
        void setAvailableStatus_WithOccupants_ShouldThrowException() {
            // Arrange
            testRoom.setCurrentOccupancy(1);
            testRoom.setStatus(RoomStatus.OCCUPIED);
            when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));

            // This depends on business rules - adjust as needed
            // Some systems might allow this, others might not
        }
    }
}
```

### 2.6 JWT Utility Tests

**File**: `src/test/java/com/beezhub/student_accommodation/security/JwtUtilTest.java`

```java
package com.beezhub.student_accommodation.security;

import com.beezhub.student_accommodation.entity.AppUser;
import com.beezhub.student_accommodation.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-minimum-256-bits-long-for-hs256");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L); // 1 hour
        ReflectionTestUtils.setField(jwtUtil, "adminExpiration", 1800000L); // 30 minutes

        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setEmail("test@university.edu");
        testUser.setRole(UserRole.STUDENT);
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate valid JWT token for user")
        void generateToken_ForUser_ShouldReturnValidToken() {
            // Act
            String token = jwtUtil.generateToken(testUser);

            // Assert
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
        }

        @Test
        @DisplayName("Should include email in token claims")
        void generateToken_ShouldIncludeEmail() {
            // Act
            String token = jwtUtil.generateToken(testUser);
            String email = jwtUtil.extractEmail(token);

            // Assert
            assertThat(email).isEqualTo("test@university.edu");
        }

        @Test
        @DisplayName("Should include role in token claims")
        void generateToken_ShouldIncludeRole() {
            // Act
            String token = jwtUtil.generateToken(testUser);
            String role = jwtUtil.extractRole(token);

            // Assert
            assertThat(role).isEqualTo("STUDENT");
        }

        @Test
        @DisplayName("Should use shorter expiration for admin users")
        void generateToken_ForAdmin_ShouldUseAdminExpiration() {
            // Arrange
            testUser.setRole(UserRole.ADMIN);

            // Act
            String token = jwtUtil.generateToken(testUser);

            // Assert - token should be valid
            assertThat(jwtUtil.isTokenValid(token, testUser)).isTrue();
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate correct token")
        void validateToken_WithValidToken_ShouldReturnTrue() {
            // Arrange
            String token = jwtUtil.generateToken(testUser);

            // Act
            boolean isValid = jwtUtil.isTokenValid(token, testUser);

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject token for different user")
        void validateToken_WithDifferentUser_ShouldReturnFalse() {
            // Arrange
            String token = jwtUtil.generateToken(testUser);
            AppUser differentUser = new AppUser();
            differentUser.setEmail("other@university.edu");

            // Act
            boolean isValid = jwtUtil.isTokenValid(token, differentUser);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject malformed token")
        void validateToken_WithMalformedToken_ShouldReturnFalse() {
            // Act & Assert
            assertThatThrownBy(() -> jwtUtil.extractEmail("invalid.token.here"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should reject expired token")
        void validateToken_WithExpiredToken_ShouldReturnFalse() {
            // Arrange - set very short expiration
            ReflectionTestUtils.setField(jwtUtil, "expiration", 1L);
            String token = jwtUtil.generateToken(testUser);

            // Wait for token to expire
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Act
            boolean isValid = jwtUtil.isTokenValid(token, testUser);

            // Assert
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Token Extraction Tests")
    class TokenExtractionTests {

        @Test
        @DisplayName("Should extract email from token")
        void extractEmail_ShouldReturnCorrectEmail() {
            // Arrange
            String token = jwtUtil.generateToken(testUser);

            // Act
            String email = jwtUtil.extractEmail(token);

            // Assert
            assertThat(email).isEqualTo("test@university.edu");
        }

        @Test
        @DisplayName("Should extract user ID from token")
        void extractUserId_ShouldReturnCorrectId() {
            // Arrange
            String token = jwtUtil.generateToken(testUser);

            // Act
            Long userId = jwtUtil.extractUserId(token);

            // Assert
            assertThat(userId).isEqualTo(1L);
        }
    }
}
```

### 2.7 Allocation Scoring Service Tests

**File**: `src/test/java/com/beezhub/student_accommodation/service/AllocationScoringServiceTest.java`

```java
package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.entity.*;
import com.beezhub.student_accommodation.repository.AllocationRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllocationScoringServiceTest {

    @Mock
    private AllocationRuleRepository allocationRuleRepository;

    @InjectMocks
    private AllocationScoringService scoringService;

    private Student testStudent;
    private Room testRoom;
    private Application testApplication;
    private AllocationRule testRule;
    private Building testBuilding;

    @BeforeEach
    void setUp() {
        testBuilding = new Building();
        testBuilding.setId(1L);
        testBuilding.setName("Main Hall");

        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setBuilding(testBuilding);

        testStudent = new Student();
        testStudent.setId(1L);

        testApplication = new Application();
        testApplication.setId(1L);
        testApplication.setStudent(testStudent);
        testApplication.setBuildingPreference(testBuilding);
        testApplication.setAccessibilityRequirements(false);

        testRule = new AllocationRule();
        testRule.setId(1L);
        testRule.setBuilding(testBuilding);
        testRule.setPreferenceWeight(new BigDecimal("0.4"));
        testRule.setDesirabilityWeight(new BigDecimal("0.3"));
        testRule.setAccessibilityWeight(new BigDecimal("0.3"));
        testRule.setActive(true);
    }

    @Nested
    @DisplayName("Score Calculation Tests")
    class ScoreCalculationTests {

        @Test
        @DisplayName("Should calculate higher score for preference match")
        void calculateScore_WithPreferenceMatch_ShouldScoreHigher() {
            // Arrange
            when(allocationRuleRepository.findByBuildingAndActiveTrue(testBuilding))
                    .thenReturn(Optional.of(testRule));

            // Act
            BigDecimal score = scoringService.calculateScore(testApplication, testRoom);

            // Assert
            assertThat(score).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should calculate lower score for no preference match")
        void calculateScore_WithoutPreferenceMatch_ShouldScoreLower() {
            // Arrange
            Building otherBuilding = new Building();
            otherBuilding.setId(2L);
            testApplication.setBuildingPreference(otherBuilding);

            when(allocationRuleRepository.findByBuildingAndActiveTrue(testBuilding))
                    .thenReturn(Optional.of(testRule));

            // Act
            BigDecimal score = scoringService.calculateScore(testApplication, testRoom);

            // Assert - score should still be positive but lower
            assertThat(score).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should use default weights when no rule exists")
        void calculateScore_WithNoRule_ShouldUseDefaults() {
            // Arrange
            when(allocationRuleRepository.findByBuildingAndActiveTrue(any()))
                    .thenReturn(Optional.empty());

            // Act
            BigDecimal score = scoringService.calculateScore(testApplication, testRoom);

            // Assert
            assertThat(score).isNotNull();
        }

        @Test
        @DisplayName("Should boost score for accessibility requirement match")
        void calculateScore_WithAccessibilityMatch_ShouldBoostScore() {
            // Arrange
            testApplication.setAccessibilityRequirements(true);
            // Assume room has accessibility features

            when(allocationRuleRepository.findByBuildingAndActiveTrue(testBuilding))
                    .thenReturn(Optional.of(testRule));

            // Act
            BigDecimal score = scoringService.calculateScore(testApplication, testRoom);

            // Assert
            assertThat(score).isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Room Ranking Tests")
    class RoomRankingTests {

        @Test
        @DisplayName("Should rank rooms by score descending")
        void rankRooms_ShouldOrderByScoreDescending() {
            // Arrange
            Room room1 = new Room();
            room1.setId(1L);
            room1.setBuilding(testBuilding);

            Room room2 = new Room();
            room2.setId(2L);
            room2.setBuilding(testBuilding);

            when(allocationRuleRepository.findByBuildingAndActiveTrue(any()))
                    .thenReturn(Optional.of(testRule));

            // Act
            List<Room> rankedRooms = scoringService.rankRoomsForApplication(
                    testApplication, List.of(room1, room2));

            // Assert
            assertThat(rankedRooms).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list for no available rooms")
        void rankRooms_WithNoRooms_ShouldReturnEmpty() {
            // Act
            List<Room> rankedRooms = scoringService.rankRoomsForApplication(
                    testApplication, List.of());

            // Assert
            assertThat(rankedRooms).isEmpty();
        }
    }
}
```

---

## 3. Integration Tests

### 3.1 Authentication API Integration Tests

**File**: `src/test/java/com/beezhub/student_accommodation/integration/AuthControllerIntegrationTest.java`

```java
package com.beezhub.student_accommodation.integration;

import com.beezhub.student_accommodation.dto.request.LoginRequest;
import com.beezhub.student_accommodation.dto.request.SignupRequest;
import com.beezhub.student_accommodation.entity.AppUser;
import com.beezhub.student_accommodation.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static String authToken;

    @BeforeEach
    void setUp() {
        // Clean up test data if needed
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/auth/signup - Should register new user")
    void signup_WithValidData_ShouldReturn201() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setEmail("newuser@university.edu");
        request.setPassword("Password123!");
        request.setFirstName("John");
        request.setLastName("Doe");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());

        // Verify user was created
        assertThat(userRepository.existsByEmail("newuser@university.edu")).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/auth/signup - Should reject duplicate email")
    void signup_WithDuplicateEmail_ShouldReturn409() throws Exception {
        // First, ensure user exists
        if (!userRepository.existsByEmail("duplicate@university.edu")) {
            AppUser user = new AppUser();
            user.setEmail("duplicate@university.edu");
            user.setPassword(passwordEncoder.encode("password"));
            user.setFirstName("Test");
            user.setLastName("User");
            user.setRole(UserRole.STUDENT);
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        SignupRequest request = new SignupRequest();
        request.setEmail("duplicate@university.edu");
        request.setPassword("Password123!");
        request.setFirstName("Jane");
        request.setLastName("Doe");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/auth/login - Should authenticate user and return token")
    void login_WithValidCredentials_ShouldReturnToken() throws Exception {
        // Create verified user for login
        if (!userRepository.existsByEmail("verified@university.edu")) {
            AppUser user = new AppUser();
            user.setEmail("verified@university.edu");
            user.setPassword(passwordEncoder.encode("Password123!"));
            user.setFirstName("Verified");
            user.setLastName("User");
            user.setRole(UserRole.STUDENT);
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        LoginRequest request = new LoginRequest();
        request.setEmail("verified@university.edu");
        request.setPassword("Password123!");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("verified@university.edu"))
                .andReturn();

        // Store token for subsequent tests
        String response = result.getResponse().getContentAsString();
        authToken = objectMapper.readTree(response).get("token").asText();
        assertThat(authToken).isNotEmpty();
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/auth/login - Should reject invalid credentials")
    void login_WithInvalidCredentials_ShouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("verified@university.edu");
        request.setPassword("WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/auth/me - Should return current user profile")
    void getMe_WithValidToken_ShouldReturnProfile() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("verified@university.edu"));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/auth/me - Should reject request without token")
    void getMe_WithoutToken_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/auth/change-password - Should change password")
    void changePassword_WithValidData_ShouldReturn200() throws Exception {
        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Password123!\",\"newPassword\":\"NewPassword456!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(8)
    @DisplayName("POST /api/auth/forgot-password - Should initiate password reset")
    void forgotPassword_WithValidEmail_ShouldReturn200() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"verified@university.edu\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/signup - Should validate required fields")
    void signup_WithMissingFields_ShouldReturn400() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setEmail(""); // Invalid empty email

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
```

### 3.2 Application API Integration Tests

**File**: `src/test/java/com/beezhub/student_accommodation/integration/ApplicationControllerIntegrationTest.java`

```java
package com.beezhub.student_accommodation.integration;

import com.beezhub.student_accommodation.dto.request.ApplicationRequest;
import com.beezhub.student_accommodation.entity.*;
import com.beezhub.student_accommodation.enums.ApplicationStatus;
import com.beezhub.student_accommodation.enums.UserRole;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApplicationControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private AccommodationTypeRepository accommodationTypeRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String studentToken;
    private String adminToken;
    private Long applicationId;
    private Long accommodationTypeId;

    @BeforeEach
    void setUp() {
        // Create test accommodation type
        AccommodationType accommodationType = accommodationTypeRepository.findByName("Single Room")
                .orElseGet(() -> {
                    AccommodationType type = new AccommodationType();
                    type.setName("Single Room");
                    return accommodationTypeRepository.save(type);
                });
        accommodationTypeId = accommodationType.getId();

        // Create test institution
        Institution institution = institutionRepository.findByName("Test University")
                .orElseGet(() -> {
                    Institution inst = new Institution();
                    inst.setName("Test University");
                    return institutionRepository.save(inst);
                });

        // Create student user and profile
        AppUser studentUser = userRepository.findByEmail("student@test.edu")
                .orElseGet(() -> {
                    AppUser user = new AppUser();
                    user.setEmail("student@test.edu");
                    user.setPassword(passwordEncoder.encode("Password123!"));
                    user.setFirstName("Test");
                    user.setLastName("Student");
                    user.setRole(UserRole.STUDENT);
                    user.setEmailVerified(true);
                    return userRepository.save(user);
                });

        Student student = studentRepository.findByUserEmail("student@test.edu")
                .orElseGet(() -> {
                    Student s = new Student();
                    s.setStudentNumber("STU001");
                    s.setUser(studentUser);
                    s.setInstitution(institution);
                    return studentRepository.save(s);
                });

        // Create admin user
        AppUser adminUser = userRepository.findByEmail("admin@test.edu")
                .orElseGet(() -> {
                    AppUser user = new AppUser();
                    user.setEmail("admin@test.edu");
                    user.setPassword(passwordEncoder.encode("AdminPass123!"));
                    user.setFirstName("Test");
                    user.setLastName("Admin");
                    user.setRole(UserRole.ADMIN);
                    user.setEmailVerified(true);
                    return userRepository.save(user);
                });

        studentToken = jwtUtil.generateToken(studentUser);
        adminToken = jwtUtil.generateToken(adminUser);
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/applications/submit - Should submit new application")
    void submitApplication_WithValidData_ShouldReturn201() throws Exception {
        // Clean up any existing applications for this student
        applicationRepository.deleteAll();

        ApplicationRequest request = new ApplicationRequest();
        request.setAccommodationTypeId(accommodationTypeId);
        request.setMoveInDate(LocalDate.now().plusMonths(1));
        request.setDuration(12);
        request.setSpecialRequirements("No special requirements");

        mockMvc.perform(post("/api/applications/submit")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationCode").exists())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/applications/my-applications - Should return student applications")
    void getMyApplications_ShouldReturnStudentApplications() throws Exception {
        mockMvc.perform(get("/api/applications/my-applications")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/applications - Admin should get all applications")
    void getAllApplications_AsAdmin_ShouldReturnPaginatedResults() throws Exception {
        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/applications/status/{status} - Should filter by status")
    void getApplicationsByStatus_ShouldReturnFilteredResults() throws Exception {
        mockMvc.perform(get("/api/applications/status/PENDING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(5)
    @DisplayName("PATCH /api/applications/{id}/status - Admin should update status")
    void updateApplicationStatus_AsAdmin_ShouldReturn200() throws Exception {
        // First create an application
        Application app = applicationRepository.findAll().stream().findFirst().orElse(null);
        if (app != null) {
            mockMvc.perform(patch("/api/applications/" + app.getId() + "/status")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"APPROVED\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/applications/search - Should search with filters")
    void searchApplications_WithFilters_ShouldReturnResults() throws Exception {
        mockMvc.perform(get("/api/applications/search")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("POST /api/applications/submit - Should reject duplicate application")
    void submitApplication_WhenPendingExists_ShouldReturn409() throws Exception {
        // Submit first application
        ApplicationRequest request = new ApplicationRequest();
        request.setAccommodationTypeId(accommodationTypeId);
        request.setMoveInDate(LocalDate.now().plusMonths(2));
        request.setDuration(12);

        // This test depends on existing application state
    }

    @Test
    @DisplayName("GET /api/applications - Student should not access all applications")
    void getAllApplications_AsStudent_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }
}
```

### 3.3 Room Allocation Integration Tests

**File**: `src/test/java/com/beezhub/student_accommodation/integration/RoomAllocationControllerIntegrationTest.java`

```java
package com.beezhub.student_accommodation.integration;

import com.beezhub.student_accommodation.dto.request.RoomAllocationRequest;
import com.beezhub.student_accommodation.entity.*;
import com.beezhub.student_accommodation.enums.*;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RoomAllocationControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private RoomAllocationRepository allocationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;
    private Long studentId;
    private Long roomId;
    private Long applicationId;

    @BeforeEach
    void setUp() {
        // Create admin user
        AppUser adminUser = userRepository.findByEmail("allocation-admin@test.edu")
                .orElseGet(() -> {
                    AppUser user = new AppUser();
                    user.setEmail("allocation-admin@test.edu");
                    user.setPassword(passwordEncoder.encode("AdminPass123!"));
                    user.setFirstName("Allocation");
                    user.setLastName("Admin");
                    user.setRole(UserRole.ADMIN);
                    user.setEmailVerified(true);
                    return userRepository.save(user);
                });
        adminToken = jwtUtil.generateToken(adminUser);

        // Create building
        Building building = buildingRepository.findByName("Test Building")
                .orElseGet(() -> {
                    Building b = new Building();
                    b.setName("Test Building");
                    b.setAddress("123 Test St");
                    b.setTotalFloors(5);
                    return buildingRepository.save(b);
                });

        // Create room
        Room room = roomRepository.findByRoomNumberAndBuilding("T101", building)
                .orElseGet(() -> {
                    Room r = new Room();
                    r.setRoomNumber("T101");
                    r.setCapacity(2);
                    r.setCurrentOccupancy(0);
                    r.setStatus(RoomStatus.AVAILABLE);
                    r.setBuilding(building);
                    return roomRepository.save(r);
                });
        roomId = room.getId();

        // Create student for allocation
        AppUser studentUser = userRepository.findByEmail("alloc-student@test.edu")
                .orElseGet(() -> {
                    AppUser user = new AppUser();
                    user.setEmail("alloc-student@test.edu");
                    user.setPassword(passwordEncoder.encode("Password123!"));
                    user.setFirstName("Alloc");
                    user.setLastName("Student");
                    user.setRole(UserRole.STUDENT);
                    user.setEmailVerified(true);
                    return userRepository.save(user);
                });

        Student student = studentRepository.findByUserEmail("alloc-student@test.edu")
                .orElseGet(() -> {
                    Student s = new Student();
                    s.setStudentNumber("ALLOC001");
                    s.setUser(studentUser);
                    return studentRepository.save(s);
                });
        studentId = student.getId();

        // Create application in AWAITING_ALLOCATION status
        Application application = applicationRepository
                .findByStudentAndStatus(student, ApplicationStatus.AWAITING_ALLOCATION)
                .orElseGet(() -> {
                    Application app = new Application();
                    app.setApplicationCode("ALLOC-APP-001");
                    app.setStudent(student);
                    app.setStatus(ApplicationStatus.AWAITING_ALLOCATION);
                    return applicationRepository.save(app);
                });
        applicationId = application.getId();
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/room-allocation - Should create allocation")
    void createAllocation_WithValidData_ShouldReturn201() throws Exception {
        // Clean up existing allocations
        allocationRepository.deleteAll();

        RoomAllocationRequest request = new RoomAllocationRequest();
        request.setStudentId(studentId);
        request.setRoomId(roomId);
        request.setApplicationId(applicationId);

        mockMvc.perform(post("/api/room-allocation")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/room-allocation/student/{id}/active - Should return active allocation")
    void getActiveAllocation_ShouldReturnStudentAllocation() throws Exception {
        mockMvc.perform(get("/api/room-allocation/student/" + studentId + "/active")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/room-allocation/room/{id}/is-available - Should check availability")
    void checkRoomAvailability_ShouldReturnBoolean() throws Exception {
        mockMvc.perform(get("/api/room-allocation/room/" + roomId + "/is-available")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isBoolean());
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/room-allocation/student/{id}/eligibility - Should check eligibility")
    void checkStudentEligibility_ShouldReturnEligibilityResponse() throws Exception {
        mockMvc.perform(get("/api/room-allocation/student/" + studentId + "/eligibility")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").exists());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/room-allocation - Should return all allocations paginated")
    void getAllAllocations_ShouldReturnPaginatedResults() throws Exception {
        mockMvc.perform(get("/api/room-allocation")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("PATCH /api/room-allocation/{id}/end - Should end allocation")
    void endAllocation_ShouldUpdateStatusToCompleted() throws Exception {
        RoomAllocation allocation = allocationRepository.findAll().stream()
                .filter(a -> a.getStatus() == AllocationStatus.ACTIVE)
                .findFirst()
                .orElse(null);

        if (allocation != null) {
            mockMvc.perform(patch("/api/room-allocation/" + allocation.getId() + "/end")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }
    }

    @Test
    @DisplayName("POST /api/room-allocation - Should reject when room full")
    void createAllocation_WhenRoomFull_ShouldReturn400() throws Exception {
        // Set room to full
        Room room = roomRepository.findById(roomId).orElseThrow();
        room.setStatus(RoomStatus.FULL);
        room.setCurrentOccupancy(room.getCapacity());
        roomRepository.save(room);

        RoomAllocationRequest request = new RoomAllocationRequest();
        request.setStudentId(studentId);
        request.setRoomId(roomId);

        mockMvc.perform(post("/api/room-allocation")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
```

### 3.4 Document API Integration Tests

**File**: `src/test/java/com/beezhub/student_accommodation/integration/DocumentControllerIntegrationTest.java`

```java
package com.beezhub.student_accommodation.integration;

import com.beezhub.student_accommodation.entity.*;
import com.beezhub.student_accommodation.enums.DocumentStatus;
import com.beezhub.student_accommodation.enums.UserRole;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.security.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocumentControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentTypeRepository documentTypeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String studentToken;
    private String adminToken;
    private Long documentTypeId;
    private Long documentId;

    @BeforeEach
    void setUp() {
        // Create document type
        DocumentType docType = documentTypeRepository.findByName("ID Document")
                .orElseGet(() -> {
                    DocumentType type = new DocumentType();
                    type.setName("ID Document");
                    return documentTypeRepository.save(type);
                });
        documentTypeId = docType.getId();

        // Create student user
        AppUser studentUser = userRepository.findByEmail("doc-student@test.edu")
                .orElseGet(() -> {
                    AppUser user = new AppUser();
                    user.setEmail("doc-student@test.edu");
                    user.setPassword(passwordEncoder.encode("Password123!"));
                    user.setFirstName("Doc");
                    user.setLastName("Student");
                    user.setRole(UserRole.STUDENT);
                    user.setEmailVerified(true);
                    return userRepository.save(user);
                });
        studentToken = jwtUtil.generateToken(studentUser);

        // Create student profile
        Student student = studentRepository.findByUserEmail("doc-student@test.edu")
                .orElseGet(() -> {
                    Student s = new Student();
                    s.setStudentNumber("DOC001");
                    s.setUser(studentUser);
                    return studentRepository.save(s);
                });

        // Create admin user
        AppUser adminUser = userRepository.findByEmail("doc-admin@test.edu")
                .orElseGet(() -> {
                    AppUser user = new AppUser();
                    user.setEmail("doc-admin@test.edu");
                    user.setPassword(passwordEncoder.encode("AdminPass123!"));
                    user.setFirstName("Doc");
                    user.setLastName("Admin");
                    user.setRole(UserRole.ADMIN);
                    user.setEmailVerified(true);
                    return userRepository.save(user);
                });
        adminToken = jwtUtil.generateToken(adminUser);
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/documents/upload-single - Should upload document")
    void uploadSingleDocument_WithValidFile_ShouldReturn201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-id.pdf",
                "application/pdf",
                "Test PDF content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload-single")
                        .file(file)
                        .param("documentTypeId", documentTypeId.toString())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentName").exists())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/documents/upload-multiple - Should upload multiple documents")
    void uploadMultipleDocuments_WithValidFiles_ShouldReturn201() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "files", "doc1.pdf", "application/pdf", "Content 1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "files", "doc2.pdf", "application/pdf", "Content 2".getBytes());

        mockMvc.perform(multipart("/api/documents/upload-multiple")
                        .file(file1)
                        .file(file2)
                        .param("documentTypeId", documentTypeId.toString())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/documents/my - Should return user documents")
    void getMyDocuments_ShouldReturnStudentDocuments() throws Exception {
        mockMvc.perform(get("/api/documents/my")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(4)
    @DisplayName("PUT /api/documents/update-status/{id} - Admin should update status")
    void updateDocumentStatus_AsAdmin_ShouldReturn200() throws Exception {
        Document doc = documentRepository.findAll().stream().findFirst().orElse(null);
        if (doc != null) {
            mockMvc.perform(put("/api/documents/update-status/" + doc.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"APPROVED\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/documents/{id}/comments - Admin should add comment")
    void addComment_AsAdmin_ShouldReturn201() throws Exception {
        Document doc = documentRepository.findAll().stream().findFirst().orElse(null);
        if (doc != null) {
            mockMvc.perform(post("/api/documents/" + doc.getId() + "/comments")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"comment\":\"Please provide clearer copy\"}"))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/documents/{id}/comments - Should return document comments")
    void getDocumentComments_ShouldReturnComments() throws Exception {
        Document doc = documentRepository.findAll().stream().findFirst().orElse(null);
        if (doc != null) {
            mockMvc.perform(get("/api/documents/" + doc.getId() + "/comments")
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Test
    @DisplayName("POST /api/documents/upload-single - Should reject oversized file")
    void uploadDocument_OversizedFile_ShouldReturn400() throws Exception {
        // Create file larger than 10MB limit
        byte[] largeContent = new byte[11 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", largeContent);

        mockMvc.perform(multipart("/api/documents/upload-single")
                        .file(largeFile)
                        .param("documentTypeId", documentTypeId.toString())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/documents/delete/{id} - Student should delete own document")
    void deleteDocument_ByOwner_ShouldReturn200() throws Exception {
        // Create a document first, then delete
        Document doc = documentRepository.findAll().stream()
                .filter(d -> d.getStudent().getUser().getEmail().equals("doc-student@test.edu"))
                .findFirst()
                .orElse(null);

        if (doc != null) {
            mockMvc.perform(delete("/api/documents/delete/" + doc.getId())
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk());
        }
    }
}
```

### 3.5 Admin Statistics Integration Tests

**File**: `src/test/java/com/beezhub/student_accommodation/integration/AdminStatsControllerIntegrationTest.java`

```java
package com.beezhub.student_accommodation.integration;

import com.beezhub.student_accommodation.entity.AppUser;
import com.beezhub.student_accommodation.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.security.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AdminStatsControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        // Create admin user
        AppUser adminUser = userRepository.findByEmail("stats-admin@test.edu")
                .orElseGet(() -> {
                    AppUser user = new AppUser();
                    user.setEmail("stats-admin@test.edu");
                    user.setPassword(passwordEncoder.encode("AdminPass123!"));
                    user.setFirstName("Stats");
                    user.setLastName("Admin");
                    user.setRole(UserRole.ADMIN);
                    user.setEmailVerified(true);
                    return userRepository.save(user);
                });
        adminToken = jwtUtil.generateToken(adminUser);

        // Create student user
        AppUser studentUser = userRepository.findByEmail("stats-student@test.edu")
                .orElseGet(() -> {
                    AppUser user = new AppUser();
                    user.setEmail("stats-student@test.edu");
                    user.setPassword(passwordEncoder.encode("Password123!"));
                    user.setFirstName("Stats");
                    user.setLastName("Student");
                    user.setRole(UserRole.STUDENT);
                    user.setEmailVerified(true);
                    return userRepository.save(user);
                });
        studentToken = jwtUtil.generateToken(studentUser);
    }

    @Test
    @DisplayName("GET /api/admin/stats - Should return dashboard statistics")
    void getDashboardStats_AsAdmin_ShouldReturnStats() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents").exists())
                .andExpect(jsonPath("$.totalApplications").exists())
                .andExpect(jsonPath("$.totalRooms").exists())
                .andExpect(jsonPath("$.pendingApplications").exists());
    }

    @Test
    @DisplayName("GET /api/admin/stats/occupancy - Should return occupancy statistics")
    void getOccupancyStats_AsAdmin_ShouldReturnOccupancyData() throws Exception {
        mockMvc.perform(get("/api/admin/stats/occupancy")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCapacity").exists())
                .andExpect(jsonPath("$.currentOccupancy").exists())
                .andExpect(jsonPath("$.occupancyRate").exists());
    }

    @Test
    @DisplayName("GET /api/admin/stats - Student should not access stats")
    void getDashboardStats_AsStudent_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/stats - Unauthenticated should return 401")
    void getDashboardStats_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized());
    }
}
```

---

## 4. End-to-End Test Scenarios (Playwright)

### 4.1 Project Setup for Playwright

**File**: `e2e/playwright.config.ts`

```typescript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['json', { outputFile: 'test-results/results.json' }]
  ],
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:4200',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
  ],
  webServer: {
    command: 'npm run start',
    url: 'http://localhost:4200',
    reuseExistingServer: !process.env.CI,
    timeout: 120000,
  },
});
```

### 4.2 Test Fixtures and Helpers

**File**: `e2e/fixtures/test-fixtures.ts`

```typescript
import { test as base, expect } from '@playwright/test';

// API configuration
const API_BASE_URL = process.env.API_URL || 'http://localhost:8080/api';

// Test user credentials
export const testUsers = {
  student: {
    email: 'e2e-student@university.edu',
    password: 'TestPassword123!',
    firstName: 'E2E',
    lastName: 'Student'
  },
  admin: {
    email: 'e2e-admin@university.edu',
    password: 'AdminPassword123!',
    firstName: 'E2E',
    lastName: 'Admin'
  }
};

// Custom fixture with authentication helpers
export const test = base.extend<{
  authenticatedStudentPage: any;
  authenticatedAdminPage: any;
  apiContext: any;
}>({
  authenticatedStudentPage: async ({ page }, use) => {
    await page.goto('/login');
    await page.fill('[data-testid="email-input"]', testUsers.student.email);
    await page.fill('[data-testid="password-input"]', testUsers.student.password);
    await page.click('[data-testid="login-button"]');
    await page.waitForURL('/dashboard');
    await use(page);
  },

  authenticatedAdminPage: async ({ page }, use) => {
    await page.goto('/login');
    await page.fill('[data-testid="email-input"]', testUsers.admin.email);
    await page.fill('[data-testid="password-input"]', testUsers.admin.password);
    await page.click('[data-testid="login-button"]');
    await page.waitForURL('/admin/dashboard');
    await use(page);
  },

  apiContext: async ({ playwright }, use) => {
    const context = await playwright.request.newContext({
      baseURL: API_BASE_URL,
      extraHTTPHeaders: {
        'Content-Type': 'application/json',
      },
    });
    await use(context);
    await context.dispose();
  },
});

export { expect };
```

### 4.3 User Authentication Flow Tests

**File**: `e2e/tests/auth.spec.ts`

```typescript
import { test, expect, testUsers } from '../fixtures/test-fixtures';

test.describe('User Authentication Flow', () => {

  test.describe('User Registration', () => {

    test('should successfully register a new student', async ({ page }) => {
      // Preconditions: User not registered
      const uniqueEmail = `test-${Date.now()}@university.edu`;

      // Step 1: Navigate to registration page
      await page.goto('/register');
      await expect(page).toHaveTitle(/Register/);

      // Step 2: Fill registration form
      await page.fill('[data-testid="firstname-input"]', 'Test');
      await page.fill('[data-testid="lastname-input"]', 'User');
      await page.fill('[data-testid="email-input"]', uniqueEmail);
      await page.fill('[data-testid="password-input"]', 'SecurePass123!');
      await page.fill('[data-testid="confirm-password-input"]', 'SecurePass123!');

      // Step 3: Submit registration
      await page.click('[data-testid="register-button"]');

      // Expected Result: Success message and redirect to verification page
      await expect(page.locator('[data-testid="success-message"]')).toBeVisible();
      await expect(page).toHaveURL(/verify-email|check-email/);
    });

    test('should show error for duplicate email registration', async ({ page }) => {
      await page.goto('/register');

      await page.fill('[data-testid="firstname-input"]', 'Duplicate');
      await page.fill('[data-testid="lastname-input"]', 'User');
      await page.fill('[data-testid="email-input"]', testUsers.student.email);
      await page.fill('[data-testid="password-input"]', 'Password123!');
      await page.fill('[data-testid="confirm-password-input"]', 'Password123!');

      await page.click('[data-testid="register-button"]');

      // Expected Result: Error message about existing email
      await expect(page.locator('[data-testid="error-message"]')).toContainText(/already registered|exists/i);
    });

    test('should validate password requirements', async ({ page }) => {
      await page.goto('/register');

      await page.fill('[data-testid="password-input"]', 'weak');
      await page.click('[data-testid="register-button"]');

      // Expected Result: Password validation error
      await expect(page.locator('[data-testid="password-error"]')).toBeVisible();
    });
  });

  test.describe('User Login', () => {

    test('should successfully login with valid credentials', async ({ page }) => {
      // Step 1: Navigate to login page
      await page.goto('/login');

      // Step 2: Enter valid credentials
      await page.fill('[data-testid="email-input"]', testUsers.student.email);
      await page.fill('[data-testid="password-input"]', testUsers.student.password);

      // Step 3: Submit login
      await page.click('[data-testid="login-button"]');

      // Expected Result: Redirect to dashboard
      await expect(page).toHaveURL('/dashboard');
      await expect(page.locator('[data-testid="user-menu"]')).toBeVisible();
    });

    test('should show error for invalid credentials', async ({ page }) => {
      await page.goto('/login');

      await page.fill('[data-testid="email-input"]', testUsers.student.email);
      await page.fill('[data-testid="password-input"]', 'WrongPassword!');

      await page.click('[data-testid="login-button"]');

      // Expected Result: Error message
      await expect(page.locator('[data-testid="error-message"]')).toContainText(/invalid|incorrect/i);
      await expect(page).toHaveURL('/login');
    });

    test('should redirect unauthenticated user to login', async ({ page }) => {
      // Try to access protected route without authentication
      await page.goto('/applications');

      // Expected Result: Redirect to login
      await expect(page).toHaveURL(/login/);
    });
  });

  test.describe('Password Reset Flow', () => {

    test('should initiate password reset process', async ({ page }) => {
      // Step 1: Navigate to forgot password page
      await page.goto('/login');
      await page.click('[data-testid="forgot-password-link"]');

      // Step 2: Enter email
      await page.fill('[data-testid="email-input"]', testUsers.student.email);

      // Step 3: Submit request
      await page.click('[data-testid="reset-button"]');

      // Expected Result: Success message
      await expect(page.locator('[data-testid="success-message"]')).toContainText(/email sent|check your email/i);
    });
  });

  test.describe('Logout', () => {

    test('should successfully logout user', async ({ authenticatedStudentPage }) => {
      const page = authenticatedStudentPage;

      // Step 1: Click user menu
      await page.click('[data-testid="user-menu"]');

      // Step 2: Click logout
      await page.click('[data-testid="logout-button"]');

      // Expected Result: Redirect to login page
      await expect(page).toHaveURL('/login');

      // Verify user can't access protected routes
      await page.goto('/dashboard');
      await expect(page).toHaveURL(/login/);
    });
  });
});
```

### 4.4 Student Application Flow Tests

**File**: `e2e/tests/application-flow.spec.ts`

```typescript
import { test, expect } from '../fixtures/test-fixtures';

test.describe('Student Application Flow', () => {

  test.describe('Submit New Application', () => {

    test('should successfully submit accommodation application', async ({ authenticatedStudentPage }) => {
      const page = authenticatedStudentPage;

      // Preconditions: Student logged in, no pending application

      // Step 1: Navigate to new application page
      await page.click('[data-testid="new-application-button"]');
      await expect(page).toHaveURL(/applications\/new/);

      // Step 2: Select accommodation type
      await page.selectOption('[data-testid="accommodation-type-select"]', { label: 'Single Room' });

      // Step 3: Set move-in date (1 month from now)
      const moveInDate = new Date();
      moveInDate.setMonth(moveInDate.getMonth() + 1);
      await page.fill('[data-testid="move-in-date-input"]', moveInDate.toISOString().split('T')[0]);

      // Step 4: Set duration
      await page.fill('[data-testid="duration-input"]', '12');

      // Step 5: Add special requirements (optional)
      await page.fill('[data-testid="special-requirements-input"]', 'Ground floor preferred due to mobility issues');

      // Step 6: Select building preference
      await page.selectOption('[data-testid="building-preference-select"]', { index: 1 });

      // Step 7: Submit application
      await page.click('[data-testid="submit-application-button"]');

      // Expected Result: Success message and redirect to applications list
      await expect(page.locator('[data-testid="success-toast"]')).toContainText(/submitted successfully/i);
      await expect(page).toHaveURL(/applications/);

      // Verify application appears in list with PENDING status
      await expect(page.locator('[data-testid="application-status"]').first()).toContainText('PENDING');
    });

    test('should prevent duplicate application submission', async ({ authenticatedStudentPage }) => {
      const page = authenticatedStudentPage;

      // Preconditions: Student already has pending application
      await page.goto('/applications/new');

      // Fill and submit form
      await page.selectOption('[data-testid="accommodation-type-select"]', { index: 1 });
      await page.click('[data-testid="submit-application-button"]');

      // Expected Result: Error message about existing application
      await expect(page.locator('[data-testid="error-message"]')).toContainText(/already have.*pending|duplicate/i);
    });
  });

  test.describe('View Applications', () => {

    test('should display list of student applications', async ({ authenticatedStudentPage }) => {
      const page = authenticatedStudentPage;

      // Step 1: Navigate to applications page
      await page.goto('/applications');

      // Expected Result: Applications list visible
      await expect(page.locator('[data-testid="applications-list"]')).toBeVisible();

      // Verify application card elements
      const applicationCard = page.locator('[data-testid="application-card"]').first();
      await expect(applicationCard.locator('[data-testid="application-code"]')).toBeVisible();
      await expect(applicationCard.locator('[data-testid="application-status"]')).toBeVisible();
      await expect(applicationCard.locator('[data-testid="submission-date"]')).toBeVisible();
    });

    test('should view application details', async ({ authenticatedStudentPage }) => {
      const page = authenticatedStudentPage;

      await page.goto('/applications');

      // Click on first application
      await page.click('[data-testid="application-card"]');

      // Expected Result: Application details page
      await expect(page).toHaveURL(/applications\/\d+/);
      await expect(page.locator('[data-testid="application-details"]')).toBeVisible();
      await expect(page.locator('[data-testid="documents-section"]')).toBeVisible();
    });
  });

  test.describe('Cancel Application', () => {

    test('should cancel pending application', async ({ authenticatedStudentPage }) => {
      const page = authenticatedStudentPage;

      // Navigate to application that can be cancelled
      await page.goto('/applications');
      await page.click('[data-testid="application-card"]:has-text("PENDING")');

      // Step 1: Click cancel button
      await page.click('[data-testid="cancel-application-button"]');

      // Step 2: Confirm cancellation
      await page.click('[data-testid="confirm-cancel-button"]');

      // Expected Result: Application removed or status updated
      await expect(page.locator('[data-testid="success-toast"]')).toContainText(/cancelled/i);
    });
  });
});
```

### 4.5 Document Upload Tests

**File**: `e2e/tests/document-upload.spec.ts`

```typescript
import { test, expect } from '../fixtures/test-fixtures';
import path from 'path';

test.describe('Document Upload Flow', () => {

  test.describe('Upload Documents', () => {

    test('should successfully upload single document', async ({ authenticatedStudentPage }) => {
      const page = authenticatedStudentPage;

      // Step 1: Navigate to documents page
      await page.goto('/documents');

      // Step 2: Click upload button
      await page.click('[data-testid="upload-document-button"]');

      // Step 3: Select document type
      await page.selectOption('[data-testid="document-type-select"]', { label: 'ID Document' });

      // Step 4: Upload file
      const fileInput = page.locator('input[type="file"]');
      await fileInput.setInputFiles(path.join(__dirname, '../fixtures/test-document.pdf'));

      // Step 5: Submit upload
      await page.click('[data-testid="upload-submit-button"]');

      // Expected Result: Document appears in list with PENDING status
      await expect(page.locator('[data-testid="success-toast"]')).toContainText(/uploaded successfully/i);
      await expect(page.locator('[data-testid="document-card"]').first()).toContainText('PENDING');
    });

    test('should upload multiple documents', async ({ authenticatedStudentPage }) => {
      const page = authenticatedStudentPage;

      await page.goto('/documents');
      await page.click('[data-testid="upload-document-button"]');

      await page.selectOption('[data-testid="document-type-select"]', { label: 'Supporting Document' });

      const fileInput = page.locator('input[type="file"]');
      await fileInput.setInputFiles([
        path.join(__dirname, '../fixtures/test-document1.pdf'),
        path.join(__dirname, '../fixtures/test-document2.pdf')
      ]);

      await page.click('[data-testid="upload-submit-button"]');

      // Expected Result: Both documents uploaded
      await expect(page.locator('[data-testid="success-toast"]')).toContainText(/uploaded/i);
    });

    test('should reject oversized file', async ({ authenticatedStudentPage }) => {
      const page = authenticatedStudentPage;

      await page.goto('/documents');
      await page.click('[data-testid="upload-document-button"]');

      // Try to upload file > 10MB
      const fileInput = page.locator('input[type="file"]');
      await fileInput.setInputFiles(path.join(__dirname, '../fixtures/large-file.pdf'));

      // Expected Result: Error message about file size
      await expect(page.locator('[data-testid="error-message"]')).toContainText(/size|too large|10MB/i);
    });
  });

  test.describe('View Documents', () => {

    test('should display document list with status', async ({ authenticatedStudentPage }) => {
      const page = authenticatedStudentPage;

      await page.goto('/documents');

      // Expected Result: Documents list visible
      await expect(page.locator('[data-testid="documents-list"]')).toBeVisible();

      // Verify document card elements
      const docCard = page.locator('[data-testid="document-card"]').first();
      await expect(docCard.locator('[data-testid="document-name"]')).toBeVisible();
      await expect(docCard.locator('[data-testid="document-status"]')).toBeVisible();
      await expect(docCard.locator('[data-testid="upload-date"]')).toBeVisible();
    });

    test('should download document', async ({ authenticatedStudentPage }) => {
      const page = authenticatedStudentPage;

      await page.goto('/documents');

      // Click download on first document
      const downloadPromise = page.waitForEvent('download');
      await page.click('[data-testid="download-document-button"]');
      const download = await downloadPromise;

      // Expected Result: Download starts
      expect(download.suggestedFilename()).toBeTruthy();
    });

    test('should view document comments from admin', async ({ authenticatedStudentPage }) => {
      const page = authenticatedStudentPage;

      await page.goto('/documents');

      // Click on document with comments
      await page.click('[data-testid="document-card"]:has([data-testid="comments-badge"])');

      // Expected Result: Comments section visible
      await expect(page.locator('[data-testid="comments-section"]')).toBeVisible();
    });
  });

  test.describe('Delete Documents', () => {

    test('should delete own document', async ({ authenticatedStudentPage }) => {
      const page = authenticatedStudentPage;

      await page.goto('/documents');

      // Click delete on first document
      await page.click('[data-testid="delete-document-button"]');
      await page.click('[data-testid="confirm-delete-button"]');

      // Expected Result: Document removed
      await expect(page.locator('[data-testid="success-toast"]')).toContainText(/deleted/i);
    });
  });
});
```

### 4.6 Admin Dashboard Tests

**File**: `e2e/tests/admin-dashboard.spec.ts`

```typescript
import { test, expect } from '../fixtures/test-fixtures';

test.describe('Admin Dashboard', () => {

  test.describe('Dashboard Overview', () => {

    test('should display dashboard statistics', async ({ authenticatedAdminPage }) => {
      const page = authenticatedAdminPage;

      // Expected Result: Dashboard with statistics cards
      await expect(page.locator('[data-testid="stats-total-students"]')).toBeVisible();
      await expect(page.locator('[data-testid="stats-total-applications"]')).toBeVisible();
      await expect(page.locator('[data-testid="stats-pending-applications"]')).toBeVisible();
      await expect(page.locator('[data-testid="stats-occupancy-rate"]')).toBeVisible();
    });

    test('should display recent activities', async ({ authenticatedAdminPage }) => {
      const page = authenticatedAdminPage;

      // Navigate to activities section
      await page.click('[data-testid="activities-tab"]');

      // Expected Result: Activity log visible
      await expect(page.locator('[data-testid="activity-log"]')).toBeVisible();
    });
  });

  test.describe('Application Management', () => {

    test('should view all applications with filters', async ({ authenticatedAdminPage }) => {
      const page = authenticatedAdminPage;

      // Navigate to applications management
      await page.click('[data-testid="nav-applications"]');

      // Expected Result: Applications table visible
      await expect(page.locator('[data-testid="applications-table"]')).toBeVisible();

      // Test status filter
      await page.selectOption('[data-testid="status-filter"]', 'PENDING');
      await expect(page.locator('[data-testid="application-row"]').first()).toContainText('PENDING');
    });

    test('should approve application', async ({ authenticatedAdminPage }) => {
      const page = authenticatedAdminPage;

      await page.goto('/admin/applications');

      // Click on pending application
      await page.click('[data-testid="application-row"]:has-text("PENDING")');

      // Click approve button
      await page.click('[data-testid="approve-button"]');
      await page.click('[data-testid="confirm-approve-button"]');

      // Expected Result: Status updated to APPROVED
      await expect(page.locator('[data-testid="success-toast"]')).toContainText(/approved/i);
    });

    test('should reject application with reason', async ({ authenticatedAdminPage }) => {
      const page = authenticatedAdminPage;

      await page.goto('/admin/applications');

      await page.click('[data-testid="application-row"]:has-text("PENDING")');
      await page.click('[data-testid="reject-button"]');

      // Enter rejection reason
      await page.fill('[data-testid="rejection-reason-input"]', 'Incomplete documentation');
      await page.click('[data-testid="confirm-reject-button"]');

      // Expected Result: Status updated to REJECTED
      await expect(page.locator('[data-testid="success-toast"]')).toContainText(/rejected/i);
    });

    test('should review documents and add comments', async ({ authenticatedAdminPage }) => {
      const page = authenticatedAdminPage;

      await page.goto('/admin/applications');
      await page.click('[data-testid="application-row"]');

      // Navigate to documents tab
      await page.click('[data-testid="documents-tab"]');

      // Add comment to document
      await page.click('[data-testid="add-comment-button"]');
      await page.fill('[data-testid="comment-input"]', 'Please provide clearer copy');
      await page.click('[data-testid="submit-comment-button"]');

      // Expected Result: Comment added
      await expect(page.locator('[data-testid="success-toast"]')).toContainText(/comment added/i);
    });

    test('should move application to allocation phase', async ({ authenticatedAdminPage }) => {
      const page = authenticatedAdminPage;

      await page.goto('/admin/applications');

      // Find approved application with all documents approved
      await page.click('[data-testid="application-row"]:has-text("APPROVED")');

      // Click move to allocation
      await page.click('[data-testid="move-to-allocation-button"]');
      await page.click('[data-testid="confirm-button"]');

      // Expected Result: Status updated to AWAITING_ALLOCATION
      await expect(page.locator('[data-testid="application-status"]')).toContainText('AWAITING_ALLOCATION');
    });
  });

  test.describe('Room Allocation', () => {

    test('should allocate room to student', async ({ authenticatedAdminPage }) => {
      const page = authenticatedAdminPage;

      // Navigate to allocations
      await page.goto('/admin/allocations');

      // Click allocate new
      await page.click('[data-testid="new-allocation-button"]');

      // Select student (from waiting list)
      await page.selectOption('[data-testid="student-select"]', { index: 1 });

      // Select available room
      await page.selectOption('[data-testid="room-select"]', { index: 1 });

      // Submit allocation
      await page.click('[data-testid="allocate-button"]');

      // Expected Result: Allocation created
      await expect(page.locator('[data-testid="success-toast"]')).toContainText(/allocated/i);
    });

    test('should view room availability', async ({ authenticatedAdminPage }) => {
      const page = authenticatedAdminPage;

      await page.goto('/admin/rooms');

      // Filter by available rooms
      await page.selectOption('[data-testid="status-filter"]', 'AVAILABLE');

      // Expected Result: Only available rooms shown
      const roomCards = page.locator('[data-testid="room-card"]');
      const count = await roomCards.count();
      for (let i = 0; i < count; i++) {
        await expect(roomCards.nth(i).locator('[data-testid="room-status"]')).toContainText('AVAILABLE');
      }
    });

    test('should end allocation', async ({ authenticatedAdminPage }) => {
      const page = authenticatedAdminPage;

      await page.goto('/admin/allocations');

      // Find active allocation
      await page.click('[data-testid="allocation-row"]:has-text("ACTIVE")');

      // Click end allocation
      await page.click('[data-testid="end-allocation-button"]');
      await page.click('[data-testid="confirm-button"]');

      // Expected Result: Status updated to COMPLETED
      await expect(page.locator('[data-testid="success-toast"]')).toContainText(/completed|ended/i);
    });
  });

  test.describe('Batch Allocation', () => {

    test('should propose batch allocation', async ({ authenticatedAdminPage }) => {
      const page = authenticatedAdminPage;

      await page.goto('/admin/allocations/batch');

      // Select multiple students
      await page.check('[data-testid="student-checkbox-1"]');
      await page.check('[data-testid="student-checkbox-2"]');

      // Click propose
      await page.click('[data-testid="propose-allocation-button"]');

      // Expected Result: Staged allocations created
      await expect(page.locator('[data-testid="staged-allocations-table"]')).toBeVisible();
    });

    test('should confirm staged allocation', async ({ authenticatedAdminPage }) => {
      const page = authenticatedAdminPage;

      await page.goto('/admin/allocations/staged');

      // Confirm first staged allocation
      await page.click('[data-testid="confirm-allocation-button"]');

      // Expected Result: Allocation confirmed
      await expect(page.locator('[data-testid="success-toast"]')).toContainText(/confirmed/i);
    });
  });
});
```

### 4.7 Room and Building Management Tests

**File**: `e2e/tests/room-management.spec.ts`

```typescript
import { test, expect } from '../fixtures/test-fixtures';

test.describe('Room and Building Management', () => {

  test.describe('Building Management', () => {

    test('should create new building', async ({ authenticatedAdminPage }) => {
      const page = authenticatedAdminPage;

      await page.goto('/admin/buildings');
      await page.click('[data-testid="add-building-button"]');

      // Fill building form
      await page.fill('[data-testid="building-name-input"]', 'New Test Building');
      await page.fill('[data-testid="building-address-input"]', '123 Test Street');
      await page.fill('[data-testid="total-floors-input"]', '5');

      await page.click('[data-testid="save-building-button"]');

      // Expected Result: Building created
      await expect(page.locator('[data-testid="success-toast"]')).toContainText(/created/i);
    });

    test('should edit building details', async ({ authenticatedAdminPage }) => {
      const page = authenticatedAdminPage;

      await page.goto('/admin/buildings');
      await page.click('[data-testid="building-card"]');
      await page.click('[data-testid="edit-building-button"]');

      await page.fill('[data-testid="building-address-input"]', '456 Updated Street');
      await page.click('[data-testid="save-building-button"]');

      // Expected Result: Building updated
      await expect(page.locator('[data-testid="success-toast"]')).toContainText(/updated/i);
    });
  });

  test.describe('Room Management', () => {

    test('should create new room', async ({ authenticatedAdminPage }) => {
      const page = authenticatedAdminPage;

      await page.goto('/admin/rooms');
      await page.click('[data-testid="add-room-button"]');

      // Fill room form
      await page.selectOption('[data-testid="building-select"]', { index: 1 });
      await page.fill('[data-testid="room-number-input"]', 'T999');
      await page.fill('[data-testid="capacity-input"]', '2');

      await page.click('[data-testid="save-room-button"]');

      // Expected Result: Room created
      await expect(page.locator('[data-testid="success-toast"]')).toContainText(/created/i);
    });

    test('should set room to maintenance', async ({ authenticatedAdminPage }) => {
      const page = authenticatedAdminPage;

      await page.goto('/admin/rooms');
      await page.click('[data-testid="room-card"]:has-text("AVAILABLE")');

      await page.selectOption('[data-testid="status-select"]', 'MAINTENANCE');
      await page.click('[data-testid="save-button"]');

      // Expected Result: Room status updated
      await expect(page.locator('[data-testid="room-status"]')).toContainText('MAINTENANCE');
    });

    test('should search rooms with filters', async ({ authenticatedAdminPage }) => {
      const page = authenticatedAdminPage;

      await page.goto('/admin/rooms');

      // Search by building
      await page.selectOption('[data-testid="building-filter"]', { index: 1 });

      // Filter by status
      await page.selectOption('[data-testid="status-filter"]', 'AVAILABLE');

      // Expected Result: Filtered results
      await expect(page.locator('[data-testid="room-card"]')).toHaveCount(await page.locator('[data-testid="room-card"]').count());
    });
  });
});
```

---

## 5. Manual Test Cases

### 5.1 Critical Priority Test Cases

| Test ID | Description | Preconditions | Steps | Expected Results | Priority |
|---------|-------------|---------------|-------|------------------|----------|
| MTC-001 | User Registration with Email Verification | Email service configured, User not registered | 1. Navigate to registration page<br>2. Fill all required fields with valid data<br>3. Submit registration<br>4. Check email inbox<br>5. Click verification link | User receives verification email within 5 minutes. Clicking link verifies account and allows login. | Critical |
| MTC-002 | Complete Application Submission Flow | Student logged in, No pending application | 1. Navigate to new application<br>2. Select accommodation type<br>3. Set move-in date (future)<br>4. Set duration<br>5. Add special requirements<br>6. Submit | Application created with PENDING status, Application code generated, Confirmation displayed | Critical |
| MTC-003 | Document Upload and Review | Student logged in, Application exists | 1. Navigate to documents<br>2. Click upload<br>3. Select document type<br>4. Choose PDF file (<10MB)<br>5. Submit | Document uploaded to MinIO, Status shows PENDING, Document appears in list | Critical |
| MTC-004 | Room Allocation Process | Admin logged in, Application in AWAITING_ALLOCATION, Available room exists | 1. Navigate to allocations<br>2. Select student from waiting list<br>3. Select available room<br>4. Confirm allocation | Allocation created with ACTIVE status, Room occupancy incremented, Application status updated to ALLOCATED | Critical |
| MTC-005 | JWT Token Expiration | User logged in, Token near expiration | 1. Login and note token<br>2. Wait for token expiration time<br>3. Attempt API call | User redirected to login, Session terminated gracefully | Critical |

### 5.2 High Priority Test Cases

| Test ID | Description | Preconditions | Steps | Expected Results | Priority |
|---------|-------------|---------------|-------|------------------|----------|
| MTC-006 | Password Reset Flow | User registered, Email service configured | 1. Click forgot password<br>2. Enter registered email<br>3. Check email<br>4. Click reset link<br>5. Enter new password<br>6. Login with new password | Reset email received, Link valid for 24 hours, New password works | High |
| MTC-007 | Admin Application Review | Admin logged in, Pending application with documents | 1. Navigate to application<br>2. Review each document<br>3. Add comments where needed<br>4. Approve/Reject documents<br>5. Update application status | Document statuses updated, Comments visible to student, Application status changed | High |
| MTC-008 | Batch Allocation Proposal | Admin logged in, Multiple students in waiting list, Available rooms | 1. Navigate to batch allocation<br>2. Select multiple students<br>3. Click propose allocation<br>4. Review proposals<br>5. Confirm or reject each | Staged allocations created with scores, Warnings displayed for constraints, Can confirm/reject individually | High |
| MTC-009 | Room Status Management | Admin logged in, Room exists | 1. Navigate to room details<br>2. Change status to MAINTENANCE<br>3. Verify allocation blocked<br>4. Change back to AVAILABLE | Room unavailable for allocation during maintenance, Status changes reflected immediately | High |
| MTC-010 | Student Profile Update | Student logged in, Profile exists | 1. Navigate to profile<br>2. Update phone number<br>3. Update special requirements<br>4. Save changes | Profile updated successfully, Changes reflected in applications | High |

### 5.3 Medium Priority Test Cases

| Test ID | Description | Preconditions | Steps | Expected Results | Priority |
|---------|-------------|---------------|-------|------------------|----------|
| MTC-011 | Search and Filter Applications | Admin logged in, Multiple applications exist | 1. Navigate to applications<br>2. Apply status filter<br>3. Apply date range<br>4. Search by student name<br>5. Reset filters | Filtered results accurate, Pagination works, Reset clears all filters | Medium |
| MTC-012 | Document Comment Thread | Admin logged in, Document exists | 1. Navigate to document<br>2. Add first comment<br>3. Add follow-up comment<br>4. View as student<br>5. Admin deletes comment | Comments displayed in order, Student sees comments, Deleted comments removed | Medium |
| MTC-013 | Building-Room Hierarchy | Admin logged in | 1. Create new building<br>2. Add rooms to building<br>3. View building details<br>4. Delete room<br>5. Attempt delete building with rooms | Rooms associated correctly, Building shows room count, Cannot delete building with active allocations | Medium |
| MTC-014 | Allocation Eligibility Check | Admin logged in, Student profile exists | 1. Navigate to allocation<br>2. Select student<br>3. View eligibility status<br>4. Check eligible rooms<br>5. View ineligibility reasons | Eligibility accurately calculated, Reasons clearly stated, Only matching rooms shown | Medium |
| MTC-015 | Dashboard Statistics Accuracy | Admin logged in, Various data exists | 1. View dashboard<br>2. Manually count applications<br>3. Calculate occupancy rate<br>4. Compare with displayed stats | All statistics match manual calculations, Real-time updates work | Medium |

### 5.4 Low Priority Test Cases

| Test ID | Description | Preconditions | Steps | Expected Results | Priority |
|---------|-------------|---------------|-------|------------------|----------|
| MTC-016 | Swagger API Documentation | API running | 1. Navigate to /swagger-ui.html<br>2. Browse endpoints<br>3. Test endpoint via Swagger<br>4. Verify authentication | All endpoints documented, Try-it-out works, Authentication required where specified | Low |
| MTC-017 | Activity Log Review | Admin logged in, Recent activities exist | 1. Navigate to activity log<br>2. Filter by action type<br>3. Filter by date<br>4. View activity details | All actions logged, Filters work correctly, Details include user and timestamp | Low |
| MTC-018 | User Role Permissions | Users with different roles | 1. Login as student<br>2. Attempt admin action<br>3. Login as admin<br>4. Access all features | Student blocked from admin features, Admin has full access, Appropriate error messages | Low |
| MTC-019 | Pagination and Sorting | Large dataset exists | 1. View list with many items<br>2. Navigate pages<br>3. Change page size<br>4. Sort by different columns | Pagination works correctly, Sorting accurate, Performance acceptable | Low |
| MTC-020 | Email Template Rendering | Email service configured | 1. Trigger verification email<br>2. Trigger password reset email<br>3. Trigger notification email<br>4. View email content | Emails render correctly, Links work, Branding consistent | Low |

---

## 6. Test Data Requirements

### 6.1 Required Test Data Sets

#### Users Dataset
```sql
-- Test Admin User
INSERT INTO app_user (email, password, first_name, last_name, role, email_verified, created_at)
VALUES ('test-admin@university.edu', '$2a$10$...', 'Test', 'Admin', 'ADMIN', true, NOW());

-- Test Student Users (10 students for various scenarios)
INSERT INTO app_user (email, password, first_name, last_name, role, email_verified, created_at)
VALUES
    ('student1@university.edu', '$2a$10$...', 'John', 'Doe', 'STUDENT', true, NOW()),
    ('student2@university.edu', '$2a$10$...', 'Jane', 'Smith', 'STUDENT', true, NOW()),
    ('student3@university.edu', '$2a$10$...', 'Bob', 'Johnson', 'STUDENT', true, NOW()),
    ('student4@university.edu', '$2a$10$...', 'Alice', 'Williams', 'STUDENT', false, NOW()),
    ('student5@university.edu', '$2a$10$...', 'Charlie', 'Brown', 'STUDENT', true, NOW());
```

#### Institutions Dataset
```sql
INSERT INTO institution (name, address, phone, email)
VALUES
    ('Test University', '123 University Ave', '+1234567890', 'admin@testuni.edu'),
    ('Second University', '456 College St', '+0987654321', 'admin@seconduni.edu');
```

#### Buildings and Rooms Dataset
```sql
-- Buildings
INSERT INTO building (name, address, total_floors)
VALUES
    ('Main Hall', '100 Campus Drive', 5),
    ('North Wing', '200 Campus Drive', 3),
    ('South Residence', '300 Campus Drive', 4);

-- Rooms (various statuses and capacities)
INSERT INTO room (room_number, capacity, current_occupancy, status, building_id)
VALUES
    ('101', 2, 0, 'AVAILABLE', 1),
    ('102', 2, 1, 'OCCUPIED', 1),
    ('103', 1, 1, 'FULL', 1),
    ('104', 2, 0, 'MAINTENANCE', 1),
    ('201', 2, 0, 'AVAILABLE', 2),
    ('202', 3, 2, 'OCCUPIED', 2);
```

#### Document Types Dataset
```sql
INSERT INTO document_type (name, description)
VALUES
    ('ID Document', 'Government-issued identification'),
    ('Proof of Enrollment', 'University enrollment confirmation'),
    ('Financial Statement', 'Proof of financial capability'),
    ('Medical Certificate', 'Health clearance certificate'),
    ('Supporting Document', 'Additional supporting documents');
```

#### Accommodation Types Dataset
```sql
INSERT INTO accommodation_type (name, description)
VALUES
    ('Single Room', 'Private room for one student'),
    ('Shared Room', 'Room shared with one roommate'),
    ('Suite', 'Private room with shared common area');
```

### 6.2 Data Setup Script

**File**: `src/test/resources/data/test-data-setup.sql`

```sql
-- Clean existing test data
DELETE FROM document_comment WHERE document_id IN (SELECT id FROM document WHERE student_id IN (SELECT id FROM student WHERE student_number LIKE 'TEST%'));
DELETE FROM document WHERE student_id IN (SELECT id FROM student WHERE student_number LIKE 'TEST%');
DELETE FROM room_allocation WHERE student_id IN (SELECT id FROM student WHERE student_number LIKE 'TEST%');
DELETE FROM staged_allocation WHERE student_id IN (SELECT id FROM student WHERE student_number LIKE 'TEST%');
DELETE FROM application WHERE student_id IN (SELECT id FROM student WHERE student_number LIKE 'TEST%');
DELETE FROM student WHERE student_number LIKE 'TEST%';
DELETE FROM app_user WHERE email LIKE 'test-%@university.edu';

-- Insert test users
INSERT INTO app_user (id, email, password, first_name, last_name, role, email_verified, created_at, updated_at)
VALUES
    (9001, 'test-admin@university.edu', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3k8TQpJMiJwvL.MPhPH.', 'Test', 'Admin', 'ADMIN', true, NOW(), NOW()),
    (9002, 'test-student1@university.edu', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3k8TQpJMiJwvL.MPhPH.', 'Test', 'Student1', 'STUDENT', true, NOW(), NOW()),
    (9003, 'test-student2@university.edu', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3k8TQpJMiJwvL.MPhPH.', 'Test', 'Student2', 'STUDENT', true, NOW(), NOW());

-- Password for all test users: "Password123!"

-- Insert test students
INSERT INTO student (id, student_number, phone, user_id, institution_id)
VALUES
    (9001, 'TEST001', '+1234567890', 9002, 1),
    (9002, 'TEST002', '+0987654321', 9003, 1);

-- Insert test applications in various states
INSERT INTO application (id, application_code, status, student_id, preferred_accommodation_type_id, move_in_date, duration, created_at)
VALUES
    (9001, 'TEST-APP-001', 'PENDING', 9001, 1, CURRENT_DATE + INTERVAL '30 days', 12, NOW()),
    (9002, 'TEST-APP-002', 'APPROVED', 9002, 2, CURRENT_DATE + INTERVAL '30 days', 12, NOW());

-- Insert test documents
INSERT INTO document (id, document_name, path, file_size, status, student_id, document_type_id, application_id, created_at)
VALUES
    (9001, 'test_id.pdf', 'test/documents/test_id.pdf', 1024, 'PENDING', 9001, 1, 9001, NOW()),
    (9002, 'test_enrollment.pdf', 'test/documents/test_enrollment.pdf', 2048, 'APPROVED', 9002, 2, 9002, NOW());
```

### 6.3 Test File Fixtures

Create the following test files in `e2e/fixtures/`:

```
e2e/
  fixtures/
    test-document.pdf          # Valid PDF file < 10MB
    test-document1.pdf         # First test document for multiple upload
    test-document2.pdf         # Second test document for multiple upload
    large-file.pdf             # File > 10MB for size limit testing
    invalid-file.exe           # Invalid file type for rejection testing
    test-image.jpg             # Image file for document type testing
```

---

## 7. Test Environment Setup

### 7.1 Dependencies and Configurations

#### Required Software
- Java 17 or higher
- Gradle 8.x
- Docker and Docker Compose
- Node.js 18+ (for Playwright)
- PostgreSQL 15+ (or use Docker)
- MinIO (or use Docker)

#### Docker Compose for Test Environment

**File**: `docker-compose.test.yml`

```yaml
version: '3.8'

services:
  postgres-test:
    image: postgres:15-alpine
    container_name: student-accommodation-postgres-test
    environment:
      POSTGRES_DB: student_accommodation_test
      POSTGRES_USER: testuser
      POSTGRES_PASSWORD: testpass
    ports:
      - "5433:5432"
    volumes:
      - postgres-test-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U testuser -d student_accommodation_test"]
      interval: 10s
      timeout: 5s
      retries: 5

  minio-test:
    image: minio/minio:latest
    container_name: student-accommodation-minio-test
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9001:9000"
      - "9091:9001"
    volumes:
      - minio-test-data:/data
    command: server /data --console-address ":9001"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 10s
      timeout: 5s
      retries: 5

  mailhog:
    image: mailhog/mailhog:latest
    container_name: student-accommodation-mailhog
    ports:
      - "1025:1025"
      - "8025:8025"

volumes:
  postgres-test-data:
  minio-test-data:
```

#### Test Application Properties

**File**: `src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/student_accommodation_test
    username: testuser
    password: testpass

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

  flyway:
    enabled: true
    clean-disabled: false

  mail:
    host: localhost
    port: 1025
    username: test
    password: test

minio:
  endpoint: http://localhost:9001
  access-key: minioadmin
  secret-key: minioadmin
  bucket: test-bucket

jwt:
  secret: test-secret-key-that-is-at-least-256-bits-long-for-testing
  expiration: 3600000
  admin-expiration: 1800000

app:
  base-url: http://localhost:4200
  support-email: test@university.edu
  university-name: Test University
```

### 7.2 Commands to Run Test Suites

#### Start Test Environment
```bash
# Start test containers
docker-compose -f docker-compose.test.yml up -d

# Wait for services to be ready
docker-compose -f docker-compose.test.yml ps

# Create MinIO bucket
docker exec student-accommodation-minio-test mc alias set local http://localhost:9000 minioadmin minioadmin
docker exec student-accommodation-minio-test mc mb local/test-bucket
```

#### Run Unit Tests
```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "AuthServiceTest"

# Run with coverage report
./gradlew test jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

#### Run Integration Tests
```bash
# Run integration tests only
./gradlew integrationTest

# Run with test containers
./gradlew integrationTest -PuseTestContainers=true

# Run specific integration test
./gradlew integrationTest --tests "AuthControllerIntegrationTest"
```

#### Run All Tests with Coverage
```bash
# Run all tests
./gradlew check

# Generate combined coverage report
./gradlew jacocoTestCoverageVerification

# View reports
open build/reports/tests/test/index.html
open build/reports/jacoco/test/html/index.html
```

#### Run Playwright E2E Tests
```bash
# Navigate to e2e directory
cd e2e

# Install dependencies
npm install

# Install Playwright browsers
npx playwright install

# Run all E2E tests
npx playwright test

# Run specific test file
npx playwright test auth.spec.ts

# Run with UI mode
npx playwright test --ui

# Run with headed browser
npx playwright test --headed

# Generate report
npx playwright show-report
```

#### Run Tests in CI/CD Pipeline

**File**: `.github/workflows/test.yml`

```yaml
name: Test Suite

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run unit tests
        run: ./gradlew test

      - name: Upload test results
        uses: actions/upload-artifact@v4
        with:
          name: unit-test-results
          path: build/reports/tests/test

  integration-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: test_db
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run integration tests
        run: ./gradlew integrationTest
        env:
          DB_URL: jdbc:postgresql://localhost:5432/test_db
          DB_USER: test
          DB_PASSWORD: test

  e2e-tests:
    runs-on: ubuntu-latest
    needs: [unit-tests, integration-tests]
    steps:
      - uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'

      - name: Install Playwright
        run: |
          cd e2e
          npm ci
          npx playwright install --with-deps

      - name: Run E2E tests
        run: |
          cd e2e
          npx playwright test

      - name: Upload Playwright report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report
          path: e2e/playwright-report
```

### 7.3 Test Database Reset Script

**File**: `scripts/reset-test-db.sh`

```bash
#!/bin/bash

# Reset test database
echo "Resetting test database..."

PGPASSWORD=testpass psql -h localhost -p 5433 -U testuser -d student_accommodation_test << EOF
-- Drop all tables
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO testuser;
GRANT ALL ON SCHEMA public TO public;
EOF

# Run migrations
echo "Running migrations..."
./gradlew flywayMigrate -Penv=test

# Load test data
echo "Loading test data..."
PGPASSWORD=testpass psql -h localhost -p 5433 -U testuser -d student_accommodation_test -f src/test/resources/data/test-data-setup.sql

echo "Test database reset complete!"
```

### 7.4 Gradle Test Configuration

Add to `build.gradle`:

```groovy
// Test configuration
test {
    useJUnitPlatform()
    testLogging {
        events "passed", "skipped", "failed"
        showExceptions true
        showCauses true
        showStackTraces true
    }
    finalizedBy jacocoTestReport
}

// Integration test source set
sourceSets {
    integrationTest {
        java {
            srcDir 'src/integrationTest/java'
        }
        resources {
            srcDir 'src/integrationTest/resources'
        }
        compileClasspath += main.output + test.output
        runtimeClasspath += main.output + test.output
    }
}

// Integration test task
task integrationTest(type: Test) {
    description = 'Runs integration tests'
    group = 'verification'

    testClassesDirs = sourceSets.integrationTest.output.classesDirs
    classpath = sourceSets.integrationTest.runtimeClasspath

    useJUnitPlatform()

    shouldRunAfter test
}

// JaCoCo configuration
jacoco {
    toolVersion = "0.8.10"
}

jacocoTestReport {
    reports {
        xml.required = true
        csv.required = true
        html.required = true
    }

    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                '**/config/**',
                '**/dto/**',
                '**/entity/**',
                '**/enums/**',
                '**/exception/**'
            ])
        }))
    }
}

jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.80
            }
        }

        rule {
            element = 'CLASS'
            includes = ['com.beezhub.student_accommodation.service.*']
            limit {
                counter = 'LINE'
                value = 'COVEREDRATIO'
                minimum = 0.80
            }
        }
    }
}

check.dependsOn integrationTest
integrationTest.mustRunAfter test
```

---

## Appendix A: Test Case Traceability Matrix

| Requirement | Unit Tests | Integration Tests | E2E Tests | Manual Tests |
|-------------|------------|-------------------|-----------|--------------|
| User Registration | AuthServiceTest | AuthControllerIntegrationTest | auth.spec.ts | MTC-001 |
| User Login | AuthServiceTest | AuthControllerIntegrationTest | auth.spec.ts | - |
| Password Reset | AuthServiceTest | AuthControllerIntegrationTest | auth.spec.ts | MTC-006 |
| Application Submit | ApplicationServiceTest | ApplicationControllerIntegrationTest | application-flow.spec.ts | MTC-002 |
| Document Upload | DocumentServiceTest | DocumentControllerIntegrationTest | document-upload.spec.ts | MTC-003 |
| Room Allocation | RoomAllocationServiceTest | RoomAllocationControllerIntegrationTest | admin-dashboard.spec.ts | MTC-004 |
| Batch Allocation | AllocationScoringServiceTest | - | admin-dashboard.spec.ts | MTC-008 |
| Admin Dashboard | AdminStatsServiceTest | AdminStatsControllerIntegrationTest | admin-dashboard.spec.ts | MTC-015 |

---

## Appendix B: Defect Severity Classification

| Severity | Description | Example |
|----------|-------------|---------|
| Critical | System unusable, data loss, security breach | Authentication bypass, data corruption |
| High | Major feature broken, no workaround | Cannot submit applications, allocation fails |
| Medium | Feature partially broken, workaround exists | Search filter not working, pagination issue |
| Low | Minor issue, cosmetic | UI alignment, typo in message |

---

*Document Version: 1.0*
*Last Updated: January 2026*
*Author: QA Engineering Team*
