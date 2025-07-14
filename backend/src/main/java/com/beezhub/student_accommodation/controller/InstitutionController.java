package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.InstitutionResponse;
import com.beezhub.student_accommodation.service.InstitutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/institution")
@RequiredArgsConstructor
@Tag(name = "Institution", description = "Endpoints for managing institutions")
public class InstitutionController {

    private final InstitutionService institutionService;

    @GetMapping
    @Operation(summary = "Get all institutions", description = "Returns a list of all registered institutions")
    public List<InstitutionResponse> getInstitutions() {
        return institutionService.getAllInstitutions();
    }

}
