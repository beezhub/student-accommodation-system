package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.InactiveTenantException;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.model.dto.TenantAdminResponseDto;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.service.tenant.TenantService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Moving a tenant admin from one property company to another.
 *
 * <p>Before this, the only reassignment path was {@code UnassignedAdminService.assignTenant},
 * which only accepts accounts stranded on the platform tenant. An admin created under the wrong
 * company could only be fixed with a raw SQL update, which also left their old token carrying
 * the old tenant claim for up to four hours.
 */
@ExtendWith(MockitoExtension.class)
class TenantAdminServiceMoveTest {

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private TenantService tenantService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private TenantAdminService tenantAdminService;

    private static final Long SOURCE_TENANT = 1L;
    private static final Long TARGET_TENANT = 5L;
    private static final Long USER_ID = 10L;

    private static AppUser admin(Long tenantId, UserRole role) {
        return AppUser.builder()
                .id(USER_ID)
                .email("ada@example.com")
                .firstName("Ada")
                .lastName("Lovelace")
                .userRole(role)
                .tenantId(tenantId)
                .build();
    }

    @Test
    void moveTenantAdmin_validMove_changesTenantAndRevokesTokens() {
        AppUser user = admin(SOURCE_TENANT, UserRole.TENANT_ADMIN);
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(tenantService.requireTenantAcceptingNewRecords(TARGET_TENANT))
                .thenReturn(Tenant.builder().id(TARGET_TENANT).name("Live Ltd").isActive(true).build());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantAdminResponseDto result =
                tenantAdminService.moveTenantAdmin(SOURCE_TENANT, USER_ID, TARGET_TENANT);

        assertThat(user.getTenantId()).isEqualTo(TARGET_TENANT);
        assertThat(result.id()).isEqualTo(USER_ID);
        verify(appUserRepository).save(user);
        // The old JWT still carries tenantId=1; without this the admin keeps the old company's
        // data until the token expires.
        verify(tokenBlacklistService).revokeTokensIssuedBefore(eq("ada@example.com"), any(), anyLong());
    }

    @Test
    void moveTenantAdmin_targetIsPlatformTenant_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> tenantAdminService.moveTenantAdmin(SOURCE_TENANT, USER_ID, 0L))
                .isInstanceOf(IllegalArgumentException.class);

        verify(appUserRepository, never()).save(any());
        verify(tokenBlacklistService, never()).revokeTokensIssuedBefore(any(), any(), anyLong());
    }

    @Test
    void moveTenantAdmin_targetIsSameTenant_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> tenantAdminService.moveTenantAdmin(SOURCE_TENANT, USER_ID, SOURCE_TENANT))
                .isInstanceOf(IllegalArgumentException.class);

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void moveTenantAdmin_userNotInSourceTenant_throwsResourceNotFoundException() {
        when(appUserRepository.findById(USER_ID))
                .thenReturn(Optional.of(admin(7L, UserRole.TENANT_ADMIN)));

        assertThatThrownBy(() -> tenantAdminService.moveTenantAdmin(SOURCE_TENANT, USER_ID, TARGET_TENANT))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void moveTenantAdmin_userIsNotTenantAdmin_throwsResourceNotFoundException() {
        when(appUserRepository.findById(USER_ID))
                .thenReturn(Optional.of(admin(SOURCE_TENANT, UserRole.BUILDING_STAFF)));

        assertThatThrownBy(() -> tenantAdminService.moveTenantAdmin(SOURCE_TENANT, USER_ID, TARGET_TENANT))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void moveTenantAdmin_userMissing_throwsResourceNotFoundException() {
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantAdminService.moveTenantAdmin(SOURCE_TENANT, USER_ID, TARGET_TENANT))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void moveTenantAdmin_targetTenantInactive_throwsInactiveTenantException() {
        when(appUserRepository.findById(USER_ID))
                .thenReturn(Optional.of(admin(SOURCE_TENANT, UserRole.TENANT_ADMIN)));
        when(tenantService.requireTenantAcceptingNewRecords(TARGET_TENANT))
                .thenThrow(new InactiveTenantException("Property company 'Gone Ltd' is deactivated"));

        assertThatThrownBy(() -> tenantAdminService.moveTenantAdmin(SOURCE_TENANT, USER_ID, TARGET_TENANT))
                .isInstanceOf(InactiveTenantException.class);

        verify(appUserRepository, never()).save(any());
        verify(tokenBlacklistService, never()).revokeTokensIssuedBefore(any(), any(), anyLong());
    }
}
