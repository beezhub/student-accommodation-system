package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.*;
import com.beezhub.student_accommodation.mapper.AppUserMapper;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.EmailVerificationToken;
import com.beezhub.student_accommodation.model.entity.PasswordResetToken;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.allocation.RoomOccupancyManager;
import com.beezhub.student_accommodation.util.SecureTokenHasher;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.EmailVerificationTokenRepository;
import com.beezhub.student_accommodation.repository.PasswordResetTokenRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.WaitingListRepository;
import com.beezhub.student_accommodation.repository.UserNotificationRepository;
import com.beezhub.student_accommodation.repository.ActivityLogRepository;
import com.beezhub.student_accommodation.security.jwt.JwtUtil;
import com.beezhub.student_accommodation.security.user.AppUserDetails;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import com.beezhub.student_accommodation.service.tenant.TenantService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.beezhub.student_accommodation.model.entity.AppUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int FAILED_LOGIN_LOCK_MINUTES = 15;

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final AppUserMapper appUserMapper;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailService emailService;
    private final StudentRepository studentRepository;
    private final ApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final RoomOccupancyManager roomOccupancyManager;
    private final WaitingListRepository waitingListRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final LoginAttemptService loginAttemptService;
    private final TenantService tenantService;
    private final UserAnonymizationService userAnonymizationService;
    private final MeterRegistry meterRegistry;

    // No default: app.base-url is required, and this one disagreed with application.yml (:4200),
    // so password-reset links pointed at the wrong port whenever it was unset (GH-135).
    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.password-reset.expiry-hours:24}")
    private int passwordResetExpiryHours;

    @Value("${app.email-verification.expiry-hours:24}")
    private int emailVerificationExpiryHours;

    @Transactional
    @Timed(value = "service.auth.signup", percentiles = {0.5, 0.95, 0.99})
    public LoginResponse signup(SignupRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyRegisteredException("Email already registered");
        }
        var user = appUserMapper.toEntity(request, passwordEncoder.encode(request.getPassword()));
        // Public signup always creates STUDENT accounts (see AppUserMapper.toEntity).
        // Students are platform-level entities (ADR-001) and must not be scoped to any
        // building-owner tenant.  Assign the PLATFORM sentinel (id=0) so the Hibernate
        // tenant filter is bypassed for this user — student data access is enforced by
        // student_id / user_id ownership instead.
        user.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        var save = userRepository.save(user);

        try {
            sendVerificationEmail(save.getEmail());
            log.info("Verification email sent to new user: {}", save.getEmail());
        } catch (Exception e) {
            log.error("action=signupVerificationEmailFailed userId={}", save.getId(), e);
        }

        var userData = appUserMapper.toUserData(save);
        String token = jwtUtil.generateToken(new AppUserDetails(
                user.getEmail(),
                user.getUserPassword(),
                user.getUserRole().name(),
                user.getTenantId()));
        return new LoginResponse(token, userData);
    }

    /**
     * Authenticates a user and maintains the failed-attempt counters.
     *
     * <p>{@code noRollbackFor} is load-bearing: the failure branch saves the incremented
     * {@code failed_login_attempts} and {@code account_locked_until}, then rethrows. Without this,
     * Spring rolled that save back with the transaction and the database lockout never took effect,
     * leaving brute-force protection entirely dependent on Redis (GH-117).
     */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    @Timed(value = "service.auth.login", percentiles = {0.5, 0.95, 0.99})
    public LoginResponse validateLogin(String username, String password) {
        // 1. Check if user exists first to check lock status
        var userOpt = userRepository.findByEmail(username);
        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();

            // Check DB lock
            if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
                log.warn("Login attempt for locked account: {}", user.getId());
                throw new BadCredentialsException("Invalid email or password");
            }

            // Check Redis lock (fast path)
            if (loginAttemptService.isBlocked(user.getId())) {
                log.warn("Login attempt for blocked account (Redis): {}", user.getId());
                throw new BadCredentialsException("Invalid email or password");
            }
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            var user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            // Login successful: reset counters
            loginAttemptService.loginSucceeded(user.getId());
            if (user.getFailedLoginAttempts() > 0 || user.getAccountLockedUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setAccountLockedUntil(null);
                userRepository.save(user);
            }

            String token = jwtUtil.generateToken(userDetails, Boolean.TRUE.equals(user.getMustChangePassword()));
            var userData = appUserMapper.toUserData(user);

            meterRegistry.counter("sas.auth.login.success", "tenant", String.valueOf(user.getTenantId())).increment();

            return new LoginResponse(token, userData);

        } catch (BadCredentialsException e) {
            // Login failed: increment counters
            if (userOpt.isPresent()) {
                AppUser user = userOpt.get();
                loginAttemptService.loginFailed(user.getId());

                meterRegistry.counter("sas.auth.login.failure", "tenant", String.valueOf(user.getTenantId()))
                        .increment();

                int attempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(attempts);

                if (attempts >= 5) { // Max attempts from LoginAttemptService is 5
                    user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(FAILED_LOGIN_LOCK_MINUTES));
                    log.warn("Account locked due to too many failed attempts: {}", user.getId());
                }
                userRepository.save(user);
            } else {
                meterRegistry.counter("sas.auth.login.failure", "tenant", "unknown").increment();
            }
            throw e;
        }
    }

    @Transactional
    public void unlockAccount(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (isWaitingForACompany(user)) {
            log.warn("action=unlockRefused reason=noCompany userId={} role={}", userId, user.getUserRole());
            // The company admin screen (#413) offers tenant admins only, so building staff are
            // still pointed at the API; the screen labels are the ones the admin clicks.
            String wayOut = user.getUserRole() == UserRole.TENANT_ADMIN
                    ? "Tenants > Manage admin users > Add Existing Admin"
                    : "POST /api/admin/unassigned-admins/" + userId + "/assign";
            throw new UnassignedAdminUnlockException(String.format(
                    "%s %s is not linked to a company yet. Unlocking would give them access to every "
                            + "company's data. Add them to a company instead, which unlocks the account "
                            + "too: %s.",
                    user.getFirstName(), user.getLastName(), wayOut));
        }

        user.setAccountLockedUntil(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        // Clear Redis
        loginAttemptService.loginSucceeded(userId);

        log.info("Account manually unlocked for user: {}", userId);
    }

    /**
     * A TENANT_ADMIN or BUILDING_STAFF still on the platform tenant has no company yet, and its lock
     * is the only thing keeping it out of every company's data (#412, GH-212). These are exactly the
     * roles V2026_09_06_1500 locks. The deprecated ADMIN is not among them: on tenant 0 it is most
     * likely a real operator, who must stay unlockable after an ordinary lockout.
     */
    private static boolean isWaitingForACompany(AppUser user) {
        return TenantContext.PLATFORM_TENANT_ID.equals(user.getTenantId())
                && (user.getUserRole() == UserRole.TENANT_ADMIN || user.getUserRole() == UserRole.BUILDING_STAFF);
    }

    public UserData me(Authentication authentication) {
        var user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return appUserMapper.toUserData(user);
    }

    /**
     * Issues a fresh JWT for the caller and retires the one they presented.
     *
     * <p>{@code JwtAuthenticationFilter} puts the username (a {@code String}) in the principal, so
     * the previous cast to {@code UserDetails} always threw and this endpoint never worked. The
     * user is re-read from the database rather than trusted from the old token, so a role change,
     * a tenant move or a lock takes effect on refresh instead of persisting for the token's
     * lifetime, and the old jti is blacklisted so refreshing cannot be used to keep two live
     * tokens.
     */
    @Transactional
    public LoginResponse refreshToken(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UserNotFoundException("No authenticated user found");
        }
        AppUser user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getIsAnonymized())) {
            throw new UserNotFoundException("User not found");
        }
        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            // 401, matching validateLogin. AccountLockedException is reused by the rate limiter and
            // maps to 429, which would be misleading here.
            log.warn("action=refreshRejected reason=accountLocked userId={}", user.getId());
            throw new BadCredentialsException("Account is locked");
        }

        // Retire the presented token before handing out its replacement.
        blacklistCurrentToken(authentication);

        AppUserDetails userDetails = new AppUserDetails(
                user.getEmail(), user.getUserPassword(), user.getUserRole().name(), user.getTenantId());
        String token = jwtUtil.generateToken(userDetails, Boolean.TRUE.equals(user.getMustChangePassword()));
        log.info("action=tokenRefreshed userId={}", user.getId());
        return new LoginResponse(token, appUserMapper.toUserData(user));
    }

    public void logout(Authentication authentication) {
        if (authentication == null) {
            return;
        }
        blacklistCurrentToken(authentication);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, Authentication authentication) {
        var user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getUserPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Update password
        user.setUserPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        // Blacklist current token
        blacklistCurrentToken(authentication);
    }

    @Transactional
    public void deleteAccount(DeleteAccountRequest request, Authentication authentication) {
        log.info("Attempting to delete account for user: {}", authentication.getName());

        var user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Verify password before deletion
        if (!passwordEncoder.matches(request.getPassword(), user.getUserPassword())) {
            throw new BadCredentialsException("Password is incorrect");
        }

        // Find student record if exists
        var studentOpt = studentRepository.findByAppUser_Id(user.getId());

        if (studentOpt.isPresent()) {
            var student = studentOpt.get();
            log.info("Deleting related data for student: {}", student.getId());

            // Delete room allocations. The rooms they occupied have to be recomputed afterwards:
            // this is a hard delete of OFFERED, ACCEPTED and ACTIVE rows, and without a sync the
            // room keeps the departed student in its occupancy for ever. A capacity-one room lands
            // on FULL, which the scoring strategies exclude, so it can never receive another
            // allocation and nothing later triggers a recount (GH-136).
            List<Room> affectedRooms = roomAllocationRepository.findByStudent_Id(student.getId()).stream()
                    .map(RoomAllocation::getRoom)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Room::getId, room -> room, (a, b) -> a))
                    .values().stream().toList();

            roomAllocationRepository.deleteByStudent(student);
            roomAllocationRepository.flush();
            affectedRooms.forEach(roomOccupancyManager::syncOccupancy);
            log.info("Deleted room allocations");

            // Delete waiting list entries
            waitingListRepository.deleteByStudent(student);
            log.info("Deleted waiting list entries");

            // Delete applications
            applicationRepository.deleteByStudent(student);
            log.info("Deleted applications");

            // Delete documents
            documentRepository.deleteByStudent(student);
            log.info("Deleted documents");

            // Delete student record
            studentRepository.delete(student);
            log.info("Deleted student record");
        }

        // Delete user notifications
        userNotificationRepository.deleteByUser(user);
        log.info("Deleted user notifications");

        // Delete activity logs
        activityLogRepository.deleteByUser(user);
        log.info("Deleted activity logs");

        // Delete associated tokens
        passwordResetTokenRepository.deleteByUser(user);
        emailVerificationTokenRepository.deleteByUser(user);
        log.info("Deleted tokens");

        // Blacklist current token
        blacklistCurrentToken(authentication);

        // Finally, delete user
        userRepository.delete(user);

        log.info("Account deleted successfully for user: {}", authentication.getName());
    }

    /**
     * Anonymises a user account identified by userId (admin-only operation).
     * Replaces all PII with placeholder values and invalidates the user's credentials.
     * The account record is retained for referential integrity; the user cannot log in
     * after anonymisation.
     *
     * @param userId the ID of the user to anonymise
     */
    @Transactional
    public void anonymizeAccount(Long userId) {
        log.info("Admin-initiated anonymisation for userId: {}", userId);
        userAnonymizationService.anonymizeUser(userId);
        log.info("Account anonymised successfully for userId: {}", userId);
    }

    private void blacklistCurrentToken(Authentication authentication) {
        Object credentials = authentication.getCredentials();
        if (credentials instanceof String) {
            String token = (String) credentials;
            try {
                String jti = jwtUtil.extractJti(token);
                long remainingTtl = jwtUtil.getRemainingTtlSeconds(token);

                if (jti != null && remainingTtl > 0) {
                    tokenBlacklistService.blacklistToken(jti, remainingTtl);
                }
            } catch (Exception e) {
                log.warn("Failed to blacklist token during logout/changePassword", e);
            }
        }
    }

    public AppUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserNotFoundException("No authenticated user found");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * Always reports the same outcome, whether the account exists, the send succeeded, or the
     * mail server was down.
     *
     * <p>It already returned the same message for an unknown email, but then threw a 500 when the
     * send failed — which only happens for an account that exists. That difference was an
     * enumeration oracle: a caller could tell a real address from a fake one by knocking the mail
     * server over, or simply by observing a transient failure (GH-154).
     */
    @Transactional
    public MessageResponse requestPasswordReset(String email) {
        log.info("action=passwordResetRequested");

        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Deliberately indistinguishable from the success path.
            log.info("action=passwordResetRequestedForUnknownAccount");
            return new MessageResponse("Password reset instructions have been sent to your email");
        }

        AppUser user = userOpt.get();

        passwordResetTokenRepository.deleteByUserId(user.getId());

        // The plaintext goes in the email; only its hash is stored. Plaintext at rest meant a
        // database read handed over every live reset token (GH-154).
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(passwordResetExpiryHours);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(SecureTokenHasher.hash(token))
                .user(user)
                .expiryDate(expiryDate)
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Send password reset email
        String resetUrl = baseUrl + "/reset-password?token=" + token;
        try {
            emailService.sendSimpleEmail(
                    email,
                    "Password Reset Request",
                    "Hello " + user.getFirstName() + ",\n\n" +
                            "You have requested to reset your password. Please click the link below to reset your password:\n\n"
                            +
                            resetUrl + "\n\n" +
                            "This link will expire in " + passwordResetExpiryHours + " hours.\n\n" +
                            "If you did not request this, please ignore this email.\n\n" +
                            "Best regards,\n" +
                            "Student Accommodation Team");
            log.info("action=passwordResetEmailSent userId={}", user.getId());
        } catch (Exception e) {
            // Logged, not rethrown. Throwing here reveals that the account exists.
            log.error("action=passwordResetEmailFailed userId={}", user.getId(), e);
        }

        return new MessageResponse("Password reset instructions have been sent to your email");
    }

    @Transactional
    public TokenValidationResponse verifyResetToken(String token) {
        log.info("Verifying reset token");

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(SecureTokenHasher.hash(token))
                .orElse(null);

        if (resetToken == null) {
            return new TokenValidationResponse(false, "Invalid token");
        }

        if (resetToken.isUsed()) {
            return new TokenValidationResponse(false, "Token has already been used");
        }

        if (resetToken.isExpired()) {
            return new TokenValidationResponse(false, "Token has expired");
        }

        return new TokenValidationResponse(true, "Token is valid");
    }

    @Transactional
    public MessageResponse resetPassword(String token, String newPassword) {
        log.info("Attempting to reset password with token");

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(SecureTokenHasher.hash(token))
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));

        if (resetToken.isUsed()) {
            throw new InvalidTokenException("Password reset token has already been used");
        }

        if (resetToken.isExpired()) {
            throw new TokenExpiredException("Password reset token has expired");
        }

        AppUser user = resetToken.getUser();
        user.setUserPassword(passwordEncoder.encode(newPassword));
        clearFailedLoginLockout(user);
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successful for user: {}", user.getEmail());
        return new MessageResponse("Password has been reset successfully");
    }

    /**
     * Without this the correct new password was refused for up to 15 minutes (#440). A lock further
     * away than a failed-login lock can ever be was set by an administrator (a deactivated account,
     * an admin with no company, GH-212), so it stays: a reset link only proves the mailbox.
     */
    private void clearFailedLoginLockout(AppUser user) {
        user.setFailedLoginAttempts(0);
        loginAttemptService.loginSucceeded(user.getId());

        LocalDateTime latestFailedLoginLock = LocalDateTime.now().plusMinutes(FAILED_LOGIN_LOCK_MINUTES);
        if (user.getAccountLockedUntil() != null && !user.getAccountLockedUntil().isAfter(latestFailedLoginLock)) {
            user.setAccountLockedUntil(null);
        }
    }

    // ==================== Email Verification Methods ====================

    /**
     * Always reports the same outcome, whether the account exists, is already verified, or the
     * send failed.
     *
     * <p>It previously returned 404 for an unknown address and a distinct error for an already
     * verified one, so an unauthenticated caller could enumerate registered accounts and learn
     * which had completed verification (GH-154).
     */
    @Transactional
    public MessageResponse sendVerificationEmail(String email) {
        log.info("action=verificationEmailRequested");

        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("action=verificationEmailRequestedForUnknownAccount");
            return new MessageResponse("Verification email has been sent to your email address");
        }

        AppUser user = userOpt.get();
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            log.info("action=verificationEmailSkipped reason=alreadyVerified userId={}", user.getId());
            return new MessageResponse("Verification email has been sent to your email address");
        }

        // Delete any existing verification tokens for this user
        emailVerificationTokenRepository.deleteByUserId(user.getId());

        // Generate new token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(emailVerificationExpiryHours);

        // Hashed at rest for the same reason as the reset token (GH-154).
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(SecureTokenHasher.hash(token))
                .user(user)
                .expiryDate(expiryDate)
                .used(false)
                .build();

        emailVerificationTokenRepository.save(verificationToken);

        // Send verification email
        String verificationUrl = baseUrl + "/verify-email?token=" + token;
        try {
            emailService.sendSimpleEmail(
                    email,
                    "Email Verification",
                    "Hello " + user.getFirstName() + ",\n\n" +
                            "Thank you for registering! Please click the link below to verify your email address:\n\n" +
                            verificationUrl + "\n\n" +
                            "This link will expire in " + emailVerificationExpiryHours + " hours.\n\n" +
                            "If you did not register, please ignore this email.\n\n" +
                            "Best regards,\n" +
                            "Student Accommodation Team");
            log.info("action=verificationEmailSent userId={}", user.getId());
        } catch (Exception e) {
            // Logged, not rethrown: throwing reveals that the account exists.
            log.error("action=verificationEmailFailed userId={}", user.getId(), e);
        }

        return new MessageResponse("Verification email has been sent to your email address");
    }

    @Transactional
    public MessageResponse verifyEmail(String token) {
        log.info("Attempting to verify email with token");

        EmailVerificationToken verificationToken = emailVerificationTokenRepository
                .findByToken(SecureTokenHasher.hash(token))
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (verificationToken.isUsed()) {
            throw new InvalidTokenException("Verification token has already been used");
        }

        if (verificationToken.isExpired()) {
            throw new TokenExpiredException("Verification token has expired");
        }

        AppUser user = verificationToken.getUser();

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new EmailAlreadyVerifiedException("Email is already verified");
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        // Mark token as used
        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);

        log.info("Email verification successful for user: {}", user.getEmail());
        return new MessageResponse("Email has been verified successfully");
    }
}
