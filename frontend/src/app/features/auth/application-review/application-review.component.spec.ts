import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { NEVER, of, throwError } from 'rxjs';

import { ApplicationReviewComponent } from './application-review.component';
import {
  ApplicationDraft,
  ApplicationDraftService,
} from '../../../core/services/application-draft.service';
import { ApplicationService } from '../../../core/services/application.service';
import { AuthService } from '../../../core/services/auth.service';
import { ErrorHandlingService } from '../../../core/services/error-handling.service';
import { InstitutionService } from '../../../core/services/institution.service';
import { StudentService } from '../../../core/services/student.service';
import { YearOfStudyService } from '../../../core/services/year-of-study.service';
import { StudentResponse } from '../../../core/models/student.model';
import { User } from '../../../core/models/user.model';

describe('ApplicationReviewComponent', () => {
  const user = { firstName: 'Ann', lastName: 'Lee', email: 'a@b.com' } as User;
  const failure = (status = 500) => throwError(() => new HttpErrorResponse({ status }));

  const draftWith = (dateOfBirth: Date | string): ApplicationDraft => ({
    studentId: 7,
    requiresAccessibility: false,
    preferredBuildingIds: [],
    specialRequirements: null,
    institutionName: 'UCT',
    courseName: 'BSc',
    studentNumber: 'S1',
    phoneNumber: '0821234567',
    dateOfBirth,
    gender: 'female',
  });

  let authService: { currentUserValue: User | null };
  let studentService: jasmine.SpyObj<StudentService>;
  let applicationService: jasmine.SpyObj<ApplicationService>;
  let institutionService: jasmine.SpyObj<InstitutionService>;
  let yearOfStudyService: jasmine.SpyObj<YearOfStudyService>;
  let draftService: ApplicationDraftService;
  let navigate: jasmine.Spy;
  let showWarning: jasmine.Spy;
  let showErrorSnackbar: jasmine.Spy;
  let component: ApplicationReviewComponent;

  beforeEach(() => {
    localStorage.clear();
    authService = { currentUserValue: user };
    studentService = jasmine.createSpyObj('StudentService', [
      'getStudentProfile',
      'updateStudentProfile',
    ]);
    applicationService = jasmine.createSpyObj('ApplicationService', ['createApplication']);
    institutionService = jasmine.createSpyObj('InstitutionService', ['getInstitutions']);
    yearOfStudyService = jasmine.createSpyObj('YearOfStudyService', ['getYearsOfStudy']);

    studentService.getStudentProfile.and.returnValue(
      of({ institutionId: 1, yearOfStudyId: 2 }) as never
    );
    studentService.updateStudentProfile.and.returnValue(NEVER);
    institutionService.getInstitutions.and.returnValue(of([{ id: 1, name: 'UCT' }]) as never);
    yearOfStudyService.getYearsOfStudy.and.returnValue(
      of([{ id: 2, yearOfStudy: 'Second' }]) as never
    );

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AuthService, useValue: authService },
        { provide: StudentService, useValue: studentService },
        { provide: ApplicationService, useValue: applicationService },
        { provide: InstitutionService, useValue: institutionService },
        { provide: YearOfStudyService, useValue: yearOfStudyService },
      ],
    });
    draftService = TestBed.inject(ApplicationDraftService);
    navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const errorService = TestBed.inject(ErrorHandlingService);
    showWarning = spyOn(errorService, 'showWarning');
    showErrorSnackbar = spyOn(errorService, 'showErrorSnackbar');
    spyOn(errorService, 'showSuccess');
    component = TestBed.createComponent(ApplicationReviewComponent).componentInstance;
  });

  afterEach(() => localStorage.clear());

  /** Puts the screen where the student is when they press submit. */
  function readyToSubmit(draft: ApplicationDraft = draftWith('2000-05-14')): void {
    component.currentUser = user;
    component.studentProfile = { institutionId: 1, yearOfStudyId: 2 } as StudentResponse;
    component.termsAccepted = true;
    component.draft = draft;
  }

  /**
   * #464: the review step sent the draft's date as it found it, an instant in UTC, so a student in
   * South Africa who picked the 14th had the 13th saved.
   */
  describe('date of birth', () => {
    const sentDate = () =>
      studentService.updateStudentProfile.calls.mostRecent().args[0].dateOfBirth;

    it('onSubmit_draftWithACalendarDay_sendsItUnchanged', () => {
      readyToSubmit(draftWith('2000-05-14'));

      component.onSubmit();

      expect(sentDate()).toBe('2000-05-14');
    });

    /** A draft saved before this fix is still sitting in the student's localStorage. */
    it('onSubmit_olderDraftHoldingAnInstant_sendsTheDayTheStudentPicked', () => {
      readyToSubmit(draftWith(new Date(2000, 4, 14).toISOString()));

      component.onSubmit();

      expect(sentDate()).toBe('2000-05-14');
    });
  });

  describe('loading', () => {
    it('ngOnInit_signedOut_goesToLogin', () => {
      authService.currentUserValue = null;

      component.ngOnInit();

      expect(navigate).toHaveBeenCalledWith(['/login']);
    });

    it('ngOnInit_noDraft_goesBackToTheForm', () => {
      component.ngOnInit();

      expect(showWarning).toHaveBeenCalled();
      expect(navigate).toHaveBeenCalledWith(['/application-submission']);
    });

    it('ngOnInit_draftPresent_loadsWhatTheScreenShows', () => {
      draftService.saveDraft(draftWith('2000-05-14'));

      component.ngOnInit();

      expect(component.draft?.courseName).toBe('BSc');
      expect(component.getInstitutionName()).toBe('UCT');
      expect(component.getYearOfStudyName()).toBe('Second');
    });

    it('ngOnInit_profileFails_goesBackToTheForm', () => {
      draftService.saveDraft(draftWith('2000-05-14'));
      studentService.getStudentProfile.and.returnValue(failure() as never);

      component.ngOnInit();

      expect(navigate).toHaveBeenCalledWith(['/application-submission']);
    });

    it('ngOnInit_referenceDataFails_warnsAndStays', () => {
      draftService.saveDraft(draftWith('2000-05-14'));
      institutionService.getInstitutions.and.returnValue(failure() as never);
      yearOfStudyService.getYearsOfStudy.and.returnValue(failure() as never);

      component.ngOnInit();

      expect(showWarning).toHaveBeenCalledTimes(2);
      expect(navigate).not.toHaveBeenCalled();
    });

    it('names_nothingLoadedOrNoMatch_sayNotAvailable', () => {
      expect(component.getInstitutionName()).toBe('N/A');
      expect(component.getYearOfStudyName()).toBe('N/A');

      component.studentProfile = { institutionId: 99, yearOfStudyId: 99 } as StudentResponse;

      expect(component.getInstitutionName()).toBe('N/A');
      expect(component.getYearOfStudyName()).toBe('N/A');
    });
  });

  describe('submitting', () => {
    beforeEach(() => {
      studentService.updateStudentProfile.and.returnValue(of({}) as never);
    });

    it('onSubmit_termsNotAccepted_doesNothing', () => {
      readyToSubmit();
      component.termsAccepted = false;

      component.onSubmit();

      expect(studentService.updateStudentProfile).not.toHaveBeenCalled();
    });

    it('onSubmit_profileHasNoInstitution_warnsAndDoesNotSubmit', () => {
      readyToSubmit();
      component.studentProfile = {} as StudentResponse;

      component.onSubmit();

      expect(showWarning).toHaveBeenCalled();
      expect(studentService.updateStudentProfile).not.toHaveBeenCalled();
    });

    it('onSubmit_allGood_clearsTheDraftAndConfirms', () => {
      applicationService.createApplication.and.returnValue(of([{ id: 42 }]) as never);
      draftService.saveDraft(draftWith('2000-05-14'));
      readyToSubmit();

      component.onSubmit();

      expect(draftService.getDraft()).toBeNull();
      expect(component.isSubmitting).toBeFalse();
      expect(navigate).toHaveBeenCalledWith(['/application-confirmation', 42]);
    });

    it('onSubmit_preferredBuildings_areSentWhenChosen', () => {
      applicationService.createApplication.and.returnValue(of([{ id: 42 }]) as never);
      readyToSubmit({ ...draftWith('2000-05-14'), preferredBuildingIds: [5] });

      component.onSubmit();

      const sent = applicationService.createApplication.calls.mostRecent().args[0] as unknown as {
        preferredBuildingIds: number[];
      };
      expect(sent.preferredBuildingIds).toEqual([5]);
    });

    it('onSubmit_profileUpdateFails_showsTheErrorAndStops', () => {
      studentService.updateStudentProfile.and.returnValue(failure() as never);
      readyToSubmit();

      component.onSubmit();

      expect(component.submitError?.type).toBe('server');
      expect(showErrorSnackbar).toHaveBeenCalled();
      expect(applicationService.createApplication).not.toHaveBeenCalled();
    });

    it('onSubmit_serverRefusesTheApplication_showsTheSnackbar', () => {
      applicationService.createApplication.and.returnValue(failure() as never);
      readyToSubmit();

      component.onSubmit();

      expect(component.isSubmitting).toBeFalse();
      expect(showErrorSnackbar).toHaveBeenCalled();
    });

    it('onSubmit_validationError_staysInline', () => {
      applicationService.createApplication.and.returnValue(failure(400) as never);
      readyToSubmit();

      component.onSubmit();

      expect(component.submitError?.type).toBe('validation');
      expect(showErrorSnackbar).not.toHaveBeenCalled();
    });

    it('retrySubmit_submitsAgain', () => {
      applicationService.createApplication.and.returnValue(of([{ id: 42 }]) as never);
      readyToSubmit();

      component.retrySubmit();

      expect(applicationService.createApplication).toHaveBeenCalled();
    });

    it('dismissError_clearsTheError', () => {
      component.submitError = { type: 'general', title: 'x', message: 'y', canRetry: true };

      component.dismissError();

      expect(component.submitError).toBeNull();
    });
  });
});
