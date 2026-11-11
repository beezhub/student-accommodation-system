import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';

import { ApplicationWizardService, WizardStep4 } from './application-wizard.service';
import { ApplicationService } from './application.service';
import { StudentService } from './student.service';
import { User } from '../models/user.model';

describe('ApplicationWizardService', () => {
  const user = { firstName: 'Ann', lastName: 'Lee', email: 'a@b.com' } as User;
  const step: WizardStep4 = {
    studentId: 7,
    institutionId: 1,
    yearOfStudyId: 2,
    institutionName: 'UCT',
    studentNumber: 'S1',
    phoneNumber: '0821234567',
    dateOfBirth: new Date(2000, 4, 14),
    gender: 'Female',
    courseName: 'BSc',
    requiresAccessibility: true,
    preferredBuildingIds: [5],
    specialRequirements: null,
  };

  let studentService: jasmine.SpyObj<StudentService>;
  let applicationService: jasmine.SpyObj<ApplicationService>;
  let service: ApplicationWizardService;

  beforeEach(() => {
    studentService = jasmine.createSpyObj('StudentService', ['updateStudentProfile']);
    applicationService = jasmine.createSpyObj('ApplicationService', ['createApplication']);
    studentService.updateStudentProfile.and.returnValue(of({}) as never);
    applicationService.createApplication.and.returnValue(of([{ id: 42 }]) as never);
    TestBed.configureTestingModule({
      providers: [
        { provide: StudentService, useValue: studentService },
        { provide: ApplicationService, useValue: applicationService },
      ],
    });
    service = TestBed.inject(ApplicationWizardService);
  });

  it('submit_valid_savesTheProfileThenCreatesTheApplication', () => {
    let created: unknown;
    service.submit(user, step).subscribe((apps) => (created = apps));

    const profile = studentService.updateStudentProfile.calls.mostRecent().args[0];
    expect(profile.studentNumber).toBe('S1');
    expect(profile.gender).toBe('Female');
    expect(profile.institutionId).toBe(1);
    const application = applicationService.createApplication.calls.mostRecent().args[0];
    expect(application.courseName).toBe('BSc');
    expect(application.requiresAccessibility).toBeTrue();
    expect(application.preferredBuildingIds).toEqual([5]);
    expect(created).toEqual([{ id: 42 }]);
  });

  /** Answers as a browser in South Africa does: local midnight on the 14th is the 13th in UTC. */
  it('submit_pickedDate_isSentAsThatCalendarDay', () => {
    spyOn(Date.prototype, 'toISOString').and.returnValue('2000-05-13T22:00:00.000Z');

    service.submit(user, step).subscribe();

    expect(studentService.updateStudentProfile.calls.mostRecent().args[0].dateOfBirth).toBe(
      '2000-05-14'
    );
  });

  it('submit_noBuildingsChosen_sendsNone', () => {
    service.submit(user, { ...step, preferredBuildingIds: [] }).subscribe();

    expect(
      applicationService.createApplication.calls.mostRecent().args[0].preferredBuildingIds
    ).toBeUndefined();
  });

  it('submit_profileUpdateFails_doesNotCreateTheApplication', () => {
    studentService.updateStudentProfile.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })) as never
    );
    let failed = false;

    service.submit(user, step).subscribe({ error: () => (failed = true) });

    expect(failed).toBeTrue();
    expect(applicationService.createApplication).not.toHaveBeenCalled();
  });
});
