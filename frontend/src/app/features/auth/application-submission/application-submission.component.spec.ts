import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { ApplicationSubmissionComponent } from './application-submission.component';
import { ApplicationDraftService } from '../../../core/services/application-draft.service';
import { ApplicationService } from '../../../core/services/application.service';
import { AuthService } from '../../../core/services/auth.service';
import { BuildingService } from '../../../core/services/building.service';
import { DocumentService } from '../../../core/services/document.service';
import { DocumentTypeService } from '../../../core/services/document-type.service';
import { ErrorHandlingService } from '../../../core/services/error-handling.service';
import { InstitutionService } from '../../../core/services/institution.service';
import { StudentService } from '../../../core/services/student.service';
import { User } from '../../../core/models/user.model';

describe('ApplicationSubmissionComponent', () => {
  const building = { id: 5, name: 'Liesbeek House', tenantId: 3 };
  const failure = (status = 500) => throwError(() => new HttpErrorResponse({ status }));

  let authService: { currentUserValue: Partial<User> | null };
  let applicationService: jasmine.SpyObj<ApplicationService>;
  let buildingService: jasmine.SpyObj<BuildingService>;
  let documentService: jasmine.SpyObj<DocumentService>;
  let documentTypeService: jasmine.SpyObj<DocumentTypeService>;
  let institutionService: jasmine.SpyObj<InstitutionService>;
  let studentService: jasmine.SpyObj<StudentService>;
  let navigate: jasmine.Spy;
  let showWarning: jasmine.Spy;
  let component: ApplicationSubmissionComponent;

  beforeEach(() => {
    localStorage.clear();
    authService = { currentUserValue: { emailVerified: true } };
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
      ],
    });
    navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    showWarning = spyOn(TestBed.inject(ErrorHandlingService), 'showWarning');
    component = TestBed.createComponent(ApplicationSubmissionComponent).componentInstance;
  });

  afterEach(() => localStorage.clear());

  function fillInTheForm(): void {
    component.studentId = 7;
    component.currentUser = { emailVerified: true } as User;
    component.applicationForm.patchValue({
      studentNumber: 'S1',
      phoneNumber: '0821234567',
      dateOfBirth: new Date(2000, 4, 14),
      gender: 'female',
      courseName: 'BSc',
    });
  }

  /**
   * #464: the picked date went into the draft through JSON.stringify, which converts it to UTC, so
   * a student in South Africa who picked the 14th had the 13th saved.
   */
  describe('date of birth', () => {
    it('loadStudentProfile_apiDate_becomesLocalMidnightOfThatDay', () => {
      component.loadStudentProfile();

      expect(component.applicationForm.value.dateOfBirth).toEqual(new Date(2000, 4, 14));
    });

    /**
     * Answers as a browser in South Africa does, whatever zone runs this test: local midnight on
     * the 14th is 22:00 UTC on the 13th.
     */
    it('onSubmit_pickedDate_isSavedInTheDraftAsThatCalendarDay', () => {
      fillInTheForm();
      spyOn(Date.prototype, 'toISOString').and.returnValue('2000-05-13T22:00:00.000Z');

      component.onSubmit();

      expect(TestBed.inject(ApplicationDraftService).getDraft()?.dateOfBirth).toBe('2000-05-14');
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
          gender: 'female',
          specialRequirements: 'Ground floor',
        }) as never
      );

      component.loadStudentProfile();

      expect(component.applicationForm.value.studentNumber).toBe('S1');
      expect(component.applicationForm.value.phoneNumber).toBe('0821234567');
      expect(component.applicationForm.value.gender).toBe('female');
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

    it('onBuildingSelectionChanged_cleared_goesBackToTheDefaultDocuments', () => {
      component.selectedTenantId = 3;

      component.onBuildingSelectionChanged([]);

      expect(component.selectedTenantId).toBeNull();
      expect(documentTypeService.getRequiredDocumentTypes).toHaveBeenCalledWith(undefined);
    });
  });

  describe('submitting', () => {
    it('onSubmit_validForm_savesTheDraftAndGoesToReview', () => {
      fillInTheForm();

      component.onSubmit();

      expect(TestBed.inject(ApplicationDraftService).getDraft()?.courseName).toBe('BSc');
      expect(navigate).toHaveBeenCalledWith(['/application-review']);
    });

    it('onSubmit_noStudentId_goesToProfileSetup', () => {
      component.onSubmit();

      expect(navigate).toHaveBeenCalledWith(['/profile-setup']);
    });

    it('onSubmit_emailNotVerified_goesToVerify', () => {
      fillInTheForm();
      component.currentUser = { emailVerified: false } as User;

      component.onSubmit();

      expect(navigate).toHaveBeenCalledWith(['/email-verification-required']);
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

      expect(navigate).toHaveBeenCalledWith(['/application-review']);
    });

    it('dismissError_clearsTheError', () => {
      component.submitError = { type: 'general', title: 'x', message: 'y', canRetry: true };

      component.dismissError();

      expect(component.submitError).toBeNull();
    });
  });
});
