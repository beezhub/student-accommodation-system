package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.StagedAllocation;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.repository.StagedAllocationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.service.document.DocumentExpiryQueryService;
import com.beezhub.student_accommodation.specification.ApplicationSpecification;
import com.beezhub.student_accommodation.util.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for searching and filtering applications.
 * Handles all search/query operations with no side effects.
 * Extracted from ApplicationService per TASK-2.04.
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class ApplicationSearchService {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final BuildingRepository buildingRepository;
    private final DocumentRepository documentRepository;
    private final StagedAllocationRepository stagedAllocationRepository;
    private final ApplicationMapper applicationMapper;
    private final DocumentTypeRepository documentTypeRepository;
    private final com.beezhub.student_accommodation.service.DocumentTypeService documentTypeService;
    private final DocumentExpiryQueryService documentExpiryQueryService;

    /**
     * Retrieves all applications with pagination.
     * Admin only.
     *
     * @param pageable Pagination parameters
     * @return PageResponse with applications and enriched details
     */
    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> getAllApplications(Pageable pageable) {
        log.info("Retrieving paginated applications");
        Page<Application> page = applicationRepository.findAll(pageable);

        List<ApplicationResponse> responses = enrichApplicationDetails(page.getContent());

        Page<ApplicationResponse> responsePage = new org.springframework.data.domain.PageImpl<>(responses,
                pageable,
                page.getTotalElements());
        return PageMapper.toPageResponse(responsePage);
    }

    /**
     * Searches applications by filters with pagination.
     * Admin only.
     */
    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> searchApplications(String applicationCode, String studentName,
            ApplicationStatus status, String allocationStatus, String preferredBuilding,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {
        return searchApplications(applicationCode, studentName, status, allocationStatus, preferredBuilding,
                startDate, endDate, null, null, pageable);
    }

    /** As above, also filtered by institution and intake (GH-496). */
    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> searchApplications(String applicationCode, String studentName,
            ApplicationStatus status, String allocationStatus, String preferredBuilding,
            LocalDate startDate, LocalDate endDate, Long institutionId, Long intakeId,
            Pageable pageable) {
        log.info(
                "Searching applications with filters - code: {}, student: {}, status: {}, allocationStatus: {}, building: {}, startDate: {}, endDate: {}",
                applicationCode, studentName, status, allocationStatus, preferredBuilding, startDate,
                endDate);
        Specification<Application> spec = ApplicationSpecification.filterBy(applicationCode, studentName,
                status,
                allocationStatus, preferredBuilding, startDate, endDate)
                .and(ApplicationSpecification.forInstitutionAndIntake(institutionId, intakeId));
        Page<Application> page = applicationRepository.findAll(spec, pageable);

        List<ApplicationResponse> responses = enrichApplicationDetails(page.getContent());

        Page<ApplicationResponse> responsePage = new org.springframework.data.domain.PageImpl<>(responses,
                pageable,
                page.getTotalElements());

        log.info("Found {} applications matching criteria", page.getNumberOfElements());
        return PageMapper.toPageResponse(responsePage);
    }

    /**
     * Retrieves all applications by status.
     * Admin only.
     */
    /**
     * The bare valueOf leaked the enum fully-qualified class name to the caller, because its
     * IllegalArgumentException reached the conflict handler which echoed the message (GH-149).
     * The type alone cannot separate that from the messages this codebase writes deliberately, so
     * the guard has to sit here at the parse rather than in the handler.
     */
    private static ApplicationStatus parseStatus(String status) {
        try {
            return ApplicationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidStatusException("Invalid application status: " + status);
        }
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByStatus(String status) {
        log.info("Retrieving applications for status {}", status);
        ApplicationStatus applicationStatus = parseStatus(status);
        return applicationRepository.findByStatus(applicationStatus)
                .stream()
                .map(applicationMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Enriches application responses with document and allocation details.
     * Fetches related data in batch to avoid N+1 queries.
     * Documents are queried directly from the student's document pack (student_id).
     */
    private List<ApplicationResponse> enrichApplicationDetails(List<Application> applications) {
        if (applications.isEmpty()) {
            return new ArrayList<>();
        }

        // Fetch required doc count for the current tenant's effective view.
        // Admin requests are always tenant-scoped (Hibernate filter active), so all
        // applications in this batch belong to the same tenant as TenantContext.
        int requiredDocCount = documentTypeService
                .getEffectiveRequiredTypeNames(com.beezhub.student_accommodation.service.tenant.TenantContext.getTenantId() != null
                        ? com.beezhub.student_accommodation.service.tenant.TenantContext.getTenantId()
                        : 0L)
                .size();

        // Collect student IDs from applications
        List<Long> studentIds = applications.stream()
                .map(app -> app.getStudent().getId())
                .distinct()
                .collect(Collectors.toList());

        // Batch fetch all documents for those students; group by student_id
        List<Document> allDocuments = documentRepository.findAllByStudent_IdIn(studentIds);
        Map<Long, List<Document>> documentsByStudent = allDocuments.stream()
                .collect(Collectors.groupingBy(doc -> doc.getStudent().getId()));

        // The expired badge, read for each application's company (GH-498).
        Map<Long, Boolean> expiredByApplication =
                documentExpiryQueryService.expiredByApplication(applications, documentsByStudent);

        // Fetch staged allocations in batch
        List<Long> appIds = applications.stream().map(Application::getId).collect(Collectors.toList());
        List<StagedAllocation> allStaged = stagedAllocationRepository.findByApplication_IdIn(appIds);
        Map<Long, StagedAllocation> stagedByApp = allStaged.stream()
                .collect(Collectors.toMap(
                        s -> s.getApplication().getId(),
                        s -> s,
                        (existing, replacement) -> existing
                ));

        return applications.stream().map(app -> {
            ApplicationResponse response = applicationMapper.toResponse(app);

            // Count approved documents from the student's pack
            List<Document> studentDocs = documentsByStudent.getOrDefault(app.getStudent().getId(), new ArrayList<>());
            long approvedCount = studentDocs.stream()
                    .filter(doc -> doc.getDocumentStatus() == DocumentStatus.APPROVED)
                    .count();
            response.setApprovedDocuments((int) approvedCount);
            response.setTotalRequiredDocuments(requiredDocCount);
            response.setDocumentCompletionPercent(requiredDocCount == 0 ? 0 : (int) ((approvedCount * 100) / requiredDocCount));
            response.setHasExpiredDocuments(expiredByApplication.getOrDefault(app.getId(), false));

            // Handle Allocation
            if (app.getStatus() == ApplicationStatus.ALLOCATED) {
                response.setAllocationStatus("ALLOCATED");
            }

            StagedAllocation staged = stagedByApp.get(app.getId());
            if (staged != null) {
                response.setAllocationScore(staged.getScore());
                response.setAllocationStatus(staged.getStatus().name());
            } else {
                response.setAllocationStatus(app.getStatus().name());
            }

            return response;
        }).collect(Collectors.toList());
    }
}
