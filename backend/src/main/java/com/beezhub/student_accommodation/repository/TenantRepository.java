package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByName(String name);
    Optional<Tenant> findByDomain(String domain);

    /**
     * Companies that may still receive new buildings and staff.
     *
     * <p>Deliberately separate from {@code findAll} rather than replacing it. The tenant
     * administration screen is the only place a deactivated company can be switched back on, so
     * it has to keep listing them; filtering at the single existing call site would have made
     * deactivation irreversible through the UI (GH-267).
     */
    List<Tenant> findByIsActiveTrue();
}
