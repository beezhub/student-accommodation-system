import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { BuildingAccessibilityDialogComponent } from './building-accessibility-dialog.component';
import { BuildingAttributesService } from '../../../../core/services/building-attributes.service';

/**
 * #305: the only building attribute still in use. The features appear as chips on the public
 * building page, which reads a JSON list. Gender is managed on rooms.
 */
describe('BuildingAccessibilityDialogComponent', () => {
  let fixture: ComponentFixture<BuildingAccessibilityDialogComponent>;
  let component: BuildingAccessibilityDialogComponent;
  let service: jasmine.SpyObj<BuildingAttributesService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<BuildingAccessibilityDialogComponent>>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  function create(stored?: string | null, loadFails = false): void {
    service = jasmine.createSpyObj<BuildingAttributesService>('BuildingAttributesService', [
      'getBuildingAttributes',
      'saveAccessibilityFeatures',
    ]);
    service.getBuildingAttributes.and.returnValue(
      loadFails
        ? throwError(() => new HttpErrorResponse({ status: 500 }))
        : of({ buildingId: 7, accessibilityFeatures: stored })
    );
    service.saveAccessibilityFeatures.and.returnValue(of({ buildingId: 7 }));
    dialogRef = jasmine.createSpyObj<MatDialogRef<BuildingAccessibilityDialogComponent>>(
      'MatDialogRef',
      ['close']
    );
    snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    TestBed.configureTestingModule({
      imports: [BuildingAccessibilityDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: BuildingAttributesService, useValue: service },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { building: { id: 7, name: 'Hall Alpha' } } },
      ],
    });
    // The component imports MatSnackBarModule itself, so the spy has to be an override.
    TestBed.overrideProvider(MatSnackBar, { useValue: snackBar });
    fixture = TestBed.createComponent(BuildingAccessibilityDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('showsTheBuildingsSavedFeatures', () => {
    create('["Ramp access","Lift"]');

    expect(service.getBuildingAttributes).toHaveBeenCalledWith(7);
    expect(component.features).toEqual(['Ramp access', 'Lift']);
    expect(text()).toContain('Hall Alpha');
    expect(text()).toContain('Ramp access');
  });

  it('nothingSaved_saysThePublicPageHidesTheSection', () => {
    create(null);

    expect(component.features).toEqual([]);
    expect(text()).toContain('the public page hides its Accessibility section');
  });

  it('adds_aTrimmedFeature_andIgnoresBlanksAndRepeats', () => {
    create('["Lift"]');

    component.addFeature('  Ramp access  ');
    component.addFeature('   ');
    component.addFeature('lift');

    expect(component.features).toEqual(['Lift', 'Ramp access']);
  });

  it('removesAFeature', () => {
    create('["Lift","Ramp access"]');

    component.removeFeature('Lift');

    expect(component.features).toEqual(['Ramp access']);
  });

  it('cannotSave_untilSomethingChanges', () => {
    create('["Lift"]');

    expect(component.canSave).toBeFalse();
    component.addFeature('Ramp access');
    expect(component.canSave).toBeTrue();
  });

  /** Found by driving the dialog: Save stayed disabled for text typed without pressing Enter. */
  it('textTypedButNotYetAdded_enablesSave_andIsSaved', () => {
    create('["Lift"]');

    component.onType('Ramp access');
    expect(component.canSave).toBeTrue();
    component.save();

    expect(service.saveAccessibilityFeatures).toHaveBeenCalledWith(7, ['Lift', 'Ramp access']);
  });

  it('onlySpacesTyped_doesNotEnableSave', () => {
    create('["Lift"]');

    component.onType('   ');

    expect(component.canSave).toBeFalse();
  });

  it('saves_tellsTheAdmin_andCloses', () => {
    create('["Lift"]');
    component.addFeature('Ramp access');

    component.save();

    expect(service.saveAccessibilityFeatures).toHaveBeenCalledWith(7, ['Lift', 'Ramp access']);
    expect(snackBar.open).toHaveBeenCalledWith(
      'Accessibility features saved for Hall Alpha.',
      'Close',
      jasmine.objectContaining({ panelClass: 'snack-success' })
    );
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('removingTheLastFeature_canBeSaved_soTheyCanBeCleared', () => {
    create('["Lift"]');
    component.removeFeature('Lift');

    component.save();

    expect(service.saveAccessibilityFeatures).toHaveBeenCalledWith(7, []);
  });

  it('showsTheServersReason_andStaysOpen_whenTheSaveFails', () => {
    create('["Lift"]');
    service.saveAccessibilityFeatures.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { errors: ['Accessibility features must be a list of text.'] },
          })
      )
    );
    component.addFeature('Ramp access');

    component.save();
    fixture.detectChanges();

    expect(text()).toContain('Accessibility features must be a list of text.');
    expect(component.isSaving).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  /** An empty form saved after a failed load would wipe the features it failed to show. */
  it('neverSaves_whenTheFeaturesCouldNotBeLoaded', () => {
    create(undefined, true);

    component.addFeature('Ramp access');
    component.save();
    fixture.detectChanges();

    expect(text()).toContain("This building's accessibility features could not be loaded.");
    expect(component.canSave).toBeFalse();
    expect(service.saveAccessibilityFeatures).not.toHaveBeenCalled();
  });

  it('treatsAStoredValueThatIsNotAList_asNoFeatures', () => {
    create('not json');

    expect(component.features).toEqual([]);
  });
});
