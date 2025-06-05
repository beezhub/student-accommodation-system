package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.InstitutionResponse;
import com.beezhub.student_accommodation.service.InstitutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/institution")
@RequiredArgsConstructor
public class InstitutionController {

    private final InstitutionService institutionService;

    @GetMapping("/")
    public List<InstitutionResponse> getInstitutions() {
        return institutionService.getAllInstitutions();
    }

}
