package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.EmailAlreadyRegisteredException;
import com.beezhub.student_accommodation.exceptions.UserNotFoundException;
import com.beezhub.student_accommodation.mapper.AppUserMapper;
import com.beezhub.student_accommodation.model.dto.CreateUserRequest;
import com.beezhub.student_accommodation.model.dto.UserData;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppUserMapper appUserMapper;

    @Transactional
    public UserData createUser(CreateUserRequest request) {
        log.info("Admin creating new user with role: {}", request.getUserRole());

        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyRegisteredException("Email already registered");
        }

        // Validate role is PLATFORM_ADMIN, BUILDING_OWNER, or BUILDING_STAFF (not STUDENT or deprecated roles)
        UserRole role = UserRole.enumRoleOf(request.getUserRole());
        validateAdminCreatableRole(role);

        // Create user entity
        AppUser user = appUserMapper.toEntityFromAdminRequest(
            request,
            passwordEncoder.encode(request.getPassword())
        );

        // Save and return
        AppUser savedUser = userRepository.save(user);
        log.info("User created successfully: {} with role: {}", savedUser.getEmail(), savedUser.getUserRole());

        return appUserMapper.toUserData(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserData> getAllUsers() {
        return userRepository.findAll().stream()
            .map(appUserMapper::toUserData)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserData> getUsersByRole(UserRole role) {
        return userRepository.findByUserRole(role).stream()
            .map(appUserMapper::toUserData)
            .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUser(Long userId) {
        AppUser user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Prevent deleting the last admin
        if (user.getUserRole().isAdmin()) {
            long adminCount = userRepository.countByUserRole(UserRole.PLATFORM_ADMIN);
            if (adminCount <= 1) {
                throw new IllegalStateException("Cannot delete the last admin user");
            }
        }

        userRepository.deleteById(userId);
        log.info("User deleted: {}", userId);
    }

    /**
     * Validates that the given role is admin-creatable.
     * STUDENT and deprecated roles (ADMIN, LANDLORD) are not permitted.
     *
     * @param role the role to validate
     * @throws IllegalArgumentException if the role is not admin-creatable
     */
    private void validateAdminCreatableRole(UserRole role) {
        if (role == UserRole.STUDENT || role == UserRole.ADMIN || role == UserRole.LANDLORD) {
            throw new IllegalArgumentException(
                    "Invalid role for admin creation. Allowed: PLATFORM_ADMIN, BUILDING_OWNER, BUILDING_STAFF");
        }
    }
}
