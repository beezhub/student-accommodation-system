package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.YearOfStudyResponse;
import com.beezhub.student_accommodation.service.YearOfStudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/year-of-study")
@RequiredArgsConstructor
public class YearOfStudyController {
    private final YearOfStudyService yearOfStudyService;

    @GetMapping("/")
    public List<YearOfStudyResponse> getAllYearsOfStudy() {
        return yearOfStudyService.getAllYearsOfStudy();
    }
}
