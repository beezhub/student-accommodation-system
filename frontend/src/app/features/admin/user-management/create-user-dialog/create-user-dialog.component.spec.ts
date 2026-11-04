import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { CreateUserDialogComponent } from './create-user-dialog.component';
import { AdminService, AdminUser } from '../../../../core/services/admin.service';
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

  let tenantServiceStub: {
    getAllTenants: jasmine.Spy;
    getActiveTenants: jasmine.Spy;
  };
  let fixture: ComponentFixture<CreateUserDialogComponent>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  function build(): CreateUserDialogComponent {
    tenantServiceStub = {
      getAllTenants: jasmine.createSpy('getAllTenants').and.returnValue(of(tenants)),
      getActiveTenants: jasmine.createSpy('getActiveTenants').and.returnValue(of(tenants)),
    };
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
          useValue: tenantServiceStub,
        },
      ],
    });
    // The component imports MatSnackBarModule itself, and a standalone component's own imports win
    // over TestBed providers, so the spy has to be an override (as in TenantAdminManagementComponent).
    snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    TestBed.overrideProvider(MatSnackBar, { useValue: snackBar });
    fixture = TestBed.createComponent(CreateUserDialogComponent);
    return fixture.componentInstance;
  }

  function fillIn(component: CreateUserDialogComponent, role: string): void {
    component.createUserForm.patchValue({
      firstName: 'Thandi',
      lastName: 'Mokoena',
      email: 'thandi@acme.test',
      password: 'Correct-Horse9',
    });
    component.createUserForm.get('userRole')?.setValue(role);
  }

  /** #430: the form asked for 8 characters while the server's @StrongPassword refuses them. */
  it('passwordTheServerWouldRefuse_isNotSent', () => {
    const component = build();
    const adminService = TestBed.inject(AdminService) as jasmine.SpyObj<AdminService>;
    fillIn(component, 'TENANT_ADMIN');
    component.createUserForm.get('password')?.setValue('simplepassword');

    component.onSubmit();

    expect(component.createUserForm.valid).toBeFalse();
    expect(adminService.createUser).not.toHaveBeenCalled();
  });

  it('passwordTheServerWouldRefuse_saysWhichRulesAreMissing', () => {
    const component = build();
    fillIn(component, 'TENANT_ADMIN');
    component.createUserForm.get('password')?.setValue('simplepassword');
    component.createUserForm.markAllAsTouched();

    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Password needs an uppercase letter, a number, a symbol'
    );
  });

  it('availableRoles_excludesTheDeprecatedAdminRole', () => {
    // The backend mapper rejects ADMIN outright, so it failed every time it was chosen.
    expect(build().availableRoles).toEqual([
      'PLATFORM_ADMIN',
      'PLATFORM_STAFF',
      'TENANT_ADMIN',
      'BUILDING_STAFF',
    ]);
  });

  it('defaultRole_isOneTheBackendAccepts', () => {
    const component = build();

    expect(component.availableRoles).toContain(component.createUserForm.get('userRole')?.value);
  });

  it('availableTenantsForRole_neverOffersThePlatformTenant', () => {
    expect(build().availableTenantsForRole.map((t) => t.id)).toEqual([7]);
  });

  /**
   * GH-267: this list was unfiltered, so a deactivated company stayed selectable and a new
   * TENANT_ADMIN or BUILDING_STAFF could be attached to it. The full list is for the tenant
   * administration screen, which has to keep showing deactivated companies to switch them back on.
   */
  it('tenantOptions_comeFromTheActiveOnlyList', () => {
    build();

    expect(tenantServiceStub.getActiveTenants).toHaveBeenCalled();
    expect(tenantServiceStub.getAllTenants).not.toHaveBeenCalled();
  });

  it('showTenantField_platformAdminRole_isHidden', () => {
    const component = build();

    component.createUserForm.get('userRole')?.setValue('PLATFORM_ADMIN');

    expect(component.showTenantField).toBeFalse();
  });

  it('showTenantField_platformStaffRole_isHidden', () => {
    // Staff must be tenant 0; offering a company here would produce a request the backend
    // refuses with "Platform roles are not scoped to a tenant" (GH-238).
    const component = build();

    component.createUserForm.get('userRole')?.setValue('PLATFORM_STAFF');

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
      password: 'Correct-Horse9',
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
      password: 'Correct-Horse9',
    });

    component.createUserForm.get('userRole')?.setValue('BUILDING_STAFF');

    expect(component.createUserForm.valid).toBeFalse();
  });

  /**
   * #413: a tenant admin may be created with no company yet. It waits until a company's admin
   * screen adds it, which is now the only way a company gets a new admin.
   */
  it('tenantAdminRole_isValidWithNoCompany', () => {
    const component = build();

    fillIn(component, 'TENANT_ADMIN');

    expect(component.createUserForm.valid).toBeTrue();
  });

  it('tenantAdminRole_explainsWhatLeavingTheCompanyEmptyMeans', () => {
    const component = build();
    fillIn(component, 'TENANT_ADMIN');

    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      "Leave empty to add them from a company's admin screen later. They can't sign in until then."
    );
  });

  /** They must not be handed login details that do not work yet, so the message says so. */
  it('creatingATenantAdminWithNoCompany_saysTheyCannotSignInYet', () => {
    const component = build();
    const adminService = TestBed.inject(AdminService) as jasmine.SpyObj<AdminService>;
    adminService.createUser.and.returnValue(of({} as AdminUser));
    snackBar.open.and.returnValue({ onAction: () => of() } as never);
    fillIn(component, 'TENANT_ADMIN');

    component.onSubmit();

    expect(snackBar.open).toHaveBeenCalledWith(
      "Thandi Mokoena created. They can't sign in until a company adds them as an admin.",
      'Go to companies',
      jasmine.anything()
    );
  });

  it('creatingATenantAdminWithACompany_keepsTheUsualMessage', () => {
    const component = build();
    const adminService = TestBed.inject(AdminService) as jasmine.SpyObj<AdminService>;
    adminService.createUser.and.returnValue(of({} as AdminUser));
    fillIn(component, 'TENANT_ADMIN');
    component.createUserForm.get('tenantId')?.setValue(7);

    component.onSubmit();

    expect(snackBar.open).toHaveBeenCalledWith(
      'User created successfully!',
      'Close',
      jasmine.anything()
    );
  });
});
