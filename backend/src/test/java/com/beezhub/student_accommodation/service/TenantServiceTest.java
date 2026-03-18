package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.TenantNotFoundException;
import com.beezhub.student_accommodation.model.dto.TenantRequestDto;
import com.beezhub.student_accommodation.model.dto.TenantResponseDto;
import com.beezhub.student_accommodation.model.dto.TenantStatusDto;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.service.tenant.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    private Tenant sampleTenant;

    @BeforeEach
    void setUp() {
        sampleTenant = Tenant.builder()
                .id(1L)
                .name("University of Cape Town")
                .domain("uct.ac.za")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── createTenant ─────────────────────────────────────────────

    @Test
    void createTenant_savesAndReturnsTenant() {
        TenantRequestDto request = new TenantRequestDto("University of Cape Town", "uct.ac.za");
        when(tenantRepository.save(any(Tenant.class))).thenReturn(sampleTenant);

        TenantResponseDto result = tenantService.createTenant(request);

        assertThat(result.getName()).isEqualTo("University of Cape Town");
        assertThat(result.getDomain()).isEqualTo("uct.ac.za");
        assertThat(result.getIsActive()).isTrue();
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    void createTenant_withNullDomain_savesSuccessfully() {
        TenantRequestDto request = new TenantRequestDto("University of Cape Town", null);
        Tenant savedWithoutDomain = Tenant.builder()
                .id(2L).name("University of Cape Town").isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedWithoutDomain);

        TenantResponseDto result = tenantService.createTenant(request);

        assertThat(result.getDomain()).isNull();
    }

    // ── updateTenant ─────────────────────────────────────────────

    @Test
    void updateTenant_updatesAndReturns() {
        TenantRequestDto request = new TenantRequestDto("UCT Updated", "updated.ac.za");
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(sampleTenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(sampleTenant);

        TenantResponseDto result = tenantService.updateTenant(1L, request);

        assertThat(result).isNotNull();
        verify(tenantRepository).findById(1L);
        verify(tenantRepository).save(sampleTenant);
    }

    @Test
    void updateTenant_platformTenant_throwsIllegalArgument() {
        TenantRequestDto request = new TenantRequestDto("Platform", null);

        assertThatThrownBy(() -> tenantService.updateTenant(0L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Platform tenant cannot be modified");

        verifyNoInteractions(tenantRepository);
    }

    @Test
    void updateTenant_notFound_throwsTenantNotFoundException() {
        TenantRequestDto request = new TenantRequestDto("Ghost Tenant", null);
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.updateTenant(99L, request))
                .isInstanceOf(TenantNotFoundException.class);
    }

    // ── toggleTenantStatus ───────────────────────────────────────

    @Test
    void toggleTenantStatus_deactivatesTenant() {
        TenantStatusDto status = new TenantStatusDto(false);
        Tenant deactivated = Tenant.builder().id(1L).name("UCT").isActive(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(sampleTenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(deactivated);

        TenantResponseDto result = tenantService.toggleTenantStatus(1L, status);

        assertThat(result.getIsActive()).isFalse();
        verify(tenantRepository).save(sampleTenant);
    }

    @Test
    void toggleTenantStatus_activatesTenant() {
        Tenant inactiveTenant = Tenant.builder().id(1L).name("UCT").isActive(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        TenantStatusDto status = new TenantStatusDto(true);
        Tenant activated = Tenant.builder().id(1L).name("UCT").isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(inactiveTenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(activated);

        TenantResponseDto result = tenantService.toggleTenantStatus(1L, status);

        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    void toggleTenantStatus_deactivatePlatformTenant_throwsIllegalArgument() {
        TenantStatusDto status = new TenantStatusDto(false);

        assertThatThrownBy(() -> tenantService.toggleTenantStatus(0L, status))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Platform tenant cannot be deactivated");

        verifyNoInteractions(tenantRepository);
    }

    @Test
    void toggleTenantStatus_activatePlatformTenant_isAllowed() {
        TenantStatusDto status = new TenantStatusDto(true);
        Tenant platformTenant = Tenant.builder().id(0L).name("PLATFORM").isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(tenantRepository.findById(0L)).thenReturn(Optional.of(platformTenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(platformTenant);

        TenantResponseDto result = tenantService.toggleTenantStatus(0L, status);

        assertThat(result).isNotNull();
    }

    @Test
    void toggleTenantStatus_notFound_throwsTenantNotFoundException() {
        TenantStatusDto status = new TenantStatusDto(false);
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.toggleTenantStatus(99L, status))
                .isInstanceOf(TenantNotFoundException.class);
    }
}
