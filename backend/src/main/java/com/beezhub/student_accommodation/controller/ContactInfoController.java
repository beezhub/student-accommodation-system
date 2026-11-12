package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.ContactInfoResponse;
import com.beezhub.student_accommodation.service.ContactInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, so the home page can show it to a visitor who is not signed in. SecurityConfig lists it
 * with the other anonymous GET paths.
 */
@Tag(name = "Contact", description = "How to reach support for this deployment")
@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactInfoController {

    private final ContactInfoService contactInfoService;

    @Operation(summary = "Support email and phone",
            description = "Set by APP_SUPPORT_EMAIL and APP_CONTACT_PHONE. phone is null when none is configured.")
    @GetMapping
    public ResponseEntity<ContactInfoResponse> getContactInfo() {
        return ResponseEntity.ok(contactInfoService.getContactInfo());
    }
}
