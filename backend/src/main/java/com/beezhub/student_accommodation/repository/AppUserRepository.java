package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);

    List<AppUser> findByUserRole(UserRole role);

    long countByUserRole(UserRole role);

    List<AppUser> findByTenantIdAndUserRole(Long tenantId, UserRole role);

    boolean existsByEmail(String email);
}
