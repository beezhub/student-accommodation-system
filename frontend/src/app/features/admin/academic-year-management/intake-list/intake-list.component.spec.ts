import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';

import { IntakeListComponent } from './intake-list.component';
import { AcademicYear } from '../../../../core/models/academic-year.model';
import { AcademicYearService } from '../../../../core/services/academic-year.service';

describe('IntakeListComponent', () => {
  const year: AcademicYear = {
    id: 5,
    name: '2027',
    startDate: '2027-02-01',
    endDate: '2027-11-30',
    applicationOpenDate: '2026-10-04',
    applicationCloseDate: '2027-01-18',
    isApplicationWindowOpen: true,
    intakes: [
      {
        id: 10,
        academicYearId: 5,
        intakeType: 'ANNUAL',
        name: 'Annual',
        stayStartDate: '2027-02-01',
        stayEndDate: '2027-11-30',
        applicationOpenDate: '2026-10-04',
        applicationCloseDate: '2027-01-18',
        isApplicationWindowOpen: true,
      },
    ],
  };

  let service: jasmine.SpyObj<AcademicYearService>;
  let fixture: ComponentFixture<IntakeListComponent>;

  beforeEach(() => {
    service = jasmine.createSpyObj('AcademicYearService', {
      createIntake: of({ ...year.intakes![0], id: 11, name: 'Semester 2' }),
      updateIntake: of({ ...year.intakes![0], applicationCount: 3, allocationCount: 1 }),
      closeApplicationsNow: of(year.intakes![0]),
      deleteIntake: of(undefined),
    });
    TestBed.configureTestingModule({
      imports: [IntakeListComponent],
      providers: [
        provideNoopAnimations(),
        { provide: AcademicYearService, useValue: service },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(true) }) } },
      ],
    });
    fixture = TestBed.createComponent(IntakeListComponent);
    fixture.componentRef.setInput('year', year);
    fixture.componentRef.setInput('canManage', true);
    fixture.detectChanges();
  });

  it('closeNow_confirmed_closesApplicationsAndTellsThePage', () => {
    const changed = spyOn(fixture.componentInstance.changed, 'emit');

    (fixture.nativeElement.querySelector('.intake-close') as HTMLButtonElement).click();

    expect(service.closeApplicationsNow).toHaveBeenCalledWith(10);
    expect(changed).toHaveBeenCalled();
  });

  it('delete_confirmed_deletesTheIntake', () => {
    (fixture.nativeElement.querySelector('.intake-delete') as HTMLButtonElement).click();

    expect(service.deleteIntake).toHaveBeenCalledWith(10);
  });

  it('save_newIntakeWithoutWindow_leavesTheWindowToTheServer', () => {
    const component = fixture.componentInstance;
    component.startAdd();
    component.form.patchValue({
      name: 'Semester 2',
      stayStartDate: '2027-07-15',
      stayEndDate: '2027-11-30',
    });

    component.save();

    expect(service.createIntake).toHaveBeenCalledWith(5, {
      intakeType: 'SEMESTER',
      name: 'Semester 2',
      stayStartDate: '2027-07-15',
      stayEndDate: '2027-11-30',
      applicationOpenDate: undefined,
      applicationCloseDate: undefined,
    });
    expect(component.editing).toBeNull();
  });

  it('save_edit_sendsTheChangedDates', () => {
    const component = fixture.componentInstance;
    component.startEdit(year.intakes![0]);
    component.form.patchValue({ applicationCloseDate: '2027-01-31' });

    component.save();

    expect(service.updateIntake).toHaveBeenCalledWith(
      10,
      jasmine.objectContaining({ applicationCloseDate: '2027-01-31' })
    );
  });

  it('save_invalidForm_sendsNothing', () => {
    const component = fixture.componentInstance;
    component.startAdd();
    component.form.patchValue({ name: '' });

    component.save();

    expect(service.createIntake).not.toHaveBeenCalled();
  });
});
