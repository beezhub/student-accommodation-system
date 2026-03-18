package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.DuplicateResourceException;
import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.exceptions.InvalidTokenException;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.exceptions.TokenExpiredException;
import com.beezhub.student_accommodation.model.dto.OwnerRegistrationRequest;
import com.beezhub.student_accommodation.model.dto.OwnerRegistrationSummaryDto;
import com.beezhub.student_accommodation.model.dto.OwnerRegistrationResponse;
import com.beezhub.student_accommodation.model.dto.SetOwnerPasswordRequest;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.BuildingOwner;
import com.beezhub.student_accommodation.model.entity.OwnerRegistration;
import com.beezhub.student_accommodation.model.enums.OwnerRegistrationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.mapper.OwnerRegistrationMapper;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.BuildingOwnerRepository;
import com.beezhub.student_accommodation.repository.OwnerRegistrationRepository;
import com.beezhub.student_accommodation.service.notification.EmailDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerRegistrationService {

    private final OwnerRegistrationRepository registrationRepository;
    private final AppUserRepository appUserRepository;
    private final BuildingOwnerRepository buildingOwnerRepository;
    private final OwnerRegistrationMapper mapper;
    private final EmailDispatchService emailDispatchService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url:http://localhost:4200}")
    private String baseUrl;

    // ── Task 3.1/3.2: Submit registration ──────────────────────────────────

    @Transactional
    public OwnerRegistrationResponse submit(OwnerRegistrationRequest request) {
        // Reject if email already has an active app_user
        if (appUserRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        // Reject if a non-rejected registration already exists for this email
        boolean hasPending = registrationRepository.existsByEmailAndStatusIn(
                request.getEmail(),
                List.of(OwnerRegistrationStatus.PENDING, OwnerRegistrationStatus.PENDING_REVIEW));
        if (hasPending) {
            throw new DuplicateResourceException("A pending registration already exists for this email");
        }

        String token = UUID.randomUUID().toString();
        OwnerRegistration registration = OwnerRegistration.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .organisationName(request.getOrganisationName())
                .contactPhone(request.getContactPhone())
                .message(request.getMessage())
                .status(OwnerRegistrationStatus.PENDING)
                .emailVerificationToken(token)
                .emailVerificationExpiresAt(LocalDateTime.now().plusHours(24))
                .build();

        OwnerRegistration saved = registrationRepository.save(registration);

        String verificationUrl = baseUrl + "/owner/verify-email?token=" + token;
        emailDispatchService.sendOwnerRegistrationConfirmEmail(
                saved.getEmail(), saved.getFullName(), saved.getOrganisationName(), verificationUrl);

        return mapper.toResponse(saved);
    }

    // ── Task 3.2: Verify email ──────────────────────────────────────────────

    @Transactional
    public void verifyEmail(String token) {
        OwnerRegistration registration = registrationRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid email verification token"));

        if (registration.getStatus() != OwnerRegistrationStatus.PENDING) {
            throw new InvalidStatusException("This registration has already been verified or processed");
        }
        if (LocalDateTime.now().isAfter(registration.getEmailVerificationExpiresAt())) {
            throw new TokenExpiredException("This verification link has expired");
        }

        registration.setStatus(OwnerRegistrationStatus.PENDING_REVIEW);
        registration.setVerifiedAt(LocalDateTime.now());
        registration.setEmailVerificationToken(null);
        registration.setEmailVerificationExpiresAt(null);
        registrationRepository.save(registration);
    }

    // ── Task 3.2: Resend verification ───────────────────────────────────────

    @Transactional
    public void resendVerification(String email) {
        OwnerRegistration registration = registrationRepository
                .findByEmailAndStatusIn(email, List.of(OwnerRegistrationStatus.PENDING))
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No pending unverified registration found for this email"));

        String token = UUID.randomUUID().toString();
        registration.setEmailVerificationToken(token);
        registration.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(24));
        registrationRepository.save(registration);

        String verificationUrl = baseUrl + "/owner/verify-email?token=" + token;
        emailDispatchService.sendOwnerRegistrationConfirmEmail(
                registration.getEmail(), registration.getFullName(), registration.getOrganisationName(), verificationUrl);
    }

    // ── Task 3.3: Approve registration ─────────────────────────────────────

    @Transactional
    public void approve(Long registrationId) {
        OwnerRegistration registration = getRegistrationById(registrationId);

        if (registration.getStatus() == OwnerRegistrationStatus.APPROVED) {
            throw new InvalidStatusException("This registration has already been processed");
        }
        if (registration.getStatus() != OwnerRegistrationStatus.PENDING_REVIEW) {
            throw new InvalidStatusException("Registration must be in PENDING_REVIEW status to approve");
        }

        // Create app_user
        AppUser user = AppUser.builder()
                .email(registration.getEmail())
                .firstName(extractFirstName(registration.getFullName()))
                .lastName(extractLastName(registration.getFullName()))
                .userPassword(passwordEncoder.encode(UUID.randomUUID().toString())) // temporary; replaced on password-setup
                .userRole(UserRole.BUILDING_OWNER)
                .emailVerified(true)
                .mustChangePassword(true)
                .tenantId(registration.getTenantId() != null ? registration.getTenantId() : 0L)
                .build();
        AppUser savedUser = appUserRepository.save(user);

        // Create building_owner profile
        BuildingOwner profile = BuildingOwner.builder()
                .user(savedUser)
                .businessName(registration.getOrganisationName())
                .contactPhone(registration.getContactPhone())
                .status("APPROVED")
                .tenantId(savedUser.getTenantId())
                .build();
        buildingOwnerRepository.save(profile);

        // Issue password-setup token
        String setupToken = UUID.randomUUID().toString();
        registration.setStatus(OwnerRegistrationStatus.APPROVED);
        registration.setPasswordSetupToken(setupToken);
        registration.setPasswordSetupExpiresAt(LocalDateTime.now().plusHours(48));
        registrationRepository.save(registration);

        String passwordSetupUrl = baseUrl + "/owner/set-password?token=" + setupToken;
        emailDispatchService.sendOwnerApprovedEmail(
                registration.getEmail(), registration.getFullName(), registration.getOrganisationName(), passwordSetupUrl);
    }

    // ── Task 3.4: Reject registration ──────────────────────────────────────

    @Transactional
    public void reject(Long registrationId, String reason) {
        OwnerRegistration registration = getRegistrationById(registrationId);

        if (registration.getStatus() == OwnerRegistrationStatus.APPROVED ||
                registration.getStatus() == OwnerRegistrationStatus.REJECTED) {
            throw new InvalidStatusException("This registration has already been processed");
        }

        registration.setStatus(OwnerRegistrationStatus.REJECTED);
        registrationRepository.save(registration);

        emailDispatchService.sendOwnerRejectedEmail(registration.getEmail(), registration.getFullName(), reason);
    }

    // ── Task 3.5: Re-apply ─────────────────────────────────────────────────

    @Transactional
    public OwnerRegistrationResponse reapply(OwnerRegistrationRequest request) {
        // Only allow re-application if the only existing record is REJECTED
        boolean hasActive = registrationRepository.existsByEmailAndStatusIn(
                request.getEmail(),
                List.of(OwnerRegistrationStatus.PENDING, OwnerRegistrationStatus.PENDING_REVIEW, OwnerRegistrationStatus.APPROVED));
        if (hasActive) {
            throw new DuplicateResourceException("An active registration or account already exists for this email");
        }
        // Delegate to submit — creates a fresh registration
        return submit(request);
    }

    // ── Task 3.6: Set password (password-setup token) ──────────────────────

    @Transactional
    public void setPassword(SetOwnerPasswordRequest request) {
        OwnerRegistration registration = registrationRepository
                .findByPasswordSetupToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid password setup token"));

        if (LocalDateTime.now().isAfter(registration.getPasswordSetupExpiresAt())) {
            throw new TokenExpiredException("This password setup link has expired. Please contact support.");
        }

        AppUser user = appUserRepository.findByEmail(registration.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Owner user account not found"));

        user.setUserPassword(passwordEncoder.encode(request.getPassword()));
        user.setMustChangePassword(false);
        appUserRepository.save(user);

        registration.setPasswordSetupToken(null);
        registration.setPasswordSetupExpiresAt(null);
        registrationRepository.save(registration);
    }

    // ── Task 3.7: Scheduled reminder ───────────────────────────────────────

    @Scheduled(fixedDelayString = "${app.owner-registration.reminder-interval-ms:3600000}")
    public void sendVerificationReminders() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<OwnerRegistration> unreminded = registrationRepository.findUnremindedPendingBefore(OwnerRegistrationStatus.PENDING, cutoff);
        for (OwnerRegistration reg : unreminded) {
            try {
                String newToken = UUID.randomUUID().toString();
                reg.setEmailVerificationToken(newToken);
                reg.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(24));
                reg.setReminderSentAt(LocalDateTime.now());
                registrationRepository.save(reg);

                String verificationUrl = baseUrl + "/owner/verify-email?token=" + newToken;
                emailDispatchService.sendOwnerVerificationReminderEmail(reg.getEmail(), reg.getFullName(), verificationUrl);
                log.info("Sent verification reminder to owner registration {}", reg.getId());
            } catch (Exception e) {
                log.error("Failed to send reminder for owner registration {}", reg.getId(), e);
            }
        }
    }

    // ── Admin query ────────────────────────────────────────────────────────

    public List<OwnerRegistrationSummaryDto> getPendingRegistrations() {
        return registrationRepository.findByStatus(OwnerRegistrationStatus.PENDING_REVIEW)
                .stream()
                .map(mapper::toSummary)
                .collect(Collectors.toList());
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private OwnerRegistration getRegistrationById(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Owner registration not found: " + id));
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "";
        int space = fullName.indexOf(' ');
        return space > 0 ? fullName.substring(0, space) : fullName;
    }

    private String extractLastName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "";
        int space = fullName.indexOf(' ');
        return space > 0 ? fullName.substring(space + 1) : "";
    }
}
