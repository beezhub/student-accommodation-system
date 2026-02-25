package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {
    private final AuthService authService;

    @Operation(
            summary = "User login",
            description = "Authenticates a user and returns a JWT token"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully authenticated"
    )
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.validateLogin(loginRequest.getEmail(), loginRequest.getPassword());
    }

    @Operation(
            summary = "User registration",
            description = "Register a new user and return JWT token"
    )
    @ApiResponse(
            responseCode = "201",
            description = "User successfully registered"
    )
    @PostMapping("/signup")
    public ResponseEntity<LoginResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(201).body(authService.signup(request));
    }

        @Operation(summary = "Get current user profile")
        @ApiResponse(responseCode = "200", description = "User profile returned")
        @GetMapping("/me")
        @PreAuthorize("isAuthenticated()")
        public UserData me(Authentication authentication) {
                return authService.me(authentication);
        }

        @Operation(summary = "Refresh JWT token")
        @ApiResponse(responseCode = "200", description = "New token issued")
        @PostMapping("/refresh-token")
        @PreAuthorize("isAuthenticated()")
        public LoginResponse refreshToken(Authentication authentication) {
                return authService.refreshToken(authentication);
        }

        @Operation(summary = "Change password")
        @ApiResponse(responseCode = "204", description = "Password changed")
        @PostMapping("/change-password")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                                                           Authentication authentication) {
                authService.changePassword(request, authentication);
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Logout")
        @ApiResponse(responseCode = "204", description = "Logged out")
        @PostMapping("/logout")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<Void> logout(Authentication authentication) {
                authService.logout(authentication);
                return ResponseEntity.noContent().build();
        }

        @Operation(
                summary = "Request password reset",
                description = "Send password reset email to the provided email address"
        )
        @ApiResponse(responseCode = "200", description = "Password reset email sent")
        @PostMapping("/forgot-password")
        public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
                return ResponseEntity.ok(authService.requestPasswordReset(request.getEmail()));
        }

        @Operation(
                summary = "Verify reset token validity",
                description = "Check if a password reset token is valid and not expired"
        )
        @ApiResponse(responseCode = "200", description = "Token validation result returned")
        @PostMapping("/verify-reset-token")
        public ResponseEntity<TokenValidationResponse> verifyResetToken(@Valid @RequestBody VerifyEmailRequest request) {
                return ResponseEntity.ok(authService.verifyResetToken(request.getToken()));
        }

        @Operation(
                summary = "Reset password with token",
                description = "Reset user password using a valid reset token"
        )
        @ApiResponse(responseCode = "200", description = "Password reset successfully")
        @PostMapping("/reset-password")
        public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
                return ResponseEntity.ok(authService.resetPassword(request.getToken(), request.getNewPassword()));
        }
        

        @Operation(
                summary = "Verify email with token",
                description = "Verify user's email address using verification token"
        )
        @ApiResponse(responseCode = "200", description = "Email verified successfully")
        @PostMapping("/verify-email")
        public ResponseEntity<MessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
                return ResponseEntity.ok(authService.verifyEmail(request.getToken()));
        }

        @Operation(
                summary = "Resend verification email",
                description = "Resend email verification link to the provided email address"
        )
        @ApiResponse(responseCode = "200", description = "Verification email sent")
        @PostMapping("/resend-verification")
        public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
                return ResponseEntity.ok(authService.sendVerificationEmail(request.getEmail()));
        }

        @Operation(
                summary = "Delete user account",
                description = "Permanently delete the authenticated user's account"
        )
        @ApiResponse(responseCode = "204", description = "Account deleted successfully")
        @PostMapping("/delete-account")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<Void> deleteAccount(@Valid @RequestBody DeleteAccountRequest request,
                                                   Authentication authentication) {
                authService.deleteAccount(request, authentication);
                return ResponseEntity.noContent().build();
        }

        @Operation(
                summary = "Anonymise a user account (admin)",
                description = "Replaces all PII for the specified user with placeholder values. "
                        + "The account is retained for referential integrity but the user can no longer log in. "
                        + "Use this instead of delete when GDPR erasure is required while preserving audit trails."
        )
        @ApiResponse(responseCode = "204", description = "Account anonymised successfully")
        @PostMapping("/admin/anonymize/{userId}")
        @PreAuthorize("hasRole('PLATFORM_ADMIN')")
        public ResponseEntity<Void> anonymizeAccount(@PathVariable Long userId) {
                authService.anonymizeAccount(userId);
                return ResponseEntity.noContent().build();
        }
}