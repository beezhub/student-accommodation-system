import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { BuildingManagementComponent } from './building-management.component';
import { BuildingResponse, BuildingService } from '../../../core/services/building.service';
import { BuildingAccessibilityDialogComponent } from './building-accessibility-dialog/building-accessibility-dialog.component';

describe('BuildingManagementComponent', () => {
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
