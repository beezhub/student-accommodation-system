package com.beezhub.student_accommodation.service.tenant;

import com.beezhub.student_accommodation.exceptions.TenantNotFoundException;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for tenant lookup operations.
 *
 * <p>Note: the {@code tenantFilter} Hibernate filter is <em>not</em> applied to the
 * {@code tenant} table itself, so all methods here are safe to call regardless of the
 * current {@link TenantContext} value.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    static final String DEFAULT_TENANT_NAME = "Default Tenant";

    private final TenantRepository tenantRepository;

    /**
     * Returns the default tenant used for new sign-ups and single-tenant deployments.
     *
     * @throws TenantNotFoundException if the default tenant record has not been seeded
     */
    @Transactional(readOnly = true)
    public Tenant getDefaultTenant() {
        return tenantRepository.findByName(DEFAULT_TENANT_NAME)
                .orElseThrow(() -> new TenantNotFoundException(
                        "Default tenant not found — ensure the backfill migration has been applied"));
    }

    /**
     * Returns the tenant with the given primary-key ID.
     *
     * @throws TenantNotFoundException if no tenant with that ID exists
     */
    @Transactional(readOnly = true)
    public Tenant getTenantById(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + id));
    }

    /**
     * Resolves a tenant by its domain string.
     * Useful for future domain-based tenant routing (e.g. subdomain per institution).
     *
     * @throws TenantNotFoundException if no tenant is registered for the given domain
     */
    @Transactional(readOnly = true)
    public Tenant getTenantByDomain(String domain) {
        return tenantRepository.findByDomain(domain)
                .orElseThrow(() -> new TenantNotFoundException(
                        "Tenant not found for domain: " + domain));
    }
}
