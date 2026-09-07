package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.EmailAlreadyRegisteredException;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.exceptions.UserNotFoundException;
import com.beezhub.student_accommodation.mapper.AppUserMapper;
import com.beezhub.student_accommodation.model.dto.CreateUserRequest;
import com.beezhub.student_accommodation.model.dto.UserData;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

    private final AppUserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppUserMapper appUserMapper;

    @Transactional
    public UserData createUser(CreateUserRequest request) {
        log.info("Admin creating new user with role: {}", request.getUserRole());

        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyRegisteredException("Email already registered");
        }

        // Validate role is admin-creatable
        UserRole role = UserRole.enumRoleOf(request.getUserRole());
        validateAdminCreatableRole(role, request.getTenantId());

        // Create user entity
        AppUser user = appUserMapper.toEntityFromAdminRequest(
            request,
            passwordEncoder.encode(request.getPassword())
        );

        user.setTenantId(resolveTenantForRole(role, request.getTenantId()));

        // Save and return
        AppUser savedUser = userRepository.save(user);
        log.info("User created successfully: {} with role: {}", savedUser.getEmail(), savedUser.getUserRole());

        return appUserMapper.toUserData(savedUser);
    }

    /**
     * One page of users.
     *
     * <p>Was an unpaged {@code findAll}, which loaded and mapped every account in the deployment
     * on each visit to the admin screen and grew without limit (GH-157). Every other list endpoint
     * here is paginated; this one was the exception.
     */
    @Transactional(readOnly = true)
    public Page<UserData> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(appUserMapper::toUserData);
    }

    @Transactional(readOnly = true)
    public Page<UserData> getUsersByRole(UserRole role, Pageable pageable) {
        return userRepository.findByUserRole(role, pageable).map(appUserMapper::toUserData);
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
     * Validates that the given role is admin-creatable and that tenant scoping rules are met.
     *
     * <ul>
     *   <li>{@code STUDENT} — never admin-creatable</li>
     *   <li>{@code ADMIN} — allowed, but must be scoped to a non-platform tenant (tenantId != 0)</li>
     *   <li>{@code PLATFORM_ADMIN}, {@code BUILDING_STAFF} — no tenant restriction</li>
     * </ul>
     */
    private void validateAdminCreatableRole(UserRole role, Long tenantId) {
        if (role == UserRole.STUDENT) {
            throw new IllegalArgumentException(
                    "Invalid role for admin creation. Allowed: PLATFORM_ADMIN, ADMIN, BUILDING_STAFF");
        }
    }

    /**
     * The tenant a new account is scoped to.
     *
     * <p>This used to fall back to the acting context when the request carried no tenant. A
     * platform admin creating a tenant admin therefore stamped the new account with tenant 0,
     * and 0 is the sentinel that turns the Hibernate tenant filter off and makes
     * TenantOwnershipGuard wave every check through. A tenant-scoped account silently became a
     * platform-wide reader (GH-146). There is no safe default here, so there is no default.
     *
     * <p>Platform admins are the mirror image: they must be tenant 0, because that sentinel is
     * what gives them platform-wide visibility. A platform admin stamped with a real tenant would
     * quietly lose it.
     */
    private Long resolveTenantForRole(UserRole role, Long requestedTenantId) {
        if (role == UserRole.PLATFORM_ADMIN) {
            if (requestedTenantId != null && !TenantContext.PLATFORM_TENANT_ID.equals(requestedTenantId)) {
                throw new IllegalArgumentException(
                        "Platform admins are not scoped to a tenant");
            }
            return TenantContext.PLATFORM_TENANT_ID;
        }

        if (requestedTenantId == null || TenantContext.PLATFORM_TENANT_ID.equals(requestedTenantId)) {
            throw new IllegalArgumentException(
                    "Tenant admins and building staff must be scoped to a specific institution tenant");
        }
        // tenant_id is NOT NULL behind a foreign key, and this is now the only source of the
        // value, so an unknown id would surface as a 500 rather than a 404.
        if (!tenantRepository.existsById(requestedTenantId)) {
            throw new ResourceNotFoundException("Tenant not found with id: " + requestedTenantId);
        }
        return requestedTenantId;
    }
}
