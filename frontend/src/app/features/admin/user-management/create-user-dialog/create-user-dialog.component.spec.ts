import { TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { CreateUserDialogComponent } from './create-user-dialog.component';
import { AdminService } from '../../../../core/services/admin.service';
import { AuthService } from '../../../../core/services/auth.service';
import { TenantService } from '../../../../core/services/tenant.service';

/**
 * GH-146: the backend now requires a real institution tenant for TENANT_ADMIN and BUILDING_STAFF,
 * and requires the platform tenant for PLATFORM_ADMIN. Tenant 0 is the sentinel that disables the
 * tenant filter, so offering it for a tenant-scoped role is the privilege escalation itself.
 */
describe('CreateUserDialogComponent tenant scoping', () => {
  const tenants = [
    { id: 0, name: 'Platform' },
    { id: 7, name: 'Northgate' },
  ];

  function build(): CreateUserDialogComponent {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [CreateUserDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: MatDialogRef, useValue: { close: jasmine.createSpy('close') } },
        { provide: AdminService, useValue: jasmine.createSpyObj('AdminService', ['createUser']) },
        // A platform admin: the only actor the tenant selector is shown to.
        { provide: AuthService, useValue: { currentUserValue: { tenantId: 0 } } },
        {
          provide: TenantService,
          useValue: { getAllTenants: () => of(tenants) },
        },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
      ],
    });
    return TestBed.createComponent(CreateUserDialogComponent).componentInstance;
  }

  it('availableRoles_excludesTheDeprecatedAdminRole', () => {
    // The backend mapper rejects ADMIN outright, so it failed every time it was chosen.
    expect(build().availableRoles).toEqual(['PLATFORM_ADMIN', 'TENANT_ADMIN', 'BUILDING_STAFF']);
  });

  it('defaultRole_isOneTheBackendAccepts', () => {
    const component = build();

    expect(component.availableRoles).toContain(component.createUserForm.get('userRole')?.value);
  });

  it('availableTenantsForRole_neverOffersThePlatformTenant', () => {
    expect(build().availableTenantsForRole.map((t) => t.id)).toEqual([7]);
  });

  it('showTenantField_platformAdminRole_isHidden', () => {
    const component = build();

    component.createUserForm.get('userRole')?.setValue('PLATFORM_ADMIN');

    expect(component.showTenantField).toBeFalse();
  });

  it('showTenantField_tenantScopedRole_isShown', () => {
    const component = build();

    component.createUserForm.get('userRole')?.setValue('TENANT_ADMIN');

    expect(component.showTenantField).toBeTrue();
  });

  /**
   * Without the reset, a tenant chosen for a scoped role stays on the control after switching to
   * PLATFORM_ADMIN and is submitted with it, which the backend now rejects. The control is no longer
   * visible at that point, so the user cannot see what is wrong.
   */
  it('switchingToPlatformAdmin_clearsAStaleTenantSelection', () => {
    const component = build();
    component.createUserForm.get('userRole')?.setValue('TENANT_ADMIN');
    component.createUserForm.get('tenantId')?.setValue(7);

    component.createUserForm.get('userRole')?.setValue('PLATFORM_ADMIN');

    expect(component.createUserForm.get('tenantId')?.value).toBeNull();
  });

  it('platformAdminRole_leavesTheFormValidWithNoTenant', () => {
    const component = build();
    component.createUserForm.patchValue({
      firstName: 'Ada',
      lastName: 'Lovelace',
      email: 'ada@example.com',
      password: 'password123',
    });

    component.createUserForm.get('userRole')?.setValue('PLATFORM_ADMIN');

    expect(component.createUserForm.valid).toBeTrue();
  });

  it('tenantScopedRole_requiresATenant', () => {
    const component = build();
    component.createUserForm.patchValue({
      firstName: 'Ada',
      lastName: 'Lovelace',
      email: 'ada@example.com',
      password: 'password123',
    });

    component.createUserForm.get('userRole')?.setValue('BUILDING_STAFF');

    expect(component.createUserForm.valid).toBeFalse();
  });
});
