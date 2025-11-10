package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Application", description = "Endpoints for student accommodation applications")
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService applicationService;

    @Operation(
        summary = "Submit a student accommodation application",
        description = "Submits an application for accommodation for the given student ID."
    )
    @PostMapping("/submit")
    public ResponseEntity<ApplicationResponse> submitApplication(
            @Parameter(description = "ID of the student submitting the application", required = true)
            @RequestParam("studentId") Long studentId) {

        ApplicationResponse applicationResponse = applicationService.submitApplication(studentId);
        return ResponseEntity.ok(applicationResponse);
    }

    @Operation(
        summary = "Get application with application ID",
        description = "Retrieves application for the given application ID."
    )
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @Parameter(description = "ID of the application to retrieve", required = true)
            @PathVariable("applicationId") Long applicationId ) {
        var applicationResponse = applicationService.getApplicationById(applicationId);
        return ResponseEntity.ok(applicationResponse);
    }

    @Operation(
        summary = "Get all applications for a student",
        description = "Retrieves all applications for the given student ID."
    )
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<ApplicationResponse>> getApplicationsByStudentId(
            @Parameter(description = "ID of the student to retrieve applications for", required = true)
            @PathVariable("studentId") Long studentId) {
        return ResponseEntity.ok( applicationService.getApplicationsByStudentId(studentId));
    }


    @Operation(
            summary = "Get all applications by status",
            description = "Retrieves all applications with the specified status."
    )
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ApplicationResponse>> getApplicationsByStatus(
            @Parameter(description = "Status of the applications to retrieve", required = true)
            @PathVariable("status") String status) {
        return ResponseEntity.ok(applicationService.getApplicationsByStatus(status));
    }


}
