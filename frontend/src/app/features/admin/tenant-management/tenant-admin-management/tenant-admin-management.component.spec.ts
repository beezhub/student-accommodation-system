import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { TenantAdminManagementComponent } from './tenant-admin-management.component';
import { TenantService, TenantAdminUser } from '../../../../core/services/tenant.service';

/**
 * GH-300: `confirmDeactivate` used to pop the browser's own `window.confirm`, which is untestable
 * without stubbing a global and inconsistent with the Material dialog used everywhere else for
 * destructive admin actions. It now goes through the shared `confirmAction` helper.
 */
describe('TenantAdminManagementComponent', () => {
  let component: TenantAdminManagementComponent;
  let tenantService: jasmine.SpyObj<TenantService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  const admin: TenantAdminUser = {
    id: 5,
    email: 'admin@example.com',
    firstName: 'Jane',
    lastName: 'Doe',
  };

  beforeEach(() => {
    tenantService = jasmine.createSpyObj<TenantService>('TenantService', [
      'listTenantAdmins',
      'deactivateTenantAdmin',
    ]);
    tenantService.listTenantAdmins.and.returnValue(of([admin]));
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    TestBed.configureTestingModule({
      imports: [TenantAdminManagementComponent],
      providers: [provideNoopAnimations(), { provide: TenantService, useValue: tenantService }],
    });
    // The component imports MatDialogModule and MatSnackBarModule itself, and a standalone
    // component's own imports take precedence over TestBed providers, so these have to be
    // overrides rather than providers.
    TestBed.overrideProvider(MatDialog, { useValue: dialog });
    TestBed.overrideProvider(MatSnackBar, { useValue: snackBar });

    const fixture = TestBed.createComponent(TenantAdminManagementComponent);
    component = fixture.componentInstance;
    component.tenantId = 1;
    fixture.detectChanges();
  });

  it('confirmDeactivate_confirmed_deactivatesAdminAndReloadsList', () => {
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    tenantService.deactivateTenantAdmin.and.returnValue(of(undefined));

    component.confirmDeactivate(admin);

    expect(dialog.open).toHaveBeenCalled();
    expect(tenantService.deactivateTenantAdmin).toHaveBeenCalledWith(1, admin.id);
    expect(snackBar.open).toHaveBeenCalledWith(
      'Admin account deactivated',
      'Close',
      jasmine.objectContaining({ panelClass: 'snack-success' })
    );
  });

  it('confirmDeactivate_dismissed_doesNotCallDeactivate', () => {
    dialog.open.and.returnValue({ afterClosed: () => of(false) } as never);

    component.confirmDeactivate(admin);

    expect(dialog.open).toHaveBeenCalled();
    expect(tenantService.deactivateTenantAdmin).not.toHaveBeenCalled();
  });

  it('confirmDeactivate_apiError_showsErrorMessage', () => {
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    tenantService.deactivateTenantAdmin.and.returnValue(
      throwError(() => ({ error: { message: 'Cannot deactivate the last admin' } }))
    );

    component.confirmDeactivate(admin);

    expect(snackBar.open).toHaveBeenCalledWith(
      'Cannot deactivate the last admin',
      'Close',
      jasmine.objectContaining({ duration: 4000 })
    );
  });
});
