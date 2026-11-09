package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Institution;
import com.beezhub.student_accommodation.model.entity.YearOfStudy;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.InstitutionRepository;
import com.beezhub.student_accommodation.repository.YearOfStudyRepository;
import com.beezhub.student_accommodation.security.jwt.JwtUtil;
import com.beezhub.student_accommodation.security.user.AppUserDetails;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Who may create a student profile, and for whom (GH-489).
 *
 * <p>The endpoint saved whatever {@code userId} the body named. A student could put a profile on
 * another student's account, and because {@code student.user_id} has no unique constraint, the
 * account then held two rows.
 */
@DisplayName("Student profile creation access (integration)")
class StudentProfileCreationAccessIntegrationTest extends AbstractIntegrationTest {

    private static final String ROUTE = "/api/student";

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtUtil jwtUtil;
    @Autowired AppUserRepository appUserRepository;
    @Autowired InstitutionRepository institutionRepository;
    @Autowired YearOfStudyRepository yearOfStudyRepository;

    private final List<Long> userIds = new ArrayList<>();
    private Long institutionId;
    private Long yearOfStudyId;
    private AppUser student;
    private AppUser victim;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);

        Institution institution = new Institution();
        institution.setName("Institution " + System.nanoTime());
        institutionId = institutionRepository.save(institution).getId();

        YearOfStudy year = new YearOfStudy();
        year.setYearOfStudy("Y" + (System.nanoTime() % 100000));
        yearOfStudyId = yearOfStudyRepository.save(year).getId();

        student = saveUser(UserRole.STUDENT);
        victim = saveUser(UserRole.STUDENT);
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        String users = userIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        jdbc.update("DELETE FROM student WHERE user_id IN (" + users + ")");
        jdbc.update("DELETE FROM app_user WHERE id IN (" + users + ")");
        jdbc.update("DELETE FROM institution WHERE id = ?", institutionId);
        jdbc.update("DELETE FROM year_of_study WHERE id = ?", yearOfStudyId);
    }

    @Test
    void createStudent_studentForThemselves_createsProfileOnTheirAccount() {
        ResponseEntity<String> response = post(body(student.getId()), tokenFor(student));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(profileCount(student)).isEqualTo(1);
    }

    @Test
    void createStudent_studentNamingNoUser_createsProfileOnTheirAccount() {
        ResponseEntity<String> response = post(body(null), tokenFor(student));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(profileCount(student)).isEqualTo(1);
    }

    @Test
    void createStudent_studentNamingAnotherUser_returnsForbiddenAndCreatesNothing() {
        ResponseEntity<String> response = post(body(victim.getId()), tokenFor(student));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(profileCount(victim)).isZero();
        assertThat(profileCount(student)).isZero();
    }

    @Test
    void createStudent_userAlreadyHasProfile_returnsConflictAndKeepsOneRow() {
        assertThat(post(body(student.getId()), tokenFor(student)).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> second = post(body(student.getId()), tokenFor(student));

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(profileCount(student)).isEqualTo(1);
    }

    @Test
    void createStudent_platformAdminForAStudent_createsProfileOnThatAccount() {
        ResponseEntity<String> response = post(body(victim.getId()), tokenFor(platformAdmin()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(profileCount(victim)).isEqualTo(1);
    }

    @Test
    void createStudent_platformAdminForNonStudentUser_isRefusedAndCreatesNothing() {
        AppUser admin = platformAdmin();

        ResponseEntity<String> response = post(body(admin.getId()), tokenFor(admin));

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(profileCount(admin)).isZero();
    }

    @Test
    void createStudent_platformAdminNamingNoUser_isRefused() {
        AppUser admin = platformAdmin();

        ResponseEntity<String> response = post(body(null), tokenFor(admin));

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(profileCount(admin)).isZero();
    }

    @Test
    void createStudent_platformAdminForUnknownUser_returnsNotFound() {
        ResponseEntity<String> response = post(body(Long.MAX_VALUE), tokenFor(platformAdmin()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private AppUser platformAdmin() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        AppUser admin = saveUser(UserRole.PLATFORM_ADMIN);
        TenantContext.clear();
        return admin;
    }

    private ResponseEntity<String> post(String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(ROUTE, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private String body(Long userId) {
        String user = userId == null ? "" : "\"userId\":" + userId + ",";
        return "{" + user + "\"studentNumber\":\"S" + (System.nanoTime() % 100000000L) + "\","
                + "\"phoneNumber\":\"0123456789\",\"institutionId\":" + institutionId + ","
                + "\"yearOfStudyId\":" + yearOfStudyId + ","
                + "\"firstName\":\"Test\",\"lastName\":\"User\",\"email\":\"profile@test.com\"}";
    }

    private int profileCount(AppUser user) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM student WHERE user_id = ?", Integer.class, user.getId());
        return count == null ? 0 : count;
    }

    private String tokenFor(AppUser user) {
        return jwtUtil.generateToken(new AppUserDetails(user.getEmail(), "", user.getUserRole().name(), user.getTenantId()));
    }

    private AppUser saveUser(UserRole role) {
        AppUser user = appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "." + System.nanoTime() + "@test.com")
                .firstName("Test")
                .lastName("User")
                .userPassword("$2a$10$hashed")
                .userRole(role)
                .tenantId(TenantContext.PLATFORM_TENANT_ID)
                .emailVerified(true)
                .mustChangePassword(false)
                .build());
        userIds.add(user.getId());
        return user;
    }
}
