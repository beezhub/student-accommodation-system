package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.model.dto.ApplicationRequest;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AcademicYearRepository;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private AcademicYearRepository academicYearRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private AppUserRepository appUserRepository;

    private Student student;

    @BeforeEach
    void setUp() {
        // Clean in FK-safe order
        applicationRepository.deleteAll();
        academicYearRepository.deleteAll();
        studentRepository.deleteAll();
        appUserRepository.deleteAll();

        // Seed a current AcademicYear with an open application window.
        // Uses the PLATFORM sentinel tenant (id=0), which is always present
        // after Flyway migration V2026_02_25_0013__platform_tenant_seed.sql runs.
        AcademicYear year = AcademicYear.builder()
                .name("2026 Integration")
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .applicationOpenDate(LocalDate.of(2026, 1, 1))
                .applicationCloseDate(LocalDate.of(2026, 12, 31))
                .isCurrent(true)
                .tenantId(TenantContext.PLATFORM_TENANT_ID)
                .build();
        academicYearRepository.save(year);

        // Seed a student user — students are PLATFORM-scoped (tenant_id = 0)
        AppUser user = new AppUser();
        user.setEmail("pref-test@example.com");
        user.setUserPassword("secret");
        user.setFirstName("Pref");
        user.setLastName("Tester");
        user.setUserRole(UserRole.STUDENT);
        user.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        appUserRepository.save(user);

        student = new Student();
        student.setAppUser(user);
        student.setStudentNumber("S-PREF-001");
        student = studentRepository.save(student);

        // Set up Spring Security context so authService.getCurrentUser() resolves
        // the authenticated principal by email from the SecurityContextHolder
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of()));

        // Set TenantContext so TenantListener populates tenant_id on @PrePersist
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("Application is persisted with preferences when submitted")
    void submitApplication_persistsPreferences() {
        // Arrange
        ApplicationRequest request = new ApplicationRequest();
        request.setStudentId(student.getId());
        request.setSpecialRequirements("Near elevator");
        request.setInstitutionName("Test Institute");
        request.setCourseName("CS");
        request.setRequiresAccessibility(false);

        // Act
        ApplicationResponse response = applicationService.submitApplication(request);

        // Assert
        assertThat(response.getSpecialRequirements()).isEqualTo("Near elevator");
        assertThat(response.getInstitutionName()).isEqualTo("Test Institute");
        assertThat(response.getCourseName()).isEqualTo("CS");
        assertThat(response.getRequiresAccessibility()).isEqualTo(false);
    }
}
