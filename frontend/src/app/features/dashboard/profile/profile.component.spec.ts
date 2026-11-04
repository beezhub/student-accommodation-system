import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { ProfileComponent } from './profile.component';
import { AuthService } from '../../../core/services/auth.service';
import { InstitutionService } from '../../../core/services/institution.service';
import { StudentService } from '../../../core/services/student.service';
import { YearOfStudyService } from '../../../core/services/year-of-study.service';

describe('ProfileComponent', () => {
  const profile = {
    id: 7,
    firstName: 'Ann',
    lastName: 'Lee',
    email: 'ann@example.com',
    phoneNumber: '0821234567',
    studentNumber: 'S1',
    institutionId: 1,
    yearOfStudyId: 2,
    dateOfBirth: '2000-05-14',
  };

  let studentService: jasmine.SpyObj<StudentService>;

  function setUp(): ProfileComponent {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AuthService, useValue: { currentUserValue: { id: '1' } } },
        { provide: InstitutionService, useValue: { getInstitutions: () => of([]) } },
        { provide: YearOfStudyService, useValue: { getYearsOfStudy: () => of([]) } },
        { provide: StudentService, useValue: studentService },
      ],
    });
    return TestBed.createComponent(ProfileComponent).componentInstance;
  }

  beforeEach(() => {
    studentService = jasmine.createSpyObj<StudentService>('StudentService', [
      'getStudentProfile',
      'updateStudentProfile',
    ]);
  });

  /** #464: a date of birth has no time and no zone, so it must not pass through UTC. */
  describe('date of birth', () => {
    beforeEach(() => {
      studentService.getStudentProfile.and.returnValue(of(profile) as never);
      studentService.updateStudentProfile.and.returnValue(of(profile) as never);
    });

    it('ngOnInit_apiDate_becomesLocalMidnightOfThatDay', () => {
      const component = setUp();

      component.ngOnInit();

      expect(component.profileForm.value.dateOfBirth).toEqual(new Date(2000, 4, 14));
    });

    /**
     * Answers as a browser in South Africa does, whatever zone runs this test: local midnight on
     * the 14th is 22:00 UTC on the 13th.
     */
    it('onSubmit_pickedDate_isSentAsThatCalendarDay', () => {
      const component = setUp();
      component.ngOnInit();
      component.profileForm.patchValue({ dateOfBirth: new Date(2000, 4, 14) });
      spyOn(Date.prototype, 'toISOString').and.returnValue('2000-05-13T22:00:00.000Z');

      component.onSubmit();

      const sent = studentService.updateStudentProfile.calls.mostRecent().args[0];
      expect(sent.dateOfBirth).toBe('2000-05-14');
    });

    it('toggleEdit_cancellingAnEdit_restoresTheSameDay', () => {
      const component = setUp();
      component.ngOnInit();
      component.isEditing = true;
      component.profileForm.patchValue({ dateOfBirth: new Date(1999, 0, 1) });

      component.toggleEdit();

      expect(component.profileForm.value.dateOfBirth).toEqual(new Date(2000, 4, 14));
    });

    it('formatDateForDisplay_apiDate_showsThatDay', () => {
      const component = setUp();

      expect(component.formatDateForDisplay('2000-05-14')).toContain('14');
    });
  });
});
