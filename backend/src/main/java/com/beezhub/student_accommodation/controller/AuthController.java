package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.ChangePasswordRequest;
import com.beezhub.student_accommodation.model.dto.LoginRequest;
import com.beezhub.student_accommodation.model.dto.LoginResponse;
import com.beezhub.student_accommodation.model.dto.SignupRequest;
import com.beezhub.student_accommodation.model.dto.UserData;
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

        @Operation(summary = "Logout (stateless)")
        @ApiResponse(responseCode = "204", description = "Logged out")
        @PostMapping("/logout")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<Void> logout() {
                // Stateless JWT: client should discard token. No server action required.
                return ResponseEntity.noContent().build();
        }
}