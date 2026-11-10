import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { NewApplicationComponent } from './new-application.component';
import { ApplicationService } from '../../../../core/services/application.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ApplyOptionsService } from '../../../../core/services/apply-options.service';
import { ErrorHandlingService } from '../../../../core/services/error-handling.service';
import { InstitutionService } from '../../../../core/services/institution.service';
import { StudentService } from '../../../../core/services/student.service';

/**
 * #441: errorInterceptor has already shown the backend's reason by the time the submit callback
 * runs, so a second snackbar from this screen could only replace it.
 */
describe('NewApplicationComponent submit errors', () => {
  it('onSubmit_missingDocuments_showsTheReasonInlineWithoutASecondSnackbar', () => {
    const applicationService = jasmine.createSpyObj<ApplicationService>('ApplicationService', [
      'createApplication',
    ]);
    applicationService.createApplication.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: { status: 409, errors: ['Missing required documents: ID Copy'] },
          })
      )
    );
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: ApplicationService, useValue: applicationService },
        { provide: AuthService, useValue: { currentUserValue: { id: '1' } } },
        {
          provide: ApplyOptionsService,
          useValue: { getBuildings: () => of([]), getOptions: () => of() },
        },
        { provide: InstitutionService, useValue: { getInstitutions: () => of([]) } },
        {
          provide: StudentService,
          useValue: { getStudentProfile: () => of({ id: 7, institutionId: 1 }) },
        },
      ],
    });
    const showErrorSnackbar = spyOn(TestBed.inject(ErrorHandlingService), 'showErrorSnackbar');

    const component = TestBed.createComponent(NewApplicationComponent).componentInstance;
    component.ngOnInit();
    component.applicationForm.patchValue({ courseName: 'BSc Computer Science' });
    // GH-495: a company that is checked and ready, so the form is allowed to submit.
    const annual = {
      intakeId: 9,
      intakeName: 'Annual',
      academicYearId: 2,
      academicYearName: '2027',
      stayStartDate: '2027-02-01',
      stayEndDate: '2027-11-30',
      applicationOpenDate: '2026-07-01',
      applicationCloseDate: '2027-01-31',
    };
    component.checks = [
      {
        companyId: 1,
        companyName: 'Company A',
        buildingIds: [5],
        loading: false,
        loadError: null,
        chosenIntakeId: 9,
        options: {
          companyId: 1,
          companyName: 'Company A',
          openIntakes: [annual],
          existingApplications: [],
          missingDocuments: [],
        },
      },
    ];
    component.applicationForm.patchValue({ preferredBuildingIds: [5] });
    component.onSubmit();

    expect(component.submitError?.message).toBe('Missing required documents: ID Copy');
    expect(showErrorSnackbar).not.toHaveBeenCalled();
  });
});
