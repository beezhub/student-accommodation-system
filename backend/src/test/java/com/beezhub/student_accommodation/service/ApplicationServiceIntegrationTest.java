package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.ApplicationRequest;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ApplicationServiceIntegrationTest {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private AppUserRepository appUserRepository;

    private Student student;

    @BeforeEach
    void setup() {
        AppUser user = new AppUser();
        user.setEmail("pref-test@example.com");
        user.setUserPassword("secret");
        user.setFirstName("Pref");
        user.setLastName("Tester");
        user.setUserRole(UserRole.STUDENT);
        appUserRepository.save(user);

        student = new Student();
        student.setAppUser(user);
        student.setStudentNumber("S-PREF-001");
        studentRepository.save(student);
    }

    @Test
    void submitApplication_persistsPreferences() {
        ApplicationRequest request = new ApplicationRequest();
        request.setStudentId(student.getId());
        request.setPreferredAccommodationType("HOSTEL");
        request.setPreferredMoveInDate(LocalDate.of(2026, 2, 1));
        request.setPreferredDurationMonths(12);
        request.setPreferredRoomType("DOUBLE");
        request.setSpecialRequirements("Near elevator");
        request.setInstitutionName("Test Institute");
        request.setCourseName("CS");

        ApplicationResponse response = applicationService.submitApplication(request);

        assertThat(response.getPreferredAccommodationType()).isEqualTo("HOSTEL");
        assertThat(response.getPreferredMoveInDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(response.getPreferredDurationMonths()).isEqualTo(12);
        assertThat(response.getPreferredRoomType()).isEqualTo("DOUBLE");
        assertThat(response.getSpecialRequirements()).isEqualTo("Near elevator");
        assertThat(response.getInstitutionName()).isEqualTo("Test Institute");
        assertThat(response.getCourseName()).isEqualTo("CS");
    }
}
