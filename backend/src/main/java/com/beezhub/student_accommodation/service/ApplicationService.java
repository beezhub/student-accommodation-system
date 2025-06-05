package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.*;

@RequiredArgsConstructor
@Slf4j
@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final ApplicationMapper applicationMapper;

    public ApplicationResponse submitApplication(Long studentId) {
        log.info("Submitting application for student with ID: {}", studentId);
        return studentRepository.findById(studentId)
                .map(student -> {
                    Application application = Application.builder()
                            .applicationCode(generateApplicationCode())
                            .applicationDate(LocalDate.now())
                            .status(PENDING)
                            .student(student)
                            .build();
                    Application savedApplication = applicationRepository.save(application);
                    ApplicationResponse response = applicationMapper.toResponse(savedApplication);
                    return response;
                })
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));
    }

    private String generateApplicationCode() {
        int year = LocalDate.now().getYear();
        long count = applicationRepository.countByApplicationCodeStartingWith("APP-" + year + "-");
        String sequence = String.format("%03d", count + 1);
        return "APP-" + year + "-" + sequence;
    }
}
