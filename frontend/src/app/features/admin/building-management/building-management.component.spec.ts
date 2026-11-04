import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

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

  describe('deleteBuilding', () => {
    function setUp(deleteError: HttpErrorResponse) {
      const snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
      const dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
      dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
      TestBed.configureTestingModule({
        imports: [BuildingManagementComponent],
        providers: [
          provideNoopAnimations(),
          {
            provide: BuildingService,
            useValue: jasmine.createSpyObj('BuildingService', {
              deleteBuilding: throwError(() => deleteError),
            }),
          },
        ],
      });
      TestBed.overrideProvider(MatDialog, { useValue: dialog });
      TestBed.overrideProvider(MatSnackBar, { useValue: snackBar });
      const component = TestBed.createComponent(BuildingManagementComponent).componentInstance;
      return { component, snackBar, dialog };
    }

    it('deleteBuilding_refusedByTheServer_showsTheServerMessage', () => {
      const message = 'Block A cannot be deleted while its rooms hold 2 live allocations.';
      const { component, snackBar } = setUp(
        new HttpErrorResponse({ status: 409, error: { errors: [message] } })
      );

      component.deleteBuilding({ id: 7, name: 'Block A' } as BuildingResponse);

      expect(snackBar.open).toHaveBeenCalledWith(message, 'Close', jasmine.any(Object));
    });

    it('deleteBuilding_confirmation_warnsThatTheRoomsGoToo', () => {
      const { component, dialog } = setUp(new HttpErrorResponse({ status: 0 }));

      component.deleteBuilding({ id: 7, name: 'Block A' } as BuildingResponse);

      const config = dialog.open.calls.mostRecent().args[1];
      expect((config?.data as { message: string }).message).toContain('its rooms');
    });
  });
});
