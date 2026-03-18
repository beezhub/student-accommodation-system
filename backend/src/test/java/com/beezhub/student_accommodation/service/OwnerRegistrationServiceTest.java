package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.DuplicateResourceException;
import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.exceptions.InvalidTokenException;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.exceptions.TokenExpiredException;
import com.beezhub.student_accommodation.mapper.OwnerRegistrationMapper;
import com.beezhub.student_accommodation.model.dto.OwnerRegistrationRequest;
import com.beezhub.student_accommodation.model.dto.OwnerRegistrationResponse;
import com.beezhub.student_accommodation.model.dto.SetOwnerPasswordRequest;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.BuildingOwner;
import com.beezhub.student_accommodation.model.entity.OwnerRegistration;
import com.beezhub.student_accommodation.model.enums.OwnerRegistrationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.BuildingOwnerRepository;
import com.beezhub.student_accommodation.repository.OwnerRegistrationRepository;
import com.beezhub.student_accommodation.service.notification.EmailDispatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OwnerRegistrationService tests")
class OwnerRegistrationServiceTest {

    @Mock private OwnerRegistrationRepository registrationRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private BuildingOwnerRepository buildingOwnerRepository;
    @Mock private OwnerRegistrationMapper mapper;
    @Mock private EmailDispatchService emailDispatchService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OwnerRegistrationService service;

    private OwnerRegistrationRequest validRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:4200");

