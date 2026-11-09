package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.exceptions.TenantNotFoundException;
import com.beezhub.student_accommodation.model.dto.TenantAdminResponseDto;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.service.tenant.TenantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Listing and deactivating tenant admins. Moving one between companies is covered by
 * {@link TenantAdminServiceMoveTest}.
 */
@ExtendWith(MockitoExtension.class)
class TenantAdminServiceTest {

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private TenantService tenantService;
    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private TenantAdminService tenantAdminService;

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 10L;

    private static AppUser user(Long tenantId, UserRole role) {
        return AppUser.builder()
                .id(USER_ID)
                .email("ada@example.com")
                .firstName("Ada")
                .lastName("Lovelace")
                .userRole(role)
                .tenantId(tenantId)
                .mustChangePassword(true)
                .build();
    }

    @Test
    void listTenantAdmins_tenantExists_returnsMappedAdmins() {
        when(tenantRepository.existsById(TENANT_ID)).thenReturn(true);
        when(appUserRepository.findByTenantIdAndUserRole(TENANT_ID, UserRole.TENANT_ADMIN))
                .thenReturn(List.of(user(TENANT_ID, UserRole.TENANT_ADMIN)));

        List<TenantAdminResponseDto> result = tenantAdminService.listTenantAdmins(TENANT_ID);

        assertThat(result).hasSize(1);
        TenantAdminResponseDto dto = result.get(0);
        assertThat(dto.id()).isEqualTo(USER_ID);
        assertThat(dto.email()).isEqualTo("ada@example.com");
        assertThat(dto.firstName()).isEqualTo("Ada");
        assertThat(dto.lastName()).isEqualTo("Lovelace");
        assertThat(dto.mustChangePassword()).isTrue();
    }

    @Test
    void listTenantAdmins_tenantMissing_throwsTenantNotFoundException() {
        when(tenantRepository.existsById(TENANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> tenantAdminService.listTenantAdmins(TENANT_ID))
                .isInstanceOf(TenantNotFoundException.class);

        verify(appUserRepository, never()).findByTenantIdAndUserRole(any(), any());
    }

    @Test
    void deactivateTenantAdmin_validAdmin_locksAccountAndRevokesTokens() {
        AppUser admin = user(TENANT_ID, UserRole.TENANT_ADMIN);
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(admin));

        tenantAdminService.deactivateTenantAdmin(TENANT_ID, USER_ID);

        assertThat(admin.getAccountLockedUntil()).isAfter(LocalDateTime.of(9000, 1, 1, 0, 0));
        verify(appUserRepository).save(admin);
        // Locking only stops future logins; tokens already issued must be retired too (GH-122).
        verify(tokenBlacklistService).revokeTokensIssuedBefore(eq("ada@example.com"), any(), anyLong());
    }

    @Test
    void deactivateTenantAdmin_userMissing_throwsResourceNotFoundException() {
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantAdminService.deactivateTenantAdmin(TENANT_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(tokenBlacklistService, never()).revokeTokensIssuedBefore(any(), any(), anyLong());
    }

    @Test
    void deactivateTenantAdmin_userInOtherTenant_throwsResourceNotFoundException() {
        when(appUserRepository.findById(USER_ID))
                .thenReturn(Optional.of(user(7L, UserRole.TENANT_ADMIN)));

        assertThatThrownBy(() -> tenantAdminService.deactivateTenantAdmin(TENANT_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void deactivateTenantAdmin_userIsNotTenantAdmin_throwsResourceNotFoundException() {
        when(appUserRepository.findById(USER_ID))
                .thenReturn(Optional.of(user(TENANT_ID, UserRole.STUDENT)));

        assertThatThrownBy(() -> tenantAdminService.deactivateTenantAdmin(TENANT_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(appUserRepository, never()).save(any());
        verify(tokenBlacklistService, never()).revokeTokensIssuedBefore(any(), any(), anyLong());
    }
}
