package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.AcademicYearNotFoundException;
import com.beezhub.student_accommodation.exceptions.DuplicateResourceException;
import com.beezhub.student_accommodation.mapper.AcademicYearMapper;
import com.beezhub.student_accommodation.model.dto.AcademicYearRequest;
import com.beezhub.student_accommodation.model.dto.AcademicYearResponse;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.repository.AcademicYearRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final ApplicationRepository applicationRepository;
    private final AcademicYearMapper academicYearMapper;
    private final com.beezhub.student_accommodation.service.tenant.TenantOwnershipGuard tenantOwnershipGuard;

    @Transactional
    public AcademicYearResponse createAcademicYear(@Valid AcademicYearRequest request) {
        log.info("Creating academic year: {}", request.getName());

        if (academicYearRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Academic year with name '" + request.getName() + "' already exists");
        }

        AcademicYear academicYear = academicYearMapper.toEntity(request);

        // If this is marked as current, clear any existing current year
        if (Boolean.TRUE.equals(request.getIsCurrent())) {
            academicYearRepository.clearCurrentAcademicYear();
        }

        AcademicYear saved = academicYearRepository.save(academicYear);
        log.info("Created academic year with ID: {}", saved.getId());

        return academicYearMapper.toResponse(saved);
    }

    @Transactional
    public AcademicYearResponse updateAcademicYear(Long id, @Valid AcademicYearRequest request) {
        log.info("Updating academic year ID: {}", id);

        AcademicYear academicYear = academicYearRepository.findById(id)
                .orElseThrow(() -> new AcademicYearNotFoundException("Academic year not found with ID: " + id));
        tenantOwnershipGuard.assertOwned(academicYear, "Academic year", id);

        // Check for duplicate name (exclude current record)
        if (!academicYear.getName().equals(request.getName())
                && academicYearRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Academic year with name '" + request.getName() + "' already exists");
        }

        // If setting as current, clear any existing current year
        if (Boolean.TRUE.equals(request.getIsCurrent()) && !Boolean.TRUE.equals(academicYear.getIsCurrent())) {
            academicYearRepository.clearCurrentAcademicYear();
        }

        academicYearMapper.updateEntity(request, academicYear);
        AcademicYear saved = academicYearRepository.save(academicYear);

        log.info("Updated academic year ID: {}", saved.getId());
        return academicYearMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AcademicYearResponse getAcademicYearById(Long id) {
        AcademicYear academicYear = academicYearRepository.findById(id)
                .orElseThrow(() -> new AcademicYearNotFoundException("Academic year not found with ID: " + id));
        tenantOwnershipGuard.assertReadable(academicYear, "Academic year", id);
        return academicYearMapper.toResponse(academicYear);
    }

    @Transactional(readOnly = true)
    public List<AcademicYearResponse> getAllAcademicYears() {
        Long tenantId = TenantContext.getTenantId();
        List<AcademicYear> years = tenantId == null || TenantContext.PLATFORM_TENANT_ID.equals(tenantId)
                ? academicYearRepository.findAll()
                : academicYearRepository.findVisibleToTenant(tenantId);
        return years.stream()
                .map(academicYearMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AcademicYearResponse getCurrentAcademicYear() {
        AcademicYear academicYear = academicYearRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new AcademicYearNotFoundException("No current academic year is set"));
        return academicYearMapper.toResponse(academicYear);
    }

    @Transactional(readOnly = true)
    public AcademicYear getCurrentAcademicYearEntity() {
        return academicYearRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new AcademicYearNotFoundException("No current academic year is set"));
    }

    @Transactional
    public AcademicYearResponse setCurrentAcademicYear(Long id) {
        log.info("Setting academic year ID {} as current", id);

        AcademicYear academicYear = academicYearRepository.findById(id)
                .orElseThrow(() -> new AcademicYearNotFoundException("Academic year not found with ID: " + id));
        tenantOwnershipGuard.assertOwned(academicYear, "Academic year", id);

        // Clear any existing current year
        academicYearRepository.clearCurrentAcademicYear();

        // Set this one as current
        academicYear.setIsCurrent(true);
        AcademicYear saved = academicYearRepository.save(academicYear);

        log.info("Academic year ID {} is now current", saved.getId());
        return academicYearMapper.toResponse(saved);
    }

    @Transactional
    public void deleteAcademicYear(Long id) {
        log.info("Deleting academic year ID: {}", id);

        AcademicYear academicYear = academicYearRepository.findById(id)
                .orElseThrow(() -> new AcademicYearNotFoundException("Academic year not found with ID: " + id));
        tenantOwnershipGuard.assertOwned(academicYear, "Academic year", id);

        // Check if any applications are linked to this academic year
        if (applicationRepository.existsByAcademicYear_Id(id)) {
            throw new IllegalStateException("Cannot delete academic year with linked applications");
        }

        academicYearRepository.delete(academicYear);
        log.info("Deleted academic year ID: {}", id);
    }

    /**
     * Checks if the application window is currently open for the given academic
     * year.
     */
    public boolean isApplicationWindowOpen(AcademicYear academicYear) {
        if (academicYear == null || academicYear.getApplicationOpenDate() == null
                || academicYear.getApplicationCloseDate() == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return !today.isBefore(academicYear.getApplicationOpenDate())
                && !today.isAfter(academicYear.getApplicationCloseDate());
    }
}
