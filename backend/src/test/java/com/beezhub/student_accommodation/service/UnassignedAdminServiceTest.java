package com.beezhub.student_accommodation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.exceptions.TenantNotFoundException;
import com.beezhub.student_accommodation.model.dto.UnassignedAdminResponseDto;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GH-212: tenant 0 is the sentinel that disables the Hibernate tenant filter, so a TENANT_ADMIN or
 * BUILDING_STAFF account stamped 0 could read every company's data. Their owning company cannot be
 * derived from any column, so the migration locks them and a platform admin assigns each by hand.
 */
@ExtendWith(MockitoExtension.class)
class UnassignedAdminServiceTest {

    private static final Long OWNING_TENANT = 42L;
    private static final LocalDateTime LOCKED_FOREVER = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    @Mock private AppUserRepository appUserRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private TokenBlacklistService tokenBlacklistService;

    @InjectMocks private UnassignedAdminService service;

    private static AppUser strandedAdmin(Long id, UserRole role) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setEmail("stranded" + id + "@test.local");
        user.setFirstName("Stranded");
        user.setLastName("Admin");
        user.setUserRole(role);
        user.setTenantId(0L);
        user.setAccountLockedUntil(LOCKED_FOREVER);
        return user;
    }

    private static Tenant company() {
        Tenant tenant = new Tenant();
        tenant.setId(OWNING_TENANT);
        tenant.setName("Acme Residences");
        return tenant;
    }

    @Test
    void listUnassignedAdmins_returnsAccountsStrandedOnThePlatformTenant() {
        when(appUserRepository.findByTenantIdAndUserRoleIn(eq(0L), any())).thenReturn(List.of(
                strandedAdmin(1L, UserRole.TENANT_ADMIN),
                strandedAdmin(2L, UserRole.BUILDING_STAFF)));

        List<UnassignedAdminResponseDto> found = service.listUnassignedAdmins();

        assertThat(found).hasSize(2);
        assertThat(found).allSatisfy(row -> assertThat(row.tenantId()).isZero());
        // Newest first, so the most recently created strays are dealt with before old ones.
        assertThat(found.get(0).id()).isEqualTo(2L);
    }

    /**
     * The deprecated ADMIN role is listed for review but not locked by the migration: it is the
     * legacy platform-admin role, and locking it could lock out the account needed to do the
     * reassignments.
     */
    @Test
    void listUnassignedAdmins_includesTheDeprecatedAdminRoleForReview() {
        when(appUserRepository.findByTenantIdAndUserRoleIn(eq(0L), any()))
                .thenReturn(List.of(strandedAdmin(3L, UserRole.ADMIN)));

        assertThat(service.listUnassignedAdmins()).singleElement()
                .satisfies(row -> assertThat(row.userRole()).isEqualTo(UserRole.ADMIN));
    }

    /**
     * PLATFORM_STAFF belongs on the platform tenant, so a staff row sitting there is correct, not
     * stranded. Listing it would invite an operator to "fix" it by assigning a company, which
     * would strip the cross-company visibility the role exists for (GH-238).
     */
    @Test
    void listUnassignedAdmins_doesNotTreatPlatformStaffAsStranded() {
        ArgumentCaptor<Collection<UserRole>> roles = ArgumentCaptor.forClass(Collection.class);
        when(appUserRepository.findByTenantIdAndUserRoleIn(eq(0L), any())).thenReturn(List.of());

        service.listUnassignedAdmins();

        verify(appUserRepository).findByTenantIdAndUserRoleIn(eq(0L), roles.capture());
        assertThat(roles.getValue())
                .doesNotContain(UserRole.PLATFORM_STAFF, UserRole.PLATFORM_ADMIN)
                .contains(UserRole.TENANT_ADMIN, UserRole.BUILDING_STAFF);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"TENANT_ADMIN", "BUILDING_STAFF", "ADMIN"})
    void assignTenant_setsTheCompanyAndUnlocksTheAccount(UserRole role) {
        AppUser user = strandedAdmin(1L, role);
        when(tenantRepository.findById(OWNING_TENANT)).thenReturn(Optional.of(company()));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        UnassignedAdminResponseDto assigned = service.assignTenant(1L, OWNING_TENANT);

        assertThat(assigned.tenantId()).isEqualTo(OWNING_TENANT);
        // Assigned but still locked would be the same operational problem somewhere else.
        assertThat(assigned.accountLockedUntil()).isNull();
    }

    /**
     * A JWT carries the tenant claim it was issued with, so without this the account keeps its
     * platform-wide access until that token expires — up to four hours after the reassignment.
     */
    @Test
    void assignTenant_revokesTheTokensStillCarryingThePlatformTenant() {
        AppUser user = strandedAdmin(1L, UserRole.TENANT_ADMIN);
        when(tenantRepository.findById(OWNING_TENANT)).thenReturn(Optional.of(company()));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        service.assignTenant(1L, OWNING_TENANT);

        verify(tokenBlacklistService).revokeTokensIssuedBefore(
                eq("stranded1@test.local"), any(), anyLong());
    }

    @Test
    void assignTenant_toThePlatformTenant_isRefused() {
        assertThatThrownBy(() -> service.assignTenant(1L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("property company");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void assignTenant_unknownCompany_isRefused() {
        when(tenantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignTenant(1L, 999L))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void assignTenant_unknownUser_isRefused() {
        when(tenantRepository.findById(OWNING_TENANT)).thenReturn(Optional.of(company()));
        when(appUserRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignTenant(1L, OWNING_TENANT))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assignTenant_accountAlreadyAssigned_isRefused() {
        // Not idempotent on purpose: re-running this would silently move an account between
        // companies, which is a different and much larger operation.
        AppUser user = strandedAdmin(1L, UserRole.TENANT_ADMIN);
        user.setTenantId(7L);
        when(tenantRepository.findById(OWNING_TENANT)).thenReturn(Optional.of(company()));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.assignTenant(1L, OWNING_TENANT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already belongs");
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"STUDENT", "PLATFORM_ADMIN"})
    void assignTenant_aRoleThatBelongsOnThePlatformTenant_isRefused(UserRole role) {
        AppUser user = strandedAdmin(1L, role);
        when(tenantRepository.findById(OWNING_TENANT)).thenReturn(Optional.of(company()));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.assignTenant(1L, OWNING_TENANT))
                .isInstanceOf(IllegalStateException.class);

        verify(appUserRepository, never()).save(any());
        verify(tokenBlacklistService, never()).revokeTokensIssuedBefore(anyString(), any(), anyLong());
    }
}
