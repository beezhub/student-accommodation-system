package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.InactiveTenantException;
import com.beezhub.student_accommodation.model.dto.TenantAdminRequestDto;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.service.tenant.TenantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The one place {@code isActive} was read before GH-267.
 *
 * <p>It threw {@code DuplicateResourceException}, which the handler answers with 409 Conflict
 * although nothing is duplicated, and the other route to creating a tenant-scoped account,
 * {@code UserManagementService}, performed no such check at all. Both now go through
 * {@code TenantService.requireTenantAcceptingNewRecords}, so the rule and its status cannot drift
 * apart again.
 */
@ExtendWith(MockitoExtension.class)
class TenantAdminServiceInactiveTenantTest {

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private TenantService tenantService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private NotificationService notificationService;
    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private TenantAdminService tenantAdminService;

    private static final TenantAdminRequestDto REQUEST =
            new TenantAdminRequestDto("Ada", "Lovelace", "ada@example.com", "Str0ng!Passw0rd");

    @Test
    void createTenantAdmin_inactiveTenant_throwsInactiveTenantException() {
        when(tenantService.requireTenantAcceptingNewRecords(5L))
                .thenThrow(new InactiveTenantException("Property company 'Gone Ltd' is deactivated"));

        assertThatThrownBy(() -> tenantAdminService.createTenantAdmin(5L, REQUEST))
                .isInstanceOf(InactiveTenantException.class);

        verify(appUserRepository, never()).save(any());
        verify(notificationService, never()).sendWelcomeEmail(any());
    }

    @Test
    void createTenantAdmin_activeTenant_createsTheAdmin() {
        when(tenantService.requireTenantAcceptingNewRecords(5L))
                .thenReturn(Tenant.builder().id(5L).name("Live Ltd").isActive(true).build());
        when(appUserRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        tenantAdminService.createTenantAdmin(5L, REQUEST);

        verify(appUserRepository).save(any(AppUser.class));
    }
}
