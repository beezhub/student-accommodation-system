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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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
    @DisplayName("Creating a BUILDING_OWNER user succeeds")
    void createUser_buildingOwner_succeeds() {
        stubPasswordEncoder();
        AppUser savedUser = AppUser.builder()
                .email("test@example.com")
                .userRole(UserRole.BUILDING_OWNER)
                .build();
        when(appUserMapper.toEntityFromAdminRequest(any(), anyString())).thenReturn(savedUser);
        when(userRepository.save(any())).thenReturn(savedUser);
        when(appUserMapper.toUserData(any())).thenReturn(new UserData());

        UserData result = userManagementService.createUser(buildRequest("Building_Owner"));
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
}
