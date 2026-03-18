package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.OwnerRegistrationRequest;
import com.beezhub.student_accommodation.model.dto.OwnerRegistrationResponse;
import com.beezhub.student_accommodation.model.dto.SetOwnerPasswordRequest;
import com.beezhub.student_accommodation.service.OwnerRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/owner")
@RequiredArgsConstructor
@Tag(name = "Owner Registration", description = "Public building owner self-registration endpoints")
public class OwnerRegistrationController {

    private final OwnerRegistrationService ownerRegistrationService;

    @Operation(summary = "Submit a new building owner registration request")
    @PostMapping("/register")
    public ResponseEntity<OwnerRegistrationResponse> register(@Valid @RequestBody OwnerRegistrationRequest request) {
        return ResponseEntity.status(201).body(ownerRegistrationService.submit(request));
    }

    @Operation(summary = "Verify email address using token from registration email")
    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        ownerRegistrationService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Resend email verification link")
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestParam String email) {
        ownerRegistrationService.resendVerification(email);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Set password using token from approval email (activates account)")
    @PostMapping("/set-password")
    public ResponseEntity<Void> setPassword(@Valid @RequestBody SetOwnerPasswordRequest request) {
        ownerRegistrationService.setPassword(request);
        return ResponseEntity.ok().build();
    }
}
