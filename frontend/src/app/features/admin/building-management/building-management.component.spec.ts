import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { BuildingManagementComponent } from './building-management.component';
import { BuildingResponse, BuildingService } from '../../../core/services/building.service';
import { RoleService } from '../../../core/services/role.service';
import { BuildingAccessibilityDialogComponent } from './building-accessibility-dialog/building-accessibility-dialog.component';

describe('BuildingManagementComponent', () => {
  function renderAs(role: { isPlatformStaff: boolean }): HTMLElement {
    TestBed.configureTestingModule({
      imports: [BuildingManagementComponent],
      providers: [
        provideNoopAnimations(),
        {
          provide: BuildingService,
          useValue: jasmine.createSpyObj('BuildingService', {
            searchBuildings: of({ content: [], totalElements: 0 }),
          }),
        },
        { provide: RoleService, useValue: jasmine.createSpyObj('RoleService', role) },
      ],
    });
    const fixture = TestBed.createComponent(BuildingManagementComponent);
    fixture.detectChanges();
    return fixture.nativeElement;
  }

  // Staff cannot pick a company, and the backend refuses a platform-level caller who names none.
  it('addBuilding_platformStaff_isNotOffered', () => {
    expect(renderAs({ isPlatformStaff: true }).querySelector('.add-btn')).toBeNull();
  });

  it('addBuilding_otherAdmins_isOffered', () => {
    expect(renderAs({ isPlatformStaff: false }).querySelector('.add-btn')).not.toBeNull();
  });

  it('openAccessibilityDialog_opensTheDialogForThatBuilding', async () => {
    const dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    TestBed.configureTestingModule({
      imports: [BuildingManagementComponent],
      providers: [
        provideNoopAnimations(),
        {
          provide: BuildingService,
          useValue: jasmine.createSpyObj('BuildingService', {
            getAllBuildings: of({ content: [] }),
          }),
        },
        {
          provide: RoleService,
          useValue: jasmine.createSpyObj('RoleService', ['isPlatformStaff']),
        },
      ],
    });
    // The component imports MatDialogModule itself, so the spy has to be an override.
    TestBed.overrideProvider(MatDialog, { useValue: dialog });
    const component = TestBed.createComponent(BuildingManagementComponent).componentInstance;
    const building = { id: 7, name: 'Hall Alpha' } as BuildingResponse;

    await component.openAccessibilityDialog(building);

    expect(dialog.open).toHaveBeenCalledWith(
      BuildingAccessibilityDialogComponent,
      jasmine.objectContaining({ data: { building } })
    );
  });
});
