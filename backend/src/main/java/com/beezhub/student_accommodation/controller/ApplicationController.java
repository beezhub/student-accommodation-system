package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService applicationService;

    @PostMapping("/submit")
    public ResponseEntity<ApplicationResponse> submitApplication(@RequestParam("studentId") Long studentId) {

        ApplicationResponse applicationResponse = applicationService.submitApplication(studentId);
        return ResponseEntity.ok(applicationResponse);

    }

}
