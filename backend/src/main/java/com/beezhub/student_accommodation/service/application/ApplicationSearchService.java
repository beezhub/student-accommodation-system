package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.ApplicationDocument;
import com.beezhub.student_accommodation.model.entity.StagedAllocation;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.entity.ApplicationDocument;
import com.beezhub.student_accommodation.repository.ApplicationDocumentRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.repository.StagedAllocationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
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
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final StagedAllocationRepository stagedAllocationRepository;
    private final ApplicationMapper applicationMapper;
    private final DocumentTypeRepository documentTypeRepository;

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

        // Enrich responses with extra data
        List<ApplicationResponse> responses = enrichApplicationDetails(page.getContent());

        Page<ApplicationResponse> responsePage = new org.springframework.data.domain.PageImpl<>(responses,
                pageable,
                page.getTotalElements());
        return PageMapper.toPageResponse(responsePage);
    }

    /**
     * Searches applications by filters with pagination.
     * Admin only.
     *
     * @param applicationCode   Application code (partial match)
     * @param studentName       Student name (partial match)
     * @param status            Application status
     * @param allocationStatus  Allocation status
     * @param preferredBuilding Preferred building name
     * @param startDate         Application date from
     * @param endDate           Application date to
     * @param pageable          Pagination parameters
     * @return PageResponse with filtered applications
     */
    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> searchApplications(String applicationCode, String studentName,
            ApplicationStatus status, String allocationStatus, String preferredBuilding,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {
        log.info(
                "Searching applications with filters - code: {}, student: {}, status: {}, allocationStatus: {}, building: {}, startDate: {}, endDate: {}",
                applicationCode, studentName, status, allocationStatus, preferredBuilding, startDate,
                endDate);
        Specification<Application> spec = ApplicationSpecification.filterBy(applicationCode, studentName,
                status,
                allocationStatus, preferredBuilding, startDate, endDate);
        Page<Application> page = applicationRepository.findAll(spec, pageable);

        // Enrich responses with extra data not in Application entity
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
     *
     * @param status Application status string
     * @return List of applications with the given status
     */
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByStatus(String status) {
        log.info("Retrieving applications for status {}", status);
        ApplicationStatus applicationStatus = ApplicationStatus.valueOf(status.toUpperCase());
        return applicationRepository.findByStatus(applicationStatus)
                .stream()
                .map(applicationMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Enriches application responses with document and allocation details.
     * Fetches related data in batch to avoid N+1 queries.
     *
     * @param applications List of applications to enrich
     * @return List of enriched application responses
     */
    private List<ApplicationResponse> enrichApplicationDetails(List<Application> applications) {
        if (applications.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> appIds = applications.stream().map(Application::getId).collect(Collectors.toList());

        // Fetch actual required document type count once for all applications
        int requiredDocCount = documentTypeRepository.findByIsRequiredTrue().size();

        // Fetch junction entries in batch; group documents by application ID
        List<ApplicationDocument> allJunctionEntries = applicationDocumentRepository.findByApplication_IdIn(appIds);
        Map<Long, List<ApplicationDocument>> entriesByApp = allJunctionEntries.stream()
                .collect(Collectors.groupingBy(e -> e.getApplication().getId()));

        // Fetch staged allocations in batch
        List<StagedAllocation> allStaged = stagedAllocationRepository.findByApplication_IdIn(appIds);
        Map<Long, StagedAllocation> stagedByApp = allStaged.stream()
                .collect(Collectors.toMap(
                        s -> s.getApplication().getId(),
                        s -> s,
                        (existing, replacement) -> existing // Keep first if multiple (rare)
                ));

        return applications.stream().map(app -> {
            ApplicationResponse response = applicationMapper.toResponse(app);

            // Handle Documents via junction table
            List<ApplicationDocument> entries = entriesByApp.getOrDefault(app.getId(), new ArrayList<>());
            long approvedCount = entries.stream()
                    .filter(e -> e.getDocument().getDocumentStatus() == DocumentStatus.APPROVED)
                    .count();
            response.setApprovedDocuments((int) approvedCount);
            response.setTotalRequiredDocuments(requiredDocCount);
            response.setDocumentCompletionPercent(requiredDocCount == 0 ? 0 : (int) ((approvedCount * 100) / requiredDocCount));

            // Handle Allocation
            if (app.getStatus() == ApplicationStatus.ALLOCATED) {
                response.setAllocationStatus("ALLOCATED");
            }

            StagedAllocation staged = stagedByApp.get(app.getId());
            if (staged != null) {
                response.setAllocationScore(staged.getScore());
                // If application is AWAITING_ALLOCATION, staged status is meaningful
                response.setAllocationStatus(staged.getStatus().name());
            } else {
                response.setAllocationStatus(app.getStatus().name()); // Default to app status
            }

            return response;
        }).collect(Collectors.toList());
    }
}
