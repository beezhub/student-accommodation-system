import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { MatDialog } from '@angular/material/dialog';

import { ProfileComponent } from './profile.component';
import { AuthService } from '../../../core/services/auth.service';
import { ErrorHandlingService } from '../../../core/services/error-handling.service';
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
  let authService: {
    currentUserValue: { id: string };
    endSessionAfterPasswordChange: jasmine.Spy;
    logout: jasmine.Spy;
  };

  function setUp(): ProfileComponent {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AuthService, useValue: authService },
        { provide: InstitutionService, useValue: { getInstitutions: () => of([]) } },
        { provide: YearOfStudyService, useValue: { getYearsOfStudy: () => of([]) } },
        { provide: StudentService, useValue: studentService },
      ],
    });
    return TestBed.createComponent(ProfileComponent).componentInstance;
  }

  beforeEach(() => {
    authService = {
      currentUserValue: { id: '1' },
      endSessionAfterPasswordChange: jasmine.createSpy('endSessionAfterPasswordChange'),
      logout: jasmine.createSpy('logout'),
    };
    studentService = jasmine.createSpyObj<StudentService>('StudentService', [
      'getStudentProfile',
      'updateStudentProfile',
    ]);
  });

  describe('showing and saving', () => {
    const failure = (status = 500) => throwError(() => new HttpErrorResponse({ status }));
    let component: ProfileComponent;
    let navigate: jasmine.Spy;
    let showWarning: jasmine.Spy;
    let showErrorSnackbar: jasmine.Spy;

    beforeEach(() => {
      studentService.getStudentProfile.and.returnValue(of(profile) as never);
      studentService.updateStudentProfile.and.returnValue(of(profile) as never);
      component = setUp();
      navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
      const errorService = TestBed.inject(ErrorHandlingService);
      showWarning = spyOn(errorService, 'showWarning');
      showErrorSnackbar = spyOn(errorService, 'showErrorSnackbar');
      spyOn(errorService, 'showSuccess');
      spyOn(errorService, 'showInfo');
    });

    [
      ['MALE', 'Male'],
      ['female', 'Female'],
      ['OTHER', ''],
      ['prefer-not-to-say', ''],
    ].forEach(([stored, shown]) => {
      it(`ngOnInit_gender_${stored}_isShownAs_${shown || 'empty'}`, () => {
        studentService.getStudentProfile.and.returnValue(
          of({ ...profile, gender: stored }) as never
        );

        component.ngOnInit();

        expect(component.profileForm.value.gender).toBe(shown);
      });
    });

    it('ngOnInit_referenceDataFails_warnsTwice', () => {
      TestBed.inject(InstitutionService).getInstitutions = () => failure() as never;
      TestBed.inject(YearOfStudyService).getYearsOfStudy = () => failure() as never;

      component.ngOnInit();

      expect(showWarning).toHaveBeenCalledTimes(2);
      expect(component.institutions).toEqual([]);
      expect(component.yearsOfStudy).toEqual([]);
    });

    it('retryLoadProfile_loadsEverythingAgain', () => {
      component.ngOnInit();
      studentService.getStudentProfile.calls.reset();

      component.retryLoadProfile();

      expect(studentService.getStudentProfile).toHaveBeenCalled();
    });

    it('names_nothingLoaded_sayLoading', () => {
      expect(component.getInstitutionName('1')).toBe('Loading...');
      expect(component.getYearOfStudyName('2')).toBe('Loading...');
    });

    it('names_loaded_findTheMatchOrSayNotFound', () => {
      component.institutions = [{ id: 1, name: 'UCT' }] as never;
      component.yearsOfStudy = [{ id: 2, yearOfStudy: 'Second' }] as never;

      expect(component.getInstitutionName('1')).toBe('UCT');
      expect(component.getYearOfStudyName('2')).toBe('Second');
      expect(component.getInstitutionName('9')).toBe('Not found');
      expect(component.getYearOfStudyName('9')).toBe('Not found');
    });

    it('formatDateForDisplay_nothingOrNonsense_saysSo', () => {
      expect(component.formatDateForDisplay(null)).toBe('Not specified');
      expect(component.formatDateForDisplay('nonsense')).toBe('Invalid date');
    });

    it('onSubmit_saved_leavesEditingAndKeepsTheResponse', () => {
      component.ngOnInit();
      component.isEditing = true;

      component.onSubmit();

      expect(component.isEditing).toBeFalse();
      expect(component.isSaving).toBeFalse();
      expect(component.currentUser?.email).toBe(profile.email);
    });

    it('onSubmit_clearedEmergencyContact_isSentAsEmptyToRemoveIt', () => {
      component.ngOnInit();

      component.onSubmit();

      const sent = studentService.updateStudentProfile.calls.mostRecent().args[0];
      expect(sent.emergencyContactName).toBe('');
    });

    it('onSubmit_incompleteForm_marksFieldsAndWarns', () => {
      component.onSubmit();

      expect(component.profileForm.get('firstName')?.touched).toBeTrue();
      expect(showWarning).toHaveBeenCalled();
      expect(studentService.updateStudentProfile).not.toHaveBeenCalled();
    });

    it('onSubmit_serverError_showsTheSnackbar', () => {
      studentService.updateStudentProfile.and.returnValue(failure() as never);
      component.ngOnInit();

      component.onSubmit();

      expect(component.isSaving).toBeFalse();
      expect(showErrorSnackbar).toHaveBeenCalled();
    });

    it('onSubmit_validationError_staysInline', () => {
      studentService.updateStudentProfile.and.returnValue(failure(400) as never);
      component.ngOnInit();

      component.onSubmit();

      expect(component.saveError?.type).toBe('validation');
      expect(showErrorSnackbar).not.toHaveBeenCalled();
    });

    it('retrySave_savesAgain', () => {
      component.ngOnInit();

      component.retrySave();

      expect(studentService.updateStudentProfile).toHaveBeenCalled();
    });

    it('dismissSaveError_clearsTheError', () => {
      component.saveError = { type: 'general', title: 'x', message: 'y', canRetry: true };

      component.dismissSaveError();

      expect(component.saveError).toBeNull();
    });

    /** GH-291: the change blacklists the token, so the browser's copy is already dead. */
    it('openChangePasswordDialog_changed_endsTheSessionAndGoesToLogin', () => {
      dialogClosesWith('success');

      component.openChangePasswordDialog();

      expect(authService.endSessionAfterPasswordChange).toHaveBeenCalled();
      expect(navigate).toHaveBeenCalledWith(['/login'], {
        queryParams: { passwordChanged: 'true' },
      });
    });

    it('openChangePasswordDialog_cancelled_staysSignedIn', () => {
      dialogClosesWith(undefined);

      component.openChangePasswordDialog();

      expect(authService.endSessionAfterPasswordChange).not.toHaveBeenCalled();
    });

    it('openDeleteAccountDialog_deleted_signsOutAndGoesToLogin', () => {
      dialogClosesWith('deleted');

      component.openDeleteAccountDialog();

      expect(authService.logout).toHaveBeenCalled();
      expect(navigate).toHaveBeenCalledWith(['/login']);
    });

    it('openDeleteAccountDialog_cancelled_staysSignedIn', () => {
      dialogClosesWith(undefined);

      component.openDeleteAccountDialog();

      expect(authService.logout).not.toHaveBeenCalled();
    });

    function dialogClosesWith(result: string | undefined): void {
      spyOn(MatDialog.prototype, 'open').and.returnValue({
        afterClosed: () => of(result),
      } as never);
    }
  });

  /** #465: this screen can only update a row, so a student without one must go and create it. */
  describe('with no student profile', () => {
    let navigate: jasmine.Spy;

    const failingWith = (status: number) =>
      throwError(() => new HttpErrorResponse({ status, error: { errors: ['Nope'] } }));

    function setUpWithRouterSpy(): ProfileComponent {
      const component = setUp();
      navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
      return component;
    }

    it('ngOnInit_noProfile_goesToProfileSetup', () => {
      studentService.getStudentProfile.and.returnValue(failingWith(404));
      const component = setUpWithRouterSpy();

      component.ngOnInit();

      expect(navigate).toHaveBeenCalledOnceWith(['/profile-setup'], { replaceUrl: true });
    });

    it('ngOnInit_serverError_staysAndShowsTheError', () => {
      studentService.getStudentProfile.and.returnValue(failingWith(500));
      const component = setUpWithRouterSpy();

      component.ngOnInit();

      expect(navigate).not.toHaveBeenCalled();
      expect(component.loadError?.statusCode).toBe(500);
    });
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
