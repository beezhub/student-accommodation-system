package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.*;
import com.beezhub.student_accommodation.mapper.AppUserMapper;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.EmailVerificationToken;
import com.beezhub.student_accommodation.model.entity.PasswordResetToken;
import com.beezhub.student_accommodation.repository.*;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

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
    private final WaitingListRepository waitingListRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    @Value("${app.password-reset.expiry-hours:24}")
    private int passwordResetExpiryHours;

    @Value("${app.email-verification.expiry-hours:24}")
    private int emailVerificationExpiryHours;

    @Transactional
    public LoginResponse signup(SignupRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyRegisteredException("Email already registered");
        }
        var user = appUserMapper.toEntity(request, passwordEncoder.encode(request.getPassword()));
        var save = userRepository.save(user);

        try {
            sendVerificationEmail(save.getEmail());
            log.info("Verification email sent to new user: {}", save.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email during signup to: {}", save.getEmail(), e);
        }

        var userData = appUserMapper.toUserData(save);
        String token = jwtUtil.generateToken(new AppUserDetails(
                user.getEmail(),
                user.getUserPassword(),
                user.getUserRole().name()
        ));
        return new LoginResponse(token, userData);
    }

    public LoginResponse validateLogin(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        var user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Generate token and return user data
        // The mustChangePassword flag will be included in userData
        // Frontend should check this and enforce password change if needed
        String token = jwtUtil.generateToken(userDetails);
        var userData = appUserMapper.toUserData(user);
        return new LoginResponse(token, userData);
    }

    public UserData me(Authentication authentication) {
        var user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return appUserMapper.toUserData(user);
    }

    public LoginResponse refreshToken(Authentication authentication) {
        // Re-issue a new JWT for the authenticated principal
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);
        var user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        var userData = appUserMapper.toUserData(user);
        return new LoginResponse(token, userData);
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

            // Delete room allocations
            roomAllocationRepository.deleteByStudent(student);
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

    @Transactional
    public MessageResponse requestPasswordReset(String email) {
        log.info("Password reset requested for email: {}", email);

        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        passwordResetTokenRepository.deleteByUserId(user.getId());

        // Generate new token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(passwordResetExpiryHours);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
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
                            "You have requested to reset your password. Please click the link below to reset your password:\n\n" +
                            resetUrl + "\n\n" +
                            "This link will expire in " + passwordResetExpiryHours + " hours.\n\n" +
                            "If you did not request this, please ignore this email.\n\n" +
                            "Best regards,\n" +
                            "Student Accommodation Team"
            );
            log.info("Password reset email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", email, e);
            throw new RuntimeException("Failed to send password reset email");
        }

        return new MessageResponse("Password reset instructions have been sent to your email");
    }

    @Transactional
    public TokenValidationResponse verifyResetToken(String token) {
        log.info("Verifying reset token");

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
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

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));

        if (resetToken.isUsed()) {
            throw new InvalidTokenException("Password reset token has already been used");
        }

        if (resetToken.isExpired()) {
            throw new TokenExpiredException("Password reset token has expired");
        }

        AppUser user = resetToken.getUser();
        user.setUserPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successful for user: {}", user.getEmail());
        return new MessageResponse("Password has been reset successfully");
    }

    // ==================== Email Verification Methods ====================

    @Transactional
    public MessageResponse sendVerificationEmail(String email) {
        log.info("Sending verification email to: {}", email);

        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new EmailAlreadyVerifiedException("Email is already verified");
        }

        // Delete any existing verification tokens for this user
        emailVerificationTokenRepository.deleteByUserId(user.getId());

        // Generate new token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(emailVerificationExpiryHours);

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
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
                            "Student Accommodation Team"
            );
            log.info("Verification email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", email, e);
            throw new RuntimeException("Failed to send verification email");
        }

        return new MessageResponse("Verification email has been sent to your email address");
    }

    @Transactional
    public MessageResponse verifyEmail(String token) {
        log.info("Attempting to verify email with token");

        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
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
