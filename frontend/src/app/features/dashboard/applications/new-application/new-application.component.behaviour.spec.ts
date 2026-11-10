import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { NewApplicationComponent } from './new-application.component';
import { problemsOf } from './company-checks';
import { ApplicationService } from '../../../../core/services/application.service';
import { ApplyOptionsService } from '../../../../core/services/apply-options.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ErrorHandlingService } from '../../../../core/services/error-handling.service';
import { InstitutionService } from '../../../../core/services/institution.service';
import { StudentService } from '../../../../core/services/student.service';
import { CompanyApplyOptions, IntakeOption } from '../../../../core/models/apply-options.model';
import { BuildingResponse } from '../../../../core/services/building.service';
import { Application } from '../../../../core/models/application.model';

/** GH-495: one form across companies, checked per company as buildings are picked. */
describe('NewApplicationComponent', () => {
  const uct = { id: 1, name: 'UCT' };
  const building = (id: number, tenantId: number, companyName: string) =>
    ({ id, tenantId, companyName, name: `Residence ${id}` } as BuildingResponse);
  const residence1 = building(1, 10, 'Company A');
  const residence2 = building(2, 20, 'Company B');
  const residence3 = building(3, 10, 'Company A');
  const intake = (intakeId: number, intakeName: string): IntakeOption => ({
    intakeId,
    intakeName,
    academicYearId: 2,
    academicYearName: '2027',
    stayStartDate: '2027-02-01',
    stayEndDate: '2027-11-30',
    applicationOpenDate: '2026-07-01',
    applicationCloseDate: '2027-01-31',
  });
  const options = (
    companyId: number,
    companyName: string,
    extra: Partial<CompanyApplyOptions> = {}
  ) =>
    ({
      companyId,
      companyName,
      openIntakes: [intake(companyId * 10, 'Annual')],
      existingApplications: [],
      missingDocuments: [],
      ...extra,
    } as CompanyApplyOptions);

  let applicationService: jasmine.SpyObj<ApplicationService>;
  let applyOptions: jasmine.SpyObj<ApplyOptionsService>;
  let studentService: jasmine.SpyObj<StudentService>;
  let navigate: jasmine.Spy;
  let showSuccess: jasmine.Spy;
  let component: NewApplicationComponent;

  beforeEach(() => {
    applicationService = jasmine.createSpyObj('ApplicationService', ['createApplication']);
    applyOptions = jasmine.createSpyObj('ApplyOptionsService', ['getBuildings', 'getOptions']);
    studentService = jasmine.createSpyObj('StudentService', [
      'getStudentProfile',
      'updateStudentProfile',
    ]);
    const institutionService = jasmine.createSpyObj('InstitutionService', ['getInstitutions']);

    applyOptions.getBuildings.and.returnValue(of([residence1, residence2, residence3]));
    applyOptions.getOptions.and.callFake((buildingId: number) =>
      of(buildingId === 2 ? options(20, 'Company B') : options(10, 'Company A'))
    );
    institutionService.getInstitutions.and.returnValue(of([uct]));
    studentService.getStudentProfile.and.returnValue(of({ id: 7, institutionId: 1 }) as never);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AuthService, useValue: { currentUserValue: { id: '1' } } },
        { provide: ApplicationService, useValue: applicationService },
        { provide: ApplyOptionsService, useValue: applyOptions },
        { provide: InstitutionService, useValue: institutionService },
        { provide: StudentService, useValue: studentService },
      ],
    });
    navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const errorService = TestBed.inject(ErrorHandlingService);
    spyOn(errorService, 'showWarning');
    showSuccess = spyOn(errorService, 'showSuccess');
    component = TestBed.createComponent(NewApplicationComponent).componentInstance;
    component.ngOnInit();
    component.applicationForm.patchValue({ courseName: 'BSc' });
  });

  it('picker_listsBuildingsThatCanTakeTheInstitution_fromEveryCompany', () => {
    expect(applyOptions.getBuildings).toHaveBeenCalledWith(1);
    expect(component.buildings.map((b) => b.companyName)).toEqual([
      'Company A',
      'Company B',
      'Company A',
    ]);
    expect(component.institutionName).toBe('UCT');
  });

  it('picker_search_filtersTheListedBuildingsOnly', () => {
    component.onBuildingSearch('company b');

    expect(component.buildings).toEqual([residence2]);
    component.onBuildingSearch('');
    expect(component.buildings.length).toBe(3);
  });

  it('pick_buildingsAtTwoCompanies_checksEachCompanyOnce', () => {
    component.onBuildingSelectionChanged([1, 2, 3]);

    expect(component.checks.map((c) => [c.companyName, c.buildingIds])).toEqual([
      ['Company A', [1, 3]],
      ['Company B', [2]],
    ]);
    expect(applyOptions.getOptions).toHaveBeenCalledTimes(2);
    expect(component.checks.every((c) => c.chosenIntakeId !== null)).toBeTrue();
    expect(component.canSubmit).toBeTrue();
  });

  it('pick_companyMissingADocument_showsItBeforeSubmitAndBlocksSubmit', () => {
    applyOptions.getOptions.and.returnValue(
      of(options(20, 'Company B', { missingDocuments: ['Proof of address'] }))
    );

    component.onBuildingSelectionChanged([2]);

    expect(problemsOf(component.checks[0])).toEqual(['Company B needs proof of address.']);
    expect(component.canSubmit).toBeFalse();
  });

  it('pick_companyWithAnActiveApplicationForThatYear_linksItInsteadOfANewForm', () => {
    applyOptions.getOptions.and.returnValue(
      of(
        options(20, 'Company B', {
          openIntakes: [],
          existingApplications: [
            {
              applicationId: 44,
              applicationCode: 'A',
              academicYearName: '2027',
              intakeName: 'Annual',
              status: 'UNDER_REVIEW',
            },
          ],
        })
      )
    );

    component.onBuildingSelectionChanged([2]);

    expect(problemsOf(component.checks[0])).toEqual([
      'You already applied for 2027 here (Annual, under review).',
    ]);
    expect(component.canSubmit).toBeFalse();
  });

  it('pick_companyNotOpenYet_saysWhenItOpens', () => {
    applyOptions.getOptions.and.returnValue(
      of(
        options(20, 'Company B', {
          openIntakes: [],
          nextIntake: { ...intake(5, 'Annual'), applicationOpenDate: '2027-05-01' },
        })
      )
    );

    component.onBuildingSelectionChanged([2]);

    expect(problemsOf(component.checks[0])).toEqual(['Company B opens applications on 1 May.']);
  });

  it('removeCompany_dropsAllItsBuildings', () => {
    component.onBuildingSelectionChanged([1, 2, 3]);

    component.removeCompany(10);

    expect(component.selectedIds).toEqual([2]);
    expect(component.checks.map((c) => c.companyId)).toEqual([20]);
  });

  it('intake_severalOpen_isAskedForAndSentPerCompany', () => {
    applyOptions.getOptions.and.callFake((buildingId: number) =>
      of(
        buildingId === 2
          ? options(20, 'Company B')
          : options(10, 'Company A', {
              openIntakes: [intake(101, 'Annual'), intake(102, 'Semester 1')],
            })
      )
    );
    applicationService.createApplication.and.returnValue(of([]));
    component.onBuildingSelectionChanged([1, 2]);
    expect(component.canSubmit).toBeFalse();

    component.onIntakeChosen(10, 102);
    component.onSubmit();

    const sent = applicationService.createApplication.calls.mostRecent().args[0];
    expect(sent.preferredBuildingIds).toEqual([1, 2]);
    expect(sent.intakeIds).toEqual({ 10: 102, 20: 200 });
  });

  it('submit_success_saysWhatWasSentToWhichCompany', () => {
    applicationService.createApplication.and.returnValue(
      of([
        {
          id: 1,
          companyName: 'Company A',
          preferredBuildings: [{ buildingId: 1, buildingName: 'Residence 1', preferenceOrder: 1 }],
        },
        {
          id: 2,
          companyName: 'Company B',
          preferredBuildings: [{ buildingId: 2, buildingName: 'Residence 2', preferenceOrder: 1 }],
        },
      ] as Application[])
    );
    component.onBuildingSelectionChanged([1, 2]);

    component.onSubmit();

    expect(showSuccess).toHaveBeenCalledWith(
      '2 applications sent: Company A (Residence 1) and Company B (Residence 2).'
    );
    expect(navigate).toHaveBeenCalledWith(['/applications']);
  });

  it('submit_refused_keepsWhatWasTypedAndChecksEachCompanyAgain', () => {
    applicationService.createApplication.and.returnValue(
      throwError(
        () => new HttpErrorResponse({ status: 400, error: { errors: ['Company B closed'] } })
      )
    );
    component.applicationForm.patchValue({ specialRequirements: 'Ground floor' });
    component.onBuildingSelectionChanged([1, 2]);
    applyOptions.getOptions.calls.reset();

    component.onSubmit();

    expect(component.submitError).not.toBeNull();
    expect(component.applicationForm.value.courseName).toBe('BSc');
    expect(component.applicationForm.value.specialRequirements).toBe('Ground floor');
    expect(component.selectedIds).toEqual([1, 2]);
    expect(applyOptions.getOptions).toHaveBeenCalledTimes(2);
    expect(navigate).not.toHaveBeenCalledWith(['/applications']);
  });

  it('institution_missing_isAskedInlineThenBuildingsLoad', () => {
    applyOptions.getBuildings.calls.reset();
    studentService.getStudentProfile.and.returnValue(of({ id: 7 }) as never);
    component.loadStudentProfile();

    expect(component.needsInstitution).toBeTrue();
    expect(applyOptions.getBuildings).not.toHaveBeenCalled();

    component.onInstitutionSaved(uct);

    expect(component.needsInstitution).toBeFalse();
    expect(component.institutionName).toBe('UCT');
    expect(applyOptions.getBuildings).toHaveBeenCalledWith(1);
  });

  it('onSubmit_nothingPicked_warnsAndDoesNotSubmit', () => {
    component.onSubmit();

    expect(applicationService.createApplication).not.toHaveBeenCalled();
  });

  it('ngOnInit_signedOut_goesToLogin', () => {
    const fresh = TestBed.createComponent(NewApplicationComponent).componentInstance;
    (TestBed.inject(AuthService) as unknown as { currentUserValue: null }).currentUserValue = null;

    fresh.ngOnInit();

    expect(navigate).toHaveBeenCalledWith(['/login']);
  });
});
