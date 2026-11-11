import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';

import {
  AcademicYearDialogData,
  AcademicYearFormDialogComponent,
} from './academic-year-form-dialog.component';
import { AcademicYear } from '../../../../core/models/academic-year.model';
import { AcademicYearService } from '../../../../core/services/academic-year.service';
import { InstitutionService } from '../../../../core/services/institution.service';

describe('AcademicYearFormDialogComponent', () => {
  const saved: AcademicYear = {
    id: 5,
    name: '2027',
    startDate: '2027-02-01',
    endDate: '2027-11-30',
    isApplicationWindowOpen: false,
    intakes: [],
  };

  let service: jasmine.SpyObj<AcademicYearService>;
  let fixture: ComponentFixture<AcademicYearFormDialogComponent>;
  let component: AcademicYearFormDialogComponent;

  function build(data: AcademicYearDialogData): void {
    service = jasmine.createSpyObj('AcademicYearService', {
      createFromPattern: of(saved),
      updateAcademicYear: of(saved),
    });
    TestBed.configureTestingModule({
      imports: [AcademicYearFormDialogComponent],
      providers: [
        provideNoopAnimations(),
        provideNativeDateAdapter(),
        { provide: AcademicYearService, useValue: service },
        {
          provide: InstitutionService,
          useValue: { getInstitutions: () => of([{ id: 7, name: 'North-West University' }]) },
        },
        { provide: MatDialogRef, useValue: { close: jasmine.createSpy('close') } },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    });
    TestBed.overrideProvider(MatSnackBar, {
      useValue: jasmine.createSpyObj('MatSnackBar', ['open']),
    });
    fixture = TestBed.createComponent(AcademicYearFormDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('create_pickedDates_sendsLocalCalendarDays', () => {
    build({ mode: 'pattern' });
    component.form.patchValue({
      name: '2027',
      startDate: new Date(2027, 1, 1),
      endDate: new Date(2027, 10, 30),
      semesters: true,
    });

    component.onSubmit();

    expect(service.createFromPattern).toHaveBeenCalledWith({
      institutionId: null,
      name: '2027',
      startDate: '2027-02-01',
      endDate: '2027-11-30',
      intakeTypes: ['ANNUAL', 'SEMESTER'],
    });
  });

  it('edit_existingYear_showsItsDatesAndSendsThemBack', () => {
    build({ mode: 'edit', academicYear: saved });

    expect(component.form.controls.startDate.value).toEqual(new Date(2027, 1, 1));

    component.onSubmit();

    expect(service.updateAcademicYear).toHaveBeenCalledWith(5, {
      institutionId: null,
      name: '2027',
      startDate: '2027-02-01',
      endDate: '2027-11-30',
    });
  });

  it('dates_endBeforeStart_isRefusedWithAReason', () => {
    build({ mode: 'pattern' });
    component.form.patchValue({
      name: '2027',
      startDate: new Date(2027, 10, 30),
      endDate: new Date(2027, 1, 1),
    });
    component.form.markAllAsTouched();
    fixture.detectChanges();

    expect(component.canSubmit).toBeFalse();
    expect(text()).toContain('The last move-out day must be after the first move-in day');
  });

  it('dates_leftEmpty_sayWhatIsMissing', () => {
    build({ mode: 'pattern' });
    component.form.markAllAsTouched();
    fixture.detectChanges();

    expect(text()).toContain('Pick the first move-in day');
    expect(text()).toContain('Pick the last move-out day');
  });

  it('dates_useTheCalendarPicker', () => {
    build({ mode: 'pattern' });

    expect(fixture.nativeElement.querySelectorAll('mat-datepicker-toggle').length).toBe(2);
    expect(fixture.nativeElement.querySelector('input[type="date"]')).toBeNull();
  });

  it('intakeOptions_sayWhatEachOneCreates', () => {
    build({ mode: 'pattern' });

    expect(text()).toContain('1 intake for the whole year');
    expect(text()).toContain('2 intakes, each half of the year');
    expect(text()).toContain('3 intakes, each a third of the year');
  });

  it('preview_datesAndIntakesPicked_listsWhatWillBeCreated', () => {
    build({ mode: 'pattern' });
    component.form.patchValue({
      startDate: new Date(2027, 1, 1),
      endDate: new Date(2027, 10, 30),
      annual: false,
      semesters: true,
    });
    fixture.detectChanges();

    const rows = [...fixture.nativeElement.querySelectorAll('.preview-row')].map((row) => [
      (row as HTMLElement).querySelector('.preview-name')?.textContent?.trim(),
      (row as HTMLElement).querySelector('.preview-dates')?.textContent?.trim(),
    ]);
    expect(rows).toEqual([
      ['Semester 1', '1 February to 1 July'],
      ['Semester 2', '2 July to 30 November'],
    ]);
  });
});
