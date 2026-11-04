package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.EmailAlreadyRegisteredException;
import com.beezhub.student_accommodation.mapper.AppUserMapper;
import com.beezhub.student_accommodation.model.dto.CreateUserRequest;
import com.beezhub.student_accommodation.model.dto.UserData;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.exceptions.InactiveTenantException;
import com.beezhub.student_accommodation.exceptions.TenantNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.beezhub.student_accommodation.exceptions.UserNotFoundException;

import com.beezhub.student_accommodation.service.tenant.TenantContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserManagementService role validation tests")
class UserManagementServiceRoleValidationTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private com.beezhub.student_accommodation.service.tenant.TenantService tenantService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AppUserMapper appUserMapper;

    @InjectMocks
    private UserManagementService userManagementService;

    private CreateUserRequest buildRequest(String role) {
        return buildRequest(role, null);
    }

    private CreateUserRequest buildRequest(String role, Long tenantId) {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("test@example.com");
        req.setPassword("password123");
        req.setFirstName("Test");
        req.setLastName("User");
        req.setUserRole(role);
        req.setTenantId(tenantId);
        return req;
    }

    /** Captures the entity handed to save, which is where the tenant stamp lands. */
    private AppUser createAndCapture(CreateUserRequest request) {
        userManagementService.createUser(request);
        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(saved.capture());
        return saved.getValue();
    }

    @BeforeEach
    void setUp() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    }

    private void stubPasswordEncoder() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    }

    @Test
    @DisplayName("Creating a PLATFORM_ADMIN user succeeds")
    void createUser_platformAdmin_succeeds() {
        stubPasswordEncoder();
        AppUser savedUser = AppUser.builder()
                .email("test@example.com")
                .userRole(UserRole.PLATFORM_ADMIN)
                .build();
        when(appUserMapper.toEntityFromAdminRequest(any(), anyString())).thenReturn(savedUser);
        when(userRepository.save(any())).thenReturn(savedUser);
        when(appUserMapper.toUserData(any())).thenReturn(new UserData());

        UserData result = userManagementService.createUser(buildRequest("Platform_Admin"));
        assertThat(result).isNotNull();
    }

    /** Stubs the mapper and repository for a successful create of the given role. */
    private void stubSuccessfulCreate(UserRole role) {
        stubPasswordEncoder();
        AppUser savedUser = AppUser.builder()
                .email("test@example.com")
                .userRole(role)
                .build();
        when(appUserMapper.toEntityFromAdminRequest(any(), anyString())).thenReturn(savedUser);
        when(userRepository.save(any())).thenReturn(savedUser);
        when(appUserMapper.toUserData(any())).thenReturn(new UserData());
    }

    @Test
    @DisplayName("Creating a BUILDING_STAFF user with a tenant succeeds")
    void createUser_buildingStaffWithTenant_succeeds() {
        stubSuccessfulCreate(UserRole.BUILDING_STAFF);

        UserData result = userManagementService.createUser(buildRequest("Building_Staff", 7L));
        assertThat(result).isNotNull();
    }

    /**
     * GH-146: the tenant used to fall back to the acting context, so a platform admin creating a
     * tenant-scoped account stamped it with tenant 0. That sentinel disables the Hibernate tenant
     * filter and makes TenantOwnershipGuard wave every check through, so the new account silently
     * became a platform-wide reader. There is no safe default, so there is no default.
     */
    @Test
    @DisplayName("Creating a BUILDING_STAFF user without a tenant is rejected")
    void createUser_buildingStaffWithoutTenant_throws() {
        assertThatThrownBy(() -> userManagementService.createUser(buildRequest("Building_Staff")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scoped to a specific institution tenant");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Creating a BUILDING_STAFF user with the platform tenant is rejected")
    void createUser_buildingStaffWithPlatformTenant_throws() {
        assertThatThrownBy(() -> userManagementService.createUser(buildRequest("Building_Staff", 0L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scoped to a specific institution tenant");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("A tenant-scoped account is stamped with the requested tenant")
    void createUser_buildingStaffWithTenant_stampsThatTenant() {
        stubSuccessfulCreate(UserRole.BUILDING_STAFF);

        assertThat(createAndCapture(buildRequest("Building_Staff", 7L)).getTenantId()).isEqualTo(7L);
    }

    /**
     * #413: users are created only here, and a company's admin screen picks an existing one. A
     * tenant admin with no company yet waits on the platform tenant, locked exactly as
     * V2026_09_06_1500 locks such accounts, because tenant 0 would otherwise open every company's
     * data to it (GH-212). Assigning a company unlocks it (UnassignedAdminService.assignTenant).
     */
    @Test
    @DisplayName("A TENANT_ADMIN created without a company waits locked on the platform tenant")
    void createUser_tenantAdminWithoutCompany_waitsLockedOnThePlatformTenant() {
        stubSuccessfulCreate(UserRole.TENANT_ADMIN);

        AppUser saved = createAndCapture(buildRequest("Tenant_Admin"));

        assertThat(saved.getTenantId()).isEqualTo(TenantContext.PLATFORM_TENANT_ID);
        assertThat(saved.getAccountLockedUntil()).isEqualTo(LocalDateTime.of(9999, 12, 31, 23, 59, 59));
        verify(tenantService, never()).requireTenantAcceptingNewRecords(any());
    }

    @Test
    @DisplayName("A TENANT_ADMIN created with a company belongs to it and can sign in")
    void createUser_tenantAdminWithCompany_belongsToItUnlocked() {
        stubSuccessfulCreate(UserRole.TENANT_ADMIN);

        AppUser saved = createAndCapture(buildRequest("Tenant_Admin", 7L));

        assertThat(saved.getTenantId()).isEqualTo(7L);
        assertThat(saved.getAccountLockedUntil()).isNull();
    }

    /** Only "no company" waits. Naming the platform tenant outright is the GH-146 mistake. */
    @Test
    @DisplayName("A TENANT_ADMIN with the platform tenant named outright is still rejected")
    void createUser_tenantAdminWithPlatformTenant_throws() {
        assertThatThrownBy(() -> userManagementService.createUser(buildRequest("Tenant_Admin", 0L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scoped to a specific institution tenant");
        verify(userRepository, never()).save(any());
    }

    /**
     * tenant_id is NOT NULL behind a foreign key, and the request is now the only source of the
     * value, so an unknown id would otherwise surface as a 500 rather than a not-found.
     */
    @Test
    @DisplayName("Creating a user for an unknown tenant is rejected")
    void createUser_unknownTenant_throws() {
        stubPasswordEncoder();
        when(tenantService.requireTenantAcceptingNewRecords(99L))
                .thenThrow(new TenantNotFoundException("Property company not found with id: 99"));

        assertThatThrownBy(() -> userManagementService.createUser(buildRequest("Building_Staff", 99L)))
                .isInstanceOf(TenantNotFoundException.class);
        verify(userRepository, never()).save(any());
    }

    /**
     * This route created staff against a switched-off company while TenantAdminService, the other
     * route to the same thing, refused it. The two disagreed about whether it was allowed at all
     * (GH-267).
     */
    @Test
    @DisplayName("Creating a user for a deactivated tenant is rejected")
    void createUser_deactivatedTenant_throws() {
        stubPasswordEncoder();
        when(tenantService.requireTenantAcceptingNewRecords(7L))
                .thenThrow(new InactiveTenantException("Property company 'Gone Ltd' is deactivated"));

        assertThatThrownBy(() -> userManagementService.createUser(buildRequest("Building_Staff", 7L)))
                .isInstanceOf(InactiveTenantException.class);
        verify(userRepository, never()).save(any());
    }

    /**
     * The mirror image: 0 is what gives a platform admin platform-wide visibility, so one stamped
     * with a real tenant would quietly lose it.
     */
    @Test
    @DisplayName("A platform admin is stamped with the platform tenant")
    void createUser_platformAdmin_stampsThePlatformTenant() {
        stubSuccessfulCreate(UserRole.PLATFORM_ADMIN);

        assertThat(createAndCapture(buildRequest("Platform_Admin")).getTenantId()).isZero();
    }

    @Test
    @DisplayName("Creating a platform admin scoped to a real tenant is rejected")
    void createUser_platformAdminWithTenant_throws() {
        assertThatThrownBy(() -> userManagementService.createUser(buildRequest("Platform_Admin", 7L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not scoped to a tenant");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Creating a STUDENT user throws IllegalArgumentException")
    void createUser_student_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> userManagementService.createUser(buildRequest("Student")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid role for admin creation");
    }

    @Test
    @DisplayName("Creating user with unknown role throws IllegalArgumentException")
    void createUser_unknownRole_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> userManagementService.createUser(buildRequest("SUPERUSER")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Creating user with duplicate email throws EmailAlreadyRegisteredException")
    void createUser_duplicateEmail_throwsEmailAlreadyRegisteredException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(new AppUser()));

        assertThatThrownBy(() -> userManagementService.createUser(buildRequest("Platform_Admin")))
                .isInstanceOf(com.beezhub.student_accommodation.exceptions.EmailAlreadyRegisteredException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAllUsers returns one page of mapped users")
    void getAllUsers_returnsMappedPage() {
        AppUser u1 = AppUser.builder().id(1L).email("a@a.com").userRole(UserRole.STUDENT).build();
        AppUser u2 = AppUser.builder().id(2L).email("b@b.com").userRole(UserRole.PLATFORM_ADMIN).build();
        Pageable pageable = PageRequest.of(0, 25);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(u1, u2), pageable, 2));
        when(appUserMapper.toUserData(any())).thenReturn(new UserData());

        Page<UserData> result = userManagementService.getAllUsers(pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(userRepository).findAll(pageable);
    }

    /**
     * The unpaged findAll is no longer reachable from this path: it loaded and mapped every
     * account in the deployment on each visit to the admin screen (GH-157).
     */
    @Test
    @DisplayName("getAllUsers never falls back to an unpaged findAll")
    void getAllUsers_doesNotLoadEveryUser() {
        Pageable pageable = PageRequest.of(0, 25);
        when(userRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        userManagementService.getAllUsers(pageable);

        verify(userRepository, never()).findAll();
    }

    @Test
    @DisplayName("getUsersByRole returns one page of users with the given role")
    void getUsersByRole_returnsMappedPage() {
        AppUser admin = AppUser.builder().id(1L).email("admin@a.com").userRole(UserRole.PLATFORM_ADMIN).build();
        Pageable pageable = PageRequest.of(0, 25);
        when(userRepository.findByUserRole(UserRole.PLATFORM_ADMIN, pageable))
                .thenReturn(new PageImpl<>(List.of(admin), pageable, 1));
        when(appUserMapper.toUserData(any())).thenReturn(new UserData());

        Page<UserData> result = userManagementService.getUsersByRole(UserRole.PLATFORM_ADMIN, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findByUserRole(UserRole.PLATFORM_ADMIN, pageable);
    }

    @Test
    @DisplayName("deleteUser throws UserNotFoundException when user does not exist")
    void deleteUser_notFound_throwsUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementService.deleteUser(99L))
                .isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteUser deletes non-admin user without counting admins")
    void deleteUser_nonAdminUser_deletesSuccessfully() {
        AppUser student = AppUser.builder().id(5L).email("s@s.com").userRole(UserRole.STUDENT).build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(student));

        userManagementService.deleteUser(5L);

        verify(userRepository).deleteById(5L);
        verify(userRepository, never()).countByUserRole(any());
    }

    @Test
    @DisplayName("deleteUser deletes admin when more than one admin exists")
    void deleteUser_adminWithOtherAdmins_deletesSuccessfully() {
        AppUser admin = AppUser.builder().id(3L).email("admin@a.com").userRole(UserRole.PLATFORM_ADMIN).build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(admin));
        when(userRepository.countByUserRole(UserRole.PLATFORM_ADMIN)).thenReturn(2L);

        userManagementService.deleteUser(3L);

        verify(userRepository).deleteById(3L);
    }

    @Test
    @DisplayName("deleteUser throws IllegalStateException when deleting the last admin")
    void deleteUser_lastAdmin_throwsIllegalStateException() {
        AppUser admin = AppUser.builder().id(3L).email("admin@a.com").userRole(UserRole.PLATFORM_ADMIN).build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(admin));
        when(userRepository.countByUserRole(UserRole.PLATFORM_ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userManagementService.deleteUser(3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("last admin");
        verify(userRepository, never()).deleteById(any());
    }
}
