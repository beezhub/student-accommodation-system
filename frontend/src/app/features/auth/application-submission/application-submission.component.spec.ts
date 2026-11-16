import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { ApplicationSubmissionComponent } from './application-submission.component';
import { ApplicationWizardService } from '../../../core/services/application-wizard.service';
import { ApplicationService } from '../../../core/services/application.service';
import { AuthService } from '../../../core/services/auth.service';
import { BuildingService } from '../../../core/services/building.service';
import { DocumentService } from '../../../core/services/document.service';
import { DocumentTypeService } from '../../../core/services/document-type.service';
import { ErrorHandlingService } from '../../../core/services/error-handling.service';
import { InstitutionService } from '../../../core/services/institution.service';
import { StudentService } from '../../../core/services/student.service';
import { StudentResponse } from '../../../core/models/student.model';
import { User } from '../../../core/models/user.model';

describe('ApplicationSubmissionComponent', () => {
  const building = { id: 5, name: 'Liesbeek House', tenantId: 3 };
  const failure = (status = 500) => throwError(() => new HttpErrorResponse({ status }));

  let authService: {
    currentUserValue: Partial<User> | null;
    refreshCurrentUser: jasmine.Spy;
  };
  let applicationService: jasmine.SpyObj<ApplicationService>;
  let buildingService: jasmine.SpyObj<BuildingService>;
  let documentService: jasmine.SpyObj<DocumentService>;
  let documentTypeService: jasmine.SpyObj<DocumentTypeService>;
  let institutionService: jasmine.SpyObj<InstitutionService>;
  let studentService: jasmine.SpyObj<StudentService>;
  let wizardService: jasmine.SpyObj<ApplicationWizardService>;
  let showErrorSnackbar: jasmine.Spy;
  let navigate: jasmine.Spy;
  let showWarning: jasmine.Spy;
  let component: ApplicationSubmissionComponent;

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    authService = {
      currentUserValue: { emailVerified: true },
      refreshCurrentUser: jasmine.createSpy('refreshCurrentUser'),
    };
    applicationService = jasmine.createSpyObj('ApplicationService', ['hasActiveApplication']);
    buildingService = jasmine.createSpyObj('BuildingService', [
      'getBuildingsByInstitution',
      'getAllBuildings',
      'searchBuildings',
    ]);
    documentService = jasmine.createSpyObj('DocumentService', ['getUploadedDocuments']);
    documentTypeService = jasmine.createSpyObj('DocumentTypeService', ['getRequiredDocumentTypes']);
    institutionService = jasmine.createSpyObj('InstitutionService', ['getInstitutions']);
    studentService = jasmine.createSpyObj('StudentService', ['getStudentProfile']);
    wizardService = jasmine.createSpyObj('ApplicationWizardService', ['submit']);
    wizardService.submit.and.returnValue(of([{ id: 42 }, { id: 43 }]) as never);

    applicationService.hasActiveApplication.and.returnValue(of(false) as never);
    buildingService.getBuildingsByInstitution.and.returnValue(of([building]) as never);
    buildingService.getAllBuildings.and.returnValue(of({ content: [building] }) as never);
    documentService.getUploadedDocuments.and.returnValue(of([]) as never);
    documentTypeService.getRequiredDocumentTypes.and.returnValue(of([]) as never);
    institutionService.getInstitutions.and.returnValue(of([{ id: 1, name: 'UCT' }]) as never);
    studentService.getStudentProfile.and.returnValue(
      of({ id: 7, dateOfBirth: '2000-05-14' }) as never
    );

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AuthService, useValue: authService },
        { provide: ApplicationService, useValue: applicationService },
        { provide: BuildingService, useValue: buildingService },
        { provide: DocumentService, useValue: documentService },
        { provide: DocumentTypeService, useValue: documentTypeService },
        { provide: InstitutionService, useValue: institutionService },
        { provide: StudentService, useValue: studentService },
        { provide: ApplicationWizardService, useValue: wizardService },
      ],
    });
    navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const errorService = TestBed.inject(ErrorHandlingService);
    showWarning = spyOn(errorService, 'showWarning');
    showErrorSnackbar = spyOn(errorService, 'showErrorSnackbar');
    spyOn(errorService, 'showSuccess');
    component = TestBed.createComponent(ApplicationSubmissionComponent).componentInstance;
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  function fillInTheForm(): void {
    component.studentId = 7;
    component.currentUser = { emailVerified: true } as User;
    component.studentProfile = { institutionId: 1, yearOfStudyId: 2 } as StudentResponse;
    component.applicationForm.patchValue({
      termsAccepted: true,
      studentNumber: 'S1',
      phoneNumber: '0821234567',
      dateOfBirth: new Date(2000, 4, 14),
      gender: 'Female',
      courseName: 'BSc',
    });
  }

  /**
   * #464: a student in South Africa who picked the 14th had the 13th saved. Sending the day is
   * covered in application-wizard.service.spec.ts.
   */
  describe('date of birth', () => {
    it('loadStudentProfile_apiDate_becomesLocalMidnightOfThatDay', () => {
      component.loadStudentProfile();

      expect(component.applicationForm.value.dateOfBirth).toEqual(new Date(2000, 4, 14));
    });
  });

  describe('loading', () => {
    it('ngOnInit_signedOut_goesToLogin', () => {
      authService.currentUserValue = null;

      component.ngOnInit();

      expect(navigate).toHaveBeenCalledWith(['/login']);
    });

    it('ngOnInit_activeApplication_goesToTheDashboard', () => {
      applicationService.hasActiveApplication.and.returnValue(of(true) as never);

      component.ngOnInit();

      expect(navigate).toHaveBeenCalledWith(['/dashboard']);
      expect(studentService.getStudentProfile).not.toHaveBeenCalled();
    });

    it('ngOnInit_noActiveApplication_loadsTheProfile', () => {
      component.ngOnInit();

      expect(component.studentId).toBe(7);
    });

    it('ngOnInit_documentsMissing_listsThemMatchingByName', () => {
      documentTypeService.getRequiredDocumentTypes.and.returnValue(
        of([
          { id: 1, name: 'ID Copy' },
          { id: 2, name: 'Proof of Registration' },
        ]) as never
      );
      documentService.getUploadedDocuments.and.returnValue(of([{ typeName: 'id copy' }]) as never);

      component.ngOnInit();

      expect(component.hasAllRequiredDocs).toBeFalse();
      expect(component.missingRequiredDocs.map((d) => d.name)).toEqual(['Proof of Registration']);
      expect(component.isLoadingDocReadiness).toBeFalse();
    });

    it('ngOnInit_documentListFails_doesNotBlockTheForm', () => {
      documentService.getUploadedDocuments.and.returnValue(failure() as never);

      component.ngOnInit();

      expect(component.isLoadingDocReadiness).toBeFalse();
      expect(component.loadError).toBeNull();
    });

    it('loadStudentProfile_existingAnswers_fillInTheForm', () => {
      studentService.getStudentProfile.and.returnValue(
        of({
          id: 7,
          studentNumber: 'S1',
          phoneNumber: '0821234567',
          gender: 'Female',
          specialRequirements: 'Ground floor',
        }) as never
      );

      component.loadStudentProfile();

      expect(component.applicationForm.value.studentNumber).toBe('S1');
      expect(component.applicationForm.value.phoneNumber).toBe('0821234567');
      expect(component.applicationForm.value.gender).toBe('Female');
      expect(component.applicationForm.value.specialRequirements).toBe('Ground floor');
    });

    it('loadStudentProfile_studentAtAnInstitution_showsItAndItsBuildings', () => {
      studentService.getStudentProfile.and.returnValue(of({ id: 7, institutionId: 1 }) as never);

      component.loadStudentProfile();

      expect(component.institutionName).toBe('UCT');
      expect(buildingService.getBuildingsByInstitution).toHaveBeenCalledWith(1);
      expect(component.buildings).toEqual([building] as never);
    });

    it('loadStudentProfile_institutionHasNoBuildings_showsAllBuildings', () => {
      studentService.getStudentProfile.and.returnValue(of({ id: 7, institutionId: 1 }) as never);
      buildingService.getBuildingsByInstitution.and.returnValue(of([]) as never);

      component.loadStudentProfile();

      expect(buildingService.getAllBuildings).toHaveBeenCalled();
    });

    it('loadStudentProfile_institutionLookupFails_warnsAndStillLoadsBuildings', () => {
      studentService.getStudentProfile.and.returnValue(of({ id: 7, institutionId: 1 }) as never);
      institutionService.getInstitutions.and.returnValue(failure() as never);

      component.loadStudentProfile();

      expect(showWarning).toHaveBeenCalled();
      expect(component.buildings).toEqual([building] as never);
    });

    it('loadStudentProfile_noProfile_goesToProfileSetup', () => {
      studentService.getStudentProfile.and.returnValue(failure(404) as never);

      component.loadStudentProfile();

      expect(navigate).toHaveBeenCalledWith(['/profile-setup']);
    });

    it('loadStudentProfile_serverError_staysAndShowsTheError', () => {
      studentService.getStudentProfile.and.returnValue(failure() as never);

      component.loadStudentProfile();

      expect(navigate).not.toHaveBeenCalled();
      expect(component.loadError?.statusCode).toBe(500);
      expect(component.isRetrying).toBeFalse();
    });

    it('retryLoadProfile_asksForTheProfileAgain', () => {
      component.retryLoadProfile();

      expect(studentService.getStudentProfile).toHaveBeenCalledTimes(1);
    });

    it('loadBuildings_institutionFilterFails_warnsAndShowsAllBuildings', () => {
      component.studentInstitutionId = 1;
      buildingService.getBuildingsByInstitution.and.returnValue(failure() as never);

      component.loadBuildings();

      expect(showWarning).toHaveBeenCalled();
      expect(buildingService.getAllBuildings).toHaveBeenCalled();
    });

    it('loadBuildings_allBuildingsFail_warns', () => {
      buildingService.getAllBuildings.and.returnValue(failure() as never);

      component.loadBuildings();

      expect(showWarning).toHaveBeenCalled();
    });
  });

  describe('building search', () => {
    it('onBuildingSearch_emptyQuery_reloadsTheList', () => {
      component.onBuildingSearch('');

      expect(buildingService.searchBuildings).not.toHaveBeenCalled();
      expect(buildingService.getAllBuildings).toHaveBeenCalled();
    });

    it('onBuildingSearch_query_showsTheMatches', () => {
      const match = { id: 6, name: 'Match' };
      buildingService.searchBuildings.and.returnValue(of({ content: [match] }) as never);

      component.onBuildingSearch('mat');

      expect(component.buildings).toEqual([match] as never);
    });

    it('onBuildingSearch_searchFails_warns', () => {
      buildingService.searchBuildings.and.returnValue(failure() as never);

      component.onBuildingSearch('mat');

      expect(showWarning).toHaveBeenCalled();
    });
  });

  describe('choosing a building', () => {
    beforeEach(() => {
      component.buildings = [building] as never;
    });

    it('onBuildingSelectionChanged_newCompany_reloadsItsRequiredDocuments', () => {
      component.onBuildingSelectionChanged([5]);

      expect(component.applicationForm.value.preferredBuildingIds).toEqual([5]);
      expect(component.selectedTenantId).toBe(3);
      expect(documentTypeService.getRequiredDocumentTypes).toHaveBeenCalledWith(3);
    });

    it('onBuildingSelectionChanged_sameCompany_doesNotReload', () => {
      component.selectedTenantId = 3;

      component.onBuildingSelectionChanged([5]);

      expect(documentTypeService.getRequiredDocumentTypes).not.toHaveBeenCalled();
    });

    // #443: the Documents page offers a company's own types only for buildings it is told about
    it('onBuildingSelectionChanged_documentMissing_uploadLinksCarryThePickedBuildings', () => {
      documentTypeService.getRequiredDocumentTypes.and.returnValue(
        of([{ id: 9, name: 'Bursary Letter' }]) as never
      );
      const fixture = TestBed.createComponent(ApplicationSubmissionComponent);
      fixture.detectChanges();

      fixture.componentInstance.onBuildingSelectionChanged([5, 6]);
      fixture.detectChanges();

      const links = (fixture.nativeElement as HTMLElement).querySelectorAll('.doc-readiness a');
      expect(links.length).toBe(2);
      links.forEach((link) => expect(link.getAttribute('href')).toBe('/documents?buildings=5,6'));
    });

    it('onBuildingSelectionChanged_cleared_goesBackToTheDefaultDocuments', () => {
      component.selectedTenantId = 3;

      component.onBuildingSelectionChanged([]);

      expect(component.selectedTenantId).toBeNull();
      expect(documentTypeService.getRequiredDocumentTypes).toHaveBeenCalledWith(undefined);
    });
  });

  /** #550: a refresh before Continue to Review emptied every field on step 4. */
  describe('keeping answers across a refresh', () => {
    const student = { id: '9', emailVerified: true } as User;

    function typeAnswers(): void {
      component.applicationForm.patchValue({
        studentNumber: 'S9',
        phoneNumber: '0821234567',
        dateOfBirth: new Date(2004, 2, 14),
        gender: 'Female',
        courseName: 'BSc Computer Science',
        requiresAccessibility: true,
        preferredBuildingIds: [5],
        termsAccepted: true,
      });
    }

    function refresh(): void {
      component = TestBed.createComponent(ApplicationSubmissionComponent).componentInstance;
      component.currentUser = student;
      component.loadStudentProfile();
    }

    it('loadStudentProfile_answersTypedBeforeARefresh_comeBack', () => {
      component.currentUser = student;
      component.loadStudentProfile();
      typeAnswers();

      refresh();

      const v = component.applicationForm.value;
      expect(v.studentNumber).toBe('S9');
      expect(v.dateOfBirth).toEqual(new Date(2004, 2, 14));
      expect(v.gender).toBe('Female');
      expect(v.courseName).toBe('BSc Computer Science');
      expect(v.requiresAccessibility).toBeTrue();
      expect(v.preferredBuildingIds).toEqual([5]);
    });

    it('loadStudentProfile_afterARefresh_theConfirmationIsNotKept', () => {
      component.currentUser = student;
      component.loadStudentProfile();
      typeAnswers();

      refresh();

      expect(component.applicationForm.value.termsAccepted).toBeFalse();
    });

    it('loadStudentProfile_keptAnswers_winOverTheSavedProfile', () => {
      component.currentUser = student;
      component.loadStudentProfile();
      typeAnswers();
      studentService.getStudentProfile.and.returnValue(
        of({ id: 7, studentNumber: 'OLD', dateOfBirth: '2000-05-14' }) as never
      );

      refresh();

      expect(component.applicationForm.value.studentNumber).toBe('S9');
      expect(component.applicationForm.value.dateOfBirth).toEqual(new Date(2004, 2, 14));
    });

    it('loadStudentProfile_anotherStudentsAnswers_areNotShown', () => {
      component.currentUser = student;
      component.loadStudentProfile();
      typeAnswers();

      component = TestBed.createComponent(ApplicationSubmissionComponent).componentInstance;
      component.currentUser = { id: '10', emailVerified: true } as User;
      component.loadStudentProfile();

      expect(component.applicationForm.value.courseName).toBe('');
    });

    it('onSubmit_applicationCreated_forgetsTheKeptAnswers', () => {
      component.currentUser = student;
      component.loadStudentProfile();
      typeAnswers();
      component.studentProfile = { institutionId: 1, yearOfStudyId: 2 } as StudentResponse;

      component.onSubmit();
      refresh();

      expect(component.applicationForm.value.courseName).toBe('');
    });

    it('onSubmit_serverFails_keepsTheAnswers', () => {
      wizardService.submit.and.returnValue(failure() as never);
      component.currentUser = student;
      component.loadStudentProfile();
      typeAnswers();
      component.studentProfile = { institutionId: 1, yearOfStudyId: 2 } as StudentResponse;

      component.onSubmit();
      refresh();

      expect(component.applicationForm.value.courseName).toBe('BSc Computer Science');
    });
  });

  describe('submitting', () => {
    it('onSubmit_validForm_savesAndShowsWhatWasSaved', () => {
      fillInTheForm();

      component.onSubmit();

      const step = wizardService.submit.calls.mostRecent().args[1];
      expect(step.courseName).toBe('BSc');
      expect(step.institutionId).toBe(1);
      expect(component.isSubmitting).toBeFalse();
      expect(navigate).toHaveBeenCalledWith(['/application-review'], {
        queryParams: { ids: '42,43' },
      });
    });

    it('onSubmit_termsNotAccepted_doesNotSave', () => {
      fillInTheForm();
      component.applicationForm.patchValue({ termsAccepted: false });

      component.onSubmit();

      expect(wizardService.submit).not.toHaveBeenCalled();
      expect(showWarning).toHaveBeenCalled();
    });

    it('onSubmit_profileHasNoInstitution_warnsAndDoesNotSave', () => {
      fillInTheForm();
      component.studentProfile = {} as StudentResponse;

      component.onSubmit();

      expect(wizardService.submit).not.toHaveBeenCalled();
      expect(showWarning).toHaveBeenCalled();
    });

    it('onSubmit_serverFails_showsTheErrorAndStays', () => {
      wizardService.submit.and.returnValue(failure() as never);
      fillInTheForm();

      component.onSubmit();

      expect(component.isSubmitting).toBeFalse();
      expect(component.submitError?.type).toBe('server');
      expect(showErrorSnackbar).toHaveBeenCalled();
      expect(navigate).not.toHaveBeenCalled();
    });

    it('onSubmit_validationError_staysInline', () => {
      wizardService.submit.and.returnValue(failure(400) as never);
      fillInTheForm();

      component.onSubmit();

      expect(component.submitError?.type).toBe('validation');
      expect(showErrorSnackbar).not.toHaveBeenCalled();
    });

    it('onSubmit_noStudentId_goesToProfileSetup', () => {
      component.onSubmit();

      expect(navigate).toHaveBeenCalledWith(['/profile-setup']);
    });

    /**
     * The stored user only learns about a verification done in this browser. A student who opened
     * the link on their phone stayed "unverified" here, so step 4 sent them to the verify page on
     * every click and never saved the application. The server is asked before turning them away.
     */
    it('onSubmit_verifiedOnAnotherDevice_asksTheServerAndSaves', () => {
      fillInTheForm();
      component.currentUser = { emailVerified: false } as User;
      authService.refreshCurrentUser.and.callFake(() => {
        authService.currentUserValue = { emailVerified: true };
        return of({ emailVerified: true });
      });

      component.onSubmit();

      expect(authService.refreshCurrentUser).toHaveBeenCalled();
      expect(wizardService.submit).toHaveBeenCalled();
      expect(navigate).not.toHaveBeenCalledWith(
        ['/email-verification-required'],
        jasmine.anything()
      );
    });

    it('onSubmit_serverSaysNotVerified_goesToVerifyAndBackHere', () => {
      fillInTheForm();
      component.currentUser = { emailVerified: false } as User;
      authService.refreshCurrentUser.and.returnValue(of({ emailVerified: false }));

      component.onSubmit();

      expect(wizardService.submit).not.toHaveBeenCalled();
      expect(component.isSubmitting).toBeFalse();
      expect(navigate).toHaveBeenCalledWith(['/email-verification-required'], {
        queryParams: { returnUrl: '/application-submission' },
      });
    });

    it('onSubmit_verificationCheckFails_showsTheErrorAndStays', () => {
      fillInTheForm();
      component.currentUser = { emailVerified: false } as User;
      authService.refreshCurrentUser.and.returnValue(failure(0));

      component.onSubmit();

      expect(wizardService.submit).not.toHaveBeenCalled();
      expect(component.isSubmitting).toBeFalse();
      expect(component.submitError).not.toBeNull();
      expect(navigate).not.toHaveBeenCalled();
    });

    it('onSubmit_storedCopySaysVerified_doesNotAskTheServer', () => {
      fillInTheForm();

      component.onSubmit();

      expect(authService.refreshCurrentUser).not.toHaveBeenCalled();
      expect(wizardService.submit).toHaveBeenCalled();
    });

    it('onSubmit_incompleteForm_warnsAndStays', () => {
      component.studentId = 7;
      component.currentUser = { emailVerified: true } as User;

      component.onSubmit();

      expect(component.applicationForm.get('courseName')?.touched).toBeTrue();
      expect(navigate).not.toHaveBeenCalled();
    });

    it('retrySubmit_submitsAgain', () => {
      fillInTheForm();

      component.retrySubmit();

      expect(wizardService.submit).toHaveBeenCalled();
    });

    it('dismissError_clearsTheError', () => {
      component.submitError = { type: 'general', title: 'x', message: 'y', canRetry: true };

      component.dismissError();

      expect(component.submitError).toBeNull();
    });
  });
});