        validRequest = new OwnerRegistrationRequest();
        validRequest.setFullName("Jane Smith");
        validRequest.setEmail("jane@acme.com");
        validRequest.setOrganisationName("Acme Properties");
        validRequest.setContactPhone("+27 11 000 0000");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private OwnerRegistration pendingRegistration(String email) {
        return OwnerRegistration.builder()
                .id(1L)
                .fullName("Jane Smith")
                .email(email)
                .organisationName("Acme Properties")
                .status(OwnerRegistrationStatus.PENDING)
                .emailVerificationToken("token-abc")
                .emailVerificationExpiresAt(LocalDateTime.now().plusHours(1))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private OwnerRegistration pendingReviewRegistration() {
        return OwnerRegistration.builder()
                .id(2L)
                .fullName("Jane Smith")
                .email("jane@acme.com")
                .organisationName("Acme Properties")
                .status(OwnerRegistrationStatus.PENDING_REVIEW)
                .verifiedAt(LocalDateTime.now().minusMinutes(30))
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    // ── submit() ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submit()")
    class Submit {

        @Test
        @DisplayName("creates registration and sends verification email when no conflict exists")
        void submit_noConflict_savesAndSendsEmail() {
            when(appUserRepository.findByEmail("jane@acme.com")).thenReturn(Optional.empty());
            when(registrationRepository.existsByEmailAndStatusIn(anyString(), anyList())).thenReturn(false);
            OwnerRegistration saved = pendingRegistration("jane@acme.com");
            when(registrationRepository.save(any())).thenReturn(saved);
            when(mapper.toResponse(saved)).thenReturn(OwnerRegistrationResponse.builder().build());

            OwnerRegistrationResponse result = service.submit(validRequest);

            assertThat(result).isNotNull();
            verify(registrationRepository).save(any(OwnerRegistration.class));
            verify(emailDispatchService).sendOwnerRegistrationConfirmEmail(
                    eq("jane@acme.com"), eq("Jane Smith"), eq("Acme Properties"), anyString());
        }

        @Test
        @DisplayName("throws DuplicateResourceException when app_user already exists")
        void submit_userAlreadyExists_throwsDuplicate() {
            when(appUserRepository.findByEmail("jane@acme.com")).thenReturn(Optional.of(new AppUser()));

            assertThatThrownBy(() -> service.submit(validRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("already exists");

            verify(registrationRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws DuplicateResourceException when pending registration already exists")
        void submit_pendingRegistrationExists_throwsDuplicate() {
            when(appUserRepository.findByEmail("jane@acme.com")).thenReturn(Optional.empty());
            when(registrationRepository.existsByEmailAndStatusIn(anyString(), anyList())).thenReturn(true);

            assertThatThrownBy(() -> service.submit(validRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("pending registration");

            verify(registrationRepository, never()).save(any());
        }

        @Test
        @DisplayName("verification URL contains the saved token")
        void submit_verificationUrlContainsToken() {
            when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(registrationRepository.existsByEmailAndStatusIn(anyString(), anyList())).thenReturn(false);

            ArgumentCaptor<OwnerRegistration> captor = ArgumentCaptor.forClass(OwnerRegistration.class);
            OwnerRegistration saved = pendingRegistration("jane@acme.com");
            when(registrationRepository.save(captor.capture())).thenReturn(saved);
            when(mapper.toResponse(any())).thenReturn(OwnerRegistrationResponse.builder().build());

            service.submit(validRequest);

            OwnerRegistration persisted = captor.getValue();
            assertThat(persisted.getEmailVerificationToken()).isNotBlank();
            assertThat(persisted.getEmailVerificationExpiresAt()).isAfter(LocalDateTime.now());
        }
    }

    // ── verifyEmail() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verifyEmail()")
    class VerifyEmail {

        @Test
        @DisplayName("advances status to PENDING_REVIEW and clears token")
        void verifyEmail_validToken_advancesStatus() {
            OwnerRegistration reg = pendingRegistration("jane@acme.com");
            when(registrationRepository.findByEmailVerificationToken("token-abc")).thenReturn(Optional.of(reg));

            service.verifyEmail("token-abc");

            assertThat(reg.getStatus()).isEqualTo(OwnerRegistrationStatus.PENDING_REVIEW);
            assertThat(reg.getEmailVerificationToken()).isNull();
            assertThat(reg.getVerifiedAt()).isNotNull();
            verify(registrationRepository).save(reg);
        }

        @Test
        @DisplayName("throws InvalidTokenException for unknown token")
        void verifyEmail_unknownToken_throwsInvalidToken() {
            when(registrationRepository.findByEmailVerificationToken("bad-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.verifyEmail("bad-token"))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("throws InvalidStatusException when registration is not PENDING")
        void verifyEmail_alreadyVerified_throwsInvalidStatus() {
            OwnerRegistration reg = pendingReviewRegistration();
            when(registrationRepository.findByEmailVerificationToken("some-token")).thenReturn(Optional.of(reg));

            assertThatThrownBy(() -> service.verifyEmail("some-token"))
                    .isInstanceOf(InvalidStatusException.class);
        }

        @Test
        @DisplayName("throws TokenExpiredException when token is expired")
        void verifyEmail_expiredToken_throwsTokenExpired() {
            OwnerRegistration reg = pendingRegistration("jane@acme.com");
            reg.setEmailVerificationExpiresAt(LocalDateTime.now().minusMinutes(1));
            when(registrationRepository.findByEmailVerificationToken("old-token")).thenReturn(Optional.of(reg));

            assertThatThrownBy(() -> service.verifyEmail("old-token"))
                    .isInstanceOf(TokenExpiredException.class);
        }
    }

    // ── resendVerification() ─────────────────────────────────────────────────

    @Nested
    @DisplayName("resendVerification()")
    class ResendVerification {

        @Test
        @DisplayName("updates token and resends email for PENDING registration")
        void resendVerification_pendingExists_sendsNewEmail() {
            OwnerRegistration reg = pendingRegistration("jane@acme.com");
            when(registrationRepository.findByEmailAndStatusIn(eq("jane@acme.com"), anyList()))
                    .thenReturn(List.of(reg));
            when(registrationRepository.save(any())).thenReturn(reg);

            service.resendVerification("jane@acme.com");

            assertThat(reg.getEmailVerificationToken()).isNotBlank();
            verify(emailDispatchService).sendOwnerRegistrationConfirmEmail(
                    eq("jane@acme.com"), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when no PENDING registration exists")
        void resendVerification_noPending_throwsNotFound() {
            when(registrationRepository.findByEmailAndStatusIn(anyString(), anyList()))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.resendVerification("nobody@acme.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── approve() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("approve()")
    class Approve {

        @Test
        @DisplayName("creates AppUser and BuildingOwner then sets APPROVED status")
        void approve_pendingReview_createsUserAndProfile() {
            OwnerRegistration reg = pendingReviewRegistration();
            when(registrationRepository.findById(2L)).thenReturn(Optional.of(reg));
            AppUser savedUser = AppUser.builder().id(10L).email("jane@acme.com")
                    .userRole(UserRole.BUILDING_OWNER).build();
            when(appUserRepository.save(any())).thenReturn(savedUser);
            when(buildingOwnerRepository.save(any())).thenReturn(new BuildingOwner());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");

            service.approve(2L);

            verify(appUserRepository).save(any(AppUser.class));
            verify(buildingOwnerRepository).save(any(BuildingOwner.class));
            assertThat(reg.getStatus()).isEqualTo(OwnerRegistrationStatus.APPROVED);
            assertThat(reg.getPasswordSetupToken()).isNotBlank();
            assertThat(reg.getPasswordSetupExpiresAt()).isAfter(LocalDateTime.now());
            verify(emailDispatchService).sendOwnerApprovedEmail(
                    eq("jane@acme.com"), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("throws InvalidStatusException when registration is not PENDING_REVIEW")
        void approve_notPendingReview_throwsInvalidStatus() {
            OwnerRegistration reg = pendingRegistration("jane@acme.com");
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));

            assertThatThrownBy(() -> service.approve(1L))
                    .isInstanceOf(InvalidStatusException.class);
        }

        @Test
        @DisplayName("throws InvalidStatusException when already APPROVED")
        void approve_alreadyApproved_throwsInvalidStatus() {
            OwnerRegistration reg = pendingReviewRegistration();
            reg.setStatus(OwnerRegistrationStatus.APPROVED);
            when(registrationRepository.findById(2L)).thenReturn(Optional.of(reg));

            assertThatThrownBy(() -> service.approve(2L))
                    .isInstanceOf(InvalidStatusException.class);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when ID does not exist")
        void approve_notFound_throwsResourceNotFound() {
            when(registrationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.approve(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── reject() ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reject()")
    class Reject {

        @Test
        @DisplayName("sets REJECTED status and sends rejection email")
        void reject_pendingReview_setsRejectedAndSendsEmail() {
            OwnerRegistration reg = pendingReviewRegistration();
            when(registrationRepository.findById(2L)).thenReturn(Optional.of(reg));

            service.reject(2L, "Insufficient documentation");

            assertThat(reg.getStatus()).isEqualTo(OwnerRegistrationStatus.REJECTED);
            verify(registrationRepository).save(reg);
            verify(emailDispatchService).sendOwnerRejectedEmail(
                    eq("jane@acme.com"), anyString(), eq("Insufficient documentation"));
        }

        @Test
        @DisplayName("reject with null reason sends email without reason")
        void reject_nullReason_sendsEmailWithNullReason() {
            OwnerRegistration reg = pendingReviewRegistration();
            when(registrationRepository.findById(2L)).thenReturn(Optional.of(reg));

            service.reject(2L, null);

            verify(emailDispatchService).sendOwnerRejectedEmail(
                    eq("jane@acme.com"), anyString(), eq(null));
        }

        @Test
        @DisplayName("throws InvalidStatusException when already APPROVED or REJECTED")
        void reject_alreadyProcessed_throwsInvalidStatus() {
            OwnerRegistration reg = pendingReviewRegistration();
            reg.setStatus(OwnerRegistrationStatus.APPROVED);
            when(registrationRepository.findById(2L)).thenReturn(Optional.of(reg));

            assertThatThrownBy(() -> service.reject(2L, null))
                    .isInstanceOf(InvalidStatusException.class);
        }
    }

    // ── reapply() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reapply()")
    class Reapply {

        @Test
        @DisplayName("delegates to submit when only REJECTED record exists")
        void reapply_onlyRejected_createsNewRegistration() {
            when(registrationRepository.existsByEmailAndStatusIn(
                    eq("jane@acme.com"),
                    eq(List.of(OwnerRegistrationStatus.PENDING, OwnerRegistrationStatus.PENDING_REVIEW, OwnerRegistrationStatus.APPROVED))))
                    .thenReturn(false);
            when(appUserRepository.findByEmail("jane@acme.com")).thenReturn(Optional.empty());
            when(registrationRepository.existsByEmailAndStatusIn(
                    eq("jane@acme.com"),
                    eq(List.of(OwnerRegistrationStatus.PENDING, OwnerRegistrationStatus.PENDING_REVIEW))))
                    .thenReturn(false);
            OwnerRegistration saved = pendingRegistration("jane@acme.com");
            when(registrationRepository.save(any())).thenReturn(saved);
            when(mapper.toResponse(any())).thenReturn(OwnerRegistrationResponse.builder().build());

            OwnerRegistrationResponse result = service.reapply(validRequest);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("throws DuplicateResourceException when active registration exists")
        void reapply_activeExists_throwsDuplicate() {
            when(registrationRepository.existsByEmailAndStatusIn(
                    eq("jane@acme.com"),
                    eq(List.of(OwnerRegistrationStatus.PENDING, OwnerRegistrationStatus.PENDING_REVIEW, OwnerRegistrationStatus.APPROVED))))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.reapply(validRequest))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    // ── setPassword() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setPassword()")
    class SetPassword {

        private SetOwnerPasswordRequest passwordRequest(String token) {
            SetOwnerPasswordRequest req = new SetOwnerPasswordRequest();
            req.setToken(token);
            req.setPassword("newSecure123");
            return req;
        }

        @Test
        @DisplayName("sets password, clears token, and deactivates mustChangePassword")
        void setPassword_validToken_updatesUser() {
            OwnerRegistration reg = pendingReviewRegistration();
            reg.setStatus(OwnerRegistrationStatus.APPROVED);
            reg.setPasswordSetupToken("setup-token");
            reg.setPasswordSetupExpiresAt(LocalDateTime.now().plusHours(24));
            when(registrationRepository.findByPasswordSetupToken("setup-token")).thenReturn(Optional.of(reg));

            AppUser user = AppUser.builder().id(10L).email("jane@acme.com")
                    .userRole(UserRole.BUILDING_OWNER).mustChangePassword(true).build();
            when(appUserRepository.findByEmail("jane@acme.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newSecure123")).thenReturn("hashed");

            service.setPassword(passwordRequest("setup-token"));

            assertThat(user.getUserPassword()).isEqualTo("hashed");
            assertThat(user.isMustChangePassword()).isFalse();
            assertThat(reg.getPasswordSetupToken()).isNull();
            verify(appUserRepository).save(user);
            verify(registrationRepository).save(reg);
        }

        @Test
        @DisplayName("throws InvalidTokenException for unknown token")
        void setPassword_unknownToken_throwsInvalidToken() {
            when(registrationRepository.findByPasswordSetupToken("bad")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.setPassword(passwordRequest("bad")))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("throws TokenExpiredException when setup token is expired")
        void setPassword_expiredToken_throwsTokenExpired() {
            OwnerRegistration reg = pendingReviewRegistration();
            reg.setPasswordSetupToken("old-token");
            reg.setPasswordSetupExpiresAt(LocalDateTime.now().minusMinutes(1));
            when(registrationRepository.findByPasswordSetupToken("old-token")).thenReturn(Optional.of(reg));

            assertThatThrownBy(() -> service.setPassword(passwordRequest("old-token")))
                    .isInstanceOf(TokenExpiredException.class);
        }
    }
}
