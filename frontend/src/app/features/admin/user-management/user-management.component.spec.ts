import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { MatSelect } from '@angular/material/select';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { UserManagementComponent } from './user-management.component';
import { AdminService } from '../../../core/services/admin.service';
import { RoleService } from '../../../core/services/role.service';
import { TenantService } from '../../../core/services/tenant.service';

describe('UserManagementComponent', () => {
  it('roleFilter_everyOption_isARoleTheApiAccepts', () => {
    TestBed.configureTestingModule({
      imports: [UserManagementComponent],
      providers: [
        provideNoopAnimations(),
        {
          provide: AdminService,
          useValue: jasmine.createSpyObj('AdminService', {
            getAllUsers: of({ content: [], totalElements: 0 }),
          }),
        },
        {
          provide: RoleService,
          useValue: jasmine.createSpyObj('RoleService', { isPlatformAdmin: true }),
        },
        {
          provide: TenantService,
          useValue: jasmine.createSpyObj('TenantService', { getAllTenants: of([]) }),
        },
      ],
    });
    const fixture = TestBed.createComponent(UserManagementComponent);
    fixture.componentInstance.filtersVisible = true;
    fixture.detectChanges();

    const select = fixture.debugElement.query(By.directive(MatSelect))
      .componentInstance as MatSelect;

    // These are the display names UserRole.enumRoleOf matches on the backend. LANDLORD made it
    // answer 409, and ADMIN found only deprecated accounts.
    expect(select.options.map((option) => option.value)).toEqual([
      'all',
      'Platform_Admin',
      'Platform_Staff',
      'Tenant_Admin',
      'Building_Staff',
      'Student',
    ]);
  });
});
