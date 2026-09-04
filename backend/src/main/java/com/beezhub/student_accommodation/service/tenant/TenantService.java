package com.beezhub.student_accommodation.service.tenant;

import com.beezhub.student_accommodation.exceptions.TenantNotFoundException;
import com.beezhub.student_accommodation.model.dto.TenantRequestDto;
import com.beezhub.student_accommodation.model.dto.TenantResponseDto;
import com.beezhub.student_accommodation.model.dto.TenantStatusDto;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.repository.TenantRepository;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
     * Returns all tenants in the system.
     * Safe to call regardless of current {@link TenantContext} value because the
     * {@code tenantFilter} Hibernate filter is not applied to the {@code tenant} table.
     */
    @Transactional(readOnly = true)
    public List<TenantResponseDto> getAllTenants() {
        return tenantRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

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
    public TenantResponseDto getTenantById(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + id));
        return toResponseDto(tenant);
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

    /**
     * Creates a new tenant.
     *
     * @throws DataIntegrityViolationException if the name is already taken (propagated to 409 by handler)
     */
    @Transactional
    public TenantResponseDto createTenant(TenantRequestDto request) {
        Tenant tenant = Tenant.builder()
                .name(request.getName())
                .domain(request.getDomain())
                .build();
        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant created: id={}, name={}", saved.getId(), saved.getName());
        return toResponseDto(saved);
    }

    /**
     * Updates the name and/or domain of an existing tenant.
     * The platform tenant (id=0) may not be modified.
     *
     * @throws IllegalArgumentException if id == 0
     * @throws TenantNotFoundException  if no tenant with that ID exists
     */
    @Transactional
    public TenantResponseDto updateTenant(Long id, TenantRequestDto request) {
        if (id == 0L) {
            throw new IllegalArgumentException("Platform tenant cannot be modified");
        }
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + id));
        tenant.setName(request.getName());
        tenant.setDomain(request.getDomain());
        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant updated: id={}", saved.getId());
        return toResponseDto(saved);
    }

    /**
     * Toggles the active status of a tenant.
     * The platform tenant (id=0) may not be deactivated.
     *
     * @throws IllegalArgumentException if deactivating id == 0
     * @throws TenantNotFoundException  if no tenant with that ID exists
     */
    @Transactional
    public TenantResponseDto toggleTenantStatus(Long id, TenantStatusDto statusDto) {
        if (id == 0L && Boolean.FALSE.equals(statusDto.getIsActive())) {
            throw new IllegalArgumentException("Platform tenant cannot be deactivated");
        }
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + id));
        tenant.setIsActive(statusDto.getIsActive());
        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant {} status toggled to isActive={}", saved.getId(), saved.getIsActive());
        return toResponseDto(saved);
    }

    private TenantResponseDto toResponseDto(Tenant tenant) {
        return TenantResponseDto.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .domain(tenant.getDomain())
                .isActive(tenant.getIsActive())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }
}
