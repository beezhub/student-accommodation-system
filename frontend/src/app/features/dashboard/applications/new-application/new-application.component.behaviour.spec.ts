import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { NewApplicationComponent } from './new-application.component';
import { ApplicationService } from '../../../../core/services/application.service';
import { AuthService } from '../../../../core/services/auth.service';
import { BuildingService } from '../../../../core/services/building.service';
import { ErrorHandlingService } from '../../../../core/services/error-handling.service';
import { InstitutionService } from '../../../../core/services/institution.service';
import { StudentService } from '../../../../core/services/student.service';

describe('NewApplicationComponent', () => {
  const uct = { id: 1, name: 'UCT' };
  const building = { id: 5, name: 'Liesbeek House' };
  const failure = () => throwError(() => new HttpErrorResponse({ status: 500 }));

  let authService: { currentUserValue: { id: string } | null };
  let applicationService: jasmine.SpyObj<ApplicationService>;
  let buildingService: jasmine.SpyObj<BuildingService>;
  let institutionService: jasmine.SpyObj<InstitutionService>;
  let studentService: jasmine.SpyObj<StudentService>;
  let navigate: jasmine.Spy;
  let showWarning: jasmine.Spy;
  let showSuccess: jasmine.Spy;
  let component: NewApplicationComponent;

  beforeEach(() => {
    authService = { currentUserValue: { id: '1' } };
    applicationService = jasmine.createSpyObj('ApplicationService', ['createApplication']);
    buildingService = jasmine.createSpyObj('BuildingService', [
      'getBuildingsByInstitution',
      'getAllBuildingsNoPagination',
      'searchBuildings',
    ]);
    institutionService = jasmine.createSpyObj('InstitutionService', ['getInstitutions']);
    studentService = jasmine.createSpyObj('StudentService', ['getStudentProfile']);

    buildingService.getBuildingsByInstitution.and.returnValue(of([building]) as never);
    buildingService.getAllBuildingsNoPagination.and.returnValue(of([building]) as never);
    institutionService.getInstitutions.and.returnValue(of([uct]) as never);
    studentService.getStudentProfile.and.returnValue(of({ id: 7, institutionId: 1 }) as never);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AuthService, useValue: authService },
        { provide: ApplicationService, useValue: applicationService },
        { provide: BuildingService, useValue: buildingService },
        { provide: InstitutionService, useValue: institutionService },
        { provide: StudentService, useValue: studentService },
      ],
    });
    navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const errorService = TestBed.inject(ErrorHandlingService);
    showWarning = spyOn(errorService, 'showWarning');
    showSuccess = spyOn(errorService, 'showSuccess');
    component = TestBed.createComponent(NewApplicationComponent).componentInstance;
  });

  it('ngOnInit_signedOut_goesToLogin', () => {
    authService.currentUserValue = null;

    component.ngOnInit();

    expect(navigate).toHaveBeenCalledWith(['/login']);
    expect(studentService.getStudentProfile).not.toHaveBeenCalled();
  });

  it('ngOnInit_studentAtAnInstitution_showsItAndItsBuildings', () => {
    component.ngOnInit();

    expect(component.studentId).toBe(7);
    expect(component.institutionName).toBe('UCT');
    expect(buildingService.getBuildingsByInstitution).toHaveBeenCalledWith(1);
    expect(component.buildings).toEqual([building] as never);
  });

  it('ngOnInit_studentWithNoInstitution_showsAllBuildings', () => {
    studentService.getStudentProfile.and.returnValue(of({ id: 7 }) as never);

    component.ngOnInit();

    expect(institutionService.getInstitutions).not.toHaveBeenCalled();
    expect(buildingService.getAllBuildingsNoPagination).toHaveBeenCalled();
  });

  it('ngOnInit_institutionLookupFails_warnsAndStillLoadsBuildings', () => {
    institutionService.getInstitutions.and.returnValue(failure() as never);

    component.ngOnInit();

    expect(showWarning).toHaveBeenCalled();
    expect(component.buildings).toEqual([building] as never);
  });

  it('loadBuildings_institutionFilterFails_warnsAndShowsAllBuildings', () => {
    buildingService.getBuildingsByInstitution.and.returnValue(failure() as never);

    component.ngOnInit();

    expect(showWarning).toHaveBeenCalled();
    expect(buildingService.getAllBuildingsNoPagination).toHaveBeenCalled();
  });

  it('loadBuildings_allBuildingsFail_warns', () => {
    studentService.getStudentProfile.and.returnValue(of({ id: 7 }) as never);
    buildingService.getAllBuildingsNoPagination.and.returnValue(failure() as never);

    component.ngOnInit();

    expect(showWarning).toHaveBeenCalled();
    expect(component.buildings).toEqual([]);
  });

  it('retryLoadProfile_asksForTheProfileAgain', () => {
    component.retryLoadProfile();

    expect(studentService.getStudentProfile).toHaveBeenCalledTimes(1);
  });

  it('onSubmit_validForm_submitsAndGoesToApplications', () => {
    applicationService.createApplication.and.returnValue(of({}) as never);
    component.ngOnInit();
    component.applicationForm.patchValue({ courseName: 'BSc' });

    component.onSubmit();

    const sent = applicationService.createApplication.calls.mostRecent().args[0];
    expect(sent.studentId).toBe(7 as never);
    expect(sent.institutionName).toBe('UCT');
    expect(showSuccess).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/applications']);
  });

  it('onSubmit_incompleteForm_warnsAndDoesNotSubmit', () => {
    component.ngOnInit();

    component.onSubmit();

    expect(applicationService.createApplication).not.toHaveBeenCalled();
    expect(component.applicationForm.get('courseName')?.touched).toBeTrue();
    expect(showWarning).toHaveBeenCalled();
  });

  it('retrySubmit_submitsAgain', () => {
    applicationService.createApplication.and.returnValue(of({}) as never);
    component.ngOnInit();
    component.applicationForm.patchValue({ courseName: 'BSc' });

    component.retrySubmit();

    expect(applicationService.createApplication).toHaveBeenCalled();
  });

  it('dismissError_clearsTheError', () => {
    component.submitError = { type: 'general', title: 'x', message: 'y', canRetry: true };

    component.dismissError();

    expect(component.submitError).toBeNull();
  });

  it('onBuildingSearch_emptyQuery_reloadsTheList', () => {
    studentService.getStudentProfile.and.returnValue(of({ id: 7 }) as never);
    component.ngOnInit();
    buildingService.getAllBuildingsNoPagination.calls.reset();

    component.onBuildingSearch('');

    expect(buildingService.searchBuildings).not.toHaveBeenCalled();
    expect(buildingService.getAllBuildingsNoPagination).toHaveBeenCalled();
  });

  it('onBuildingSearch_query_showsTheMatches', () => {
    const match = { id: 6, name: 'Match' };
    buildingService.searchBuildings.and.returnValue(of({ content: [match] }) as never);

    component.onBuildingSearch('mat');

    expect(buildingService.searchBuildings).toHaveBeenCalledWith({ name: 'mat', size: 10 });
    expect(component.buildings).toEqual([match] as never);
  });

  it('onBuildingSearch_searchFails_warns', () => {
    buildingService.searchBuildings.and.returnValue(failure() as never);

    component.onBuildingSearch('mat');

    expect(showWarning).toHaveBeenCalled();
  });

  it('onBuildingSelectionChanged_updatesTheForm', () => {
    component.onBuildingSelectionChanged([5, 6]);

    expect(component.applicationForm.value.preferredBuildingIds).toEqual([5, 6]);
  });
});
