package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.EmailAlreadyRegisteredException;
import com.beezhub.student_accommodation.mapper.AppUserMapper;
import com.beezhub.student_accommodation.model.dto.CreateUserRequest;
import com.beezhub.student_accommodation.model.dto.UserData;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.beezhub.student_accommodation.exceptions.UserNotFoundException;

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
    private PasswordEncoder passwordEncoder;

    @Mock
    private AppUserMapper appUserMapper;

    @InjectMocks
    private UserManagementService userManagementService;

    private CreateUserRequest buildRequest(String role) {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("test@example.com");
        req.setPassword("password123");
        req.setFirstName("Test");
        req.setLastName("User");
        req.setUserRole(role);
        return req;
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

    @Test
    @DisplayName("Creating a BUILDING_STAFF user succeeds")
    void createUser_buildingStaff_succeeds() {
        stubPasswordEncoder();
        AppUser savedUser = AppUser.builder()
                .email("test@example.com")
                .userRole(UserRole.BUILDING_STAFF)
                .build();
        when(appUserMapper.toEntityFromAdminRequest(any(), anyString())).thenReturn(savedUser);
        when(userRepository.save(any())).thenReturn(savedUser);
        when(appUserMapper.toUserData(any())).thenReturn(new UserData());

        UserData result = userManagementService.createUser(buildRequest("Building_Staff"));
        assertThat(result).isNotNull();
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
    @DisplayName("getAllUsers returns mapped list of users")
    void getAllUsers_returnsMappedList() {
        AppUser u1 = AppUser.builder().id(1L).email("a@a.com").userRole(UserRole.STUDENT).build();
        AppUser u2 = AppUser.builder().id(2L).email("b@b.com").userRole(UserRole.PLATFORM_ADMIN).build();
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));
        when(appUserMapper.toUserData(any())).thenReturn(new UserData());

        List<UserData> result = userManagementService.getAllUsers();

        assertThat(result).hasSize(2);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("getUsersByRole returns users with the given role")
    void getUsersByRole_returnsMappedList() {
        AppUser admin = AppUser.builder().id(1L).email("admin@a.com").userRole(UserRole.PLATFORM_ADMIN).build();
        when(userRepository.findByUserRole(UserRole.PLATFORM_ADMIN)).thenReturn(List.of(admin));
        when(appUserMapper.toUserData(any())).thenReturn(new UserData());

        List<UserData> result = userManagementService.getUsersByRole(UserRole.PLATFORM_ADMIN);

        assertThat(result).hasSize(1);
        verify(userRepository).findByUserRole(UserRole.PLATFORM_ADMIN);
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
