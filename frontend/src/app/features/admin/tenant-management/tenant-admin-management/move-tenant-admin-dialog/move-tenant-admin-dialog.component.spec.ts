import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { MoveTenantAdminDialogComponent } from './move-tenant-admin-dialog.component';
import {
  TenantService,
  Tenant,
  TenantAdminUser,
} from '../../../../../core/services/tenant.service';

describe('MoveTenantAdminDialogComponent', () => {
  let component: MoveTenantAdminDialogComponent;
  let tenantService: jasmine.SpyObj<TenantService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<MoveTenantAdminDialogComponent>>;

  const admin: TenantAdminUser = {
    id: 10,
    email: 'ada@example.com',
    firstName: 'Ada',
    lastName: 'Lovelace',
  };

  const tenants: Tenant[] = [
    { id: 0, name: 'PLATFORM', isActive: true },
    { id: 1, name: 'Default Tenant', isActive: true },
    { id: 5, name: 'Live Ltd', isActive: true },
  ];

  beforeEach(() => {
    tenantService = jasmine.createSpyObj<TenantService>('TenantService', [
      'getActiveTenants',
      'moveTenantAdmin',
    ]);
    tenantService.getActiveTenants.and.returnValue(of(tenants));
    dialogRef = jasmine.createSpyObj<MatDialogRef<MoveTenantAdminDialogComponent>>('MatDialogRef', [
      'close',
    ]);

    TestBed.configureTestingModule({
      imports: [MoveTenantAdminDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: TenantService, useValue: tenantService },
        { provide: MatDialogRef, useValue: dialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            tenantId: 1,
            tenantName: 'Default Tenant',
            admin,
          },
        },
      ],
    });

    const fixture = TestBed.createComponent(MoveTenantAdminDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('ngOnInit_offersActiveTenants_exceptPlatformAndCurrent', () => {
    expect(component.targetTenants.map((t) => t.id)).toEqual([5]);
  });

  it('onSubmit_withTarget_movesAdminAndClosesWithTrue', () => {
    tenantService.moveTenantAdmin.and.returnValue(of(admin));
    component.targetTenantId.setValue(5);

    component.onSubmit();

    expect(tenantService.moveTenantAdmin).toHaveBeenCalledWith(1, 10, 5);
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('onSubmit_withoutTarget_doesNotCallTheApi', () => {
    component.onSubmit();

    expect(tenantService.moveTenantAdmin).not.toHaveBeenCalled();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('onSubmit_apiError_showsServerMessageAndStaysOpen', () => {
    tenantService.moveTenantAdmin.and.returnValue(
      throwError(() => ({ error: { message: 'Property company is deactivated' } }))
    );
    component.targetTenantId.setValue(5);

    component.onSubmit();

    expect(component.serverError).toBe('Property company is deactivated');
    expect(component.isLoading).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });
});
