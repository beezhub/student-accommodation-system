package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.DashboardStatsResponse;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final ApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;
    private final StudentRepository studentRepository;
    private final AppUserRepository appUserRepository;

    public DashboardStatsResponse getDashboardStats() {
        return DashboardStatsResponse.builder()
                .totalApplications(applicationRepository.count())
                .pendingApplications(applicationRepository.countByStatus(ApplicationStatus.PENDING))
                .approvedApplications(applicationRepository.countByStatus(ApplicationStatus.APPROVED))
                .rejectedApplications(applicationRepository.countByStatus(ApplicationStatus.REJECTED))
                .totalDocuments(documentRepository.count())
                .pendingDocuments(documentRepository.countByDocumentStatus(DocumentStatus.PENDING))
                .approvedDocuments(documentRepository.countByDocumentStatus(DocumentStatus.APPROVED))
                .rejectedDocuments(documentRepository.countByDocumentStatus(DocumentStatus.REJECTED))
                .totalStudents(studentRepository.count())
                .totalAdmins(appUserRepository.countByUserRole(UserRole.ADMIN))
                .build();
    }
}
