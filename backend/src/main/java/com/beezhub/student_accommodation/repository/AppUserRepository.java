package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);

    List<AppUser> findByUserRole(UserRole role);

    /** Paged variant; the admin user list is unbounded otherwise (GH-157). */
    org.springframework.data.domain.Page<AppUser> findByUserRole(
            UserRole role, org.springframework.data.domain.Pageable pageable);

    long countByUserRole(UserRole role);

    List<AppUser> findByTenantIdAndUserRole(Long tenantId, UserRole role);

    /**
     * Admin accounts sitting on a given tenant with any of the listed roles.
     *
     * <p>Used to find accounts stranded on the platform tenant: tenant 0 disables the Hibernate
     * tenant filter, so a TENANT_ADMIN or BUILDING_STAFF stamped 0 reads every company's data
     * (GH-212).
     */
    List<AppUser> findByTenantIdAndUserRoleIn(Long tenantId, java.util.Collection<UserRole> roles);

    boolean existsByEmail(String email);
}
