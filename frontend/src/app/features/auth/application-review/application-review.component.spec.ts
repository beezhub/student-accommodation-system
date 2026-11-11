import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { ApplicationReviewComponent } from './application-review.component';
import { ApplicationService } from '../../../core/services/application.service';
import { AuthService } from '../../../core/services/auth.service';
import { ErrorHandlingService } from '../../../core/services/error-handling.service';
import { InstitutionService } from '../../../core/services/institution.service';
import { StudentService } from '../../../core/services/student.service';
import { YearOfStudyService } from '../../../core/services/year-of-study.service';
import { Application } from '../../../core/models/application.model';
import { StudentResponse } from '../../../core/models/student.model';
import { User } from '../../../core/models/user.model';

/** #538: the page shows what step 4 saved; it no longer submits anything. */
describe('ApplicationReviewComponent', () => {
  const user = { firstName: 'Ann', lastName: 'Lee', email: 'a@b.com' } as User;
  const failure = () => throwError(() => new HttpErrorResponse({ status: 500 }));
  const saved = { id: 42, courseName: 'BSc' } as Application;
  const older = { id: 7, courseName: 'BA' } as Application;

  let authService: { currentUserValue: User | null };
  let queryParams: Record<string, string>;
  let studentService: jasmine.SpyObj<StudentService>;
  let applicationService: jasmine.SpyObj<ApplicationService>;
  let institutionService: jasmine.SpyObj<InstitutionService>;
  let yearOfStudyService: jasmine.SpyObj<YearOfStudyService>;
  let navigate: jasmine.Spy;
  let showWarning: jasmine.Spy;
  let component: ApplicationReviewComponent;

  beforeEach(() => {
    authService = { currentUserValue: user };
    queryParams = { ids: '42' };
    studentService = jasmine.createSpyObj('StudentService', ['getStudentProfile']);
    applicationService = jasmine.createSpyObj('ApplicationService', ['getMyApplications']);
    institutionService = jasmine.createSpyObj('InstitutionService', ['getInstitutions']);
    yearOfStudyService = jasmine.createSpyObj('YearOfStudyService', ['getYearsOfStudy']);

    studentService.getStudentProfile.and.returnValue(
      of({ institutionId: 1, yearOfStudyId: 2 }) as never
    );
    applicationService.getMyApplications.and.returnValue(of([saved, older]));
    institutionService.getInstitutions.and.returnValue(of([{ id: 1, name: 'UCT' }]) as never);
    yearOfStudyService.getYearsOfStudy.and.returnValue(
      of([{ id: 2, yearOfStudy: 'Second' }]) as never
    );

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              get queryParamMap() {
                return convertToParamMap(queryParams);
              },
            },
          },
        },
        { provide: AuthService, useValue: authService },
        { provide: StudentService, useValue: studentService },
        { provide: ApplicationService, useValue: applicationService },
        { provide: InstitutionService, useValue: institutionService },
        { provide: YearOfStudyService, useValue: yearOfStudyService },
      ],
    });
    navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    showWarning = spyOn(TestBed.inject(ErrorHandlingService), 'showWarning');
    component = TestBed.createComponent(ApplicationReviewComponent).componentInstance;
  });

  describe('loading', () => {
    it('ngOnInit_signedOut_goesToLogin', () => {
      authService.currentUserValue = null;

      component.ngOnInit();

      expect(navigate).toHaveBeenCalledWith(['/login']);
    });

    it('ngOnInit_noIds_goesToTheDashboard', () => {
      queryParams = {};

      component.ngOnInit();

      expect(navigate).toHaveBeenCalledWith(['/dashboard']);
      expect(applicationService.getMyApplications).not.toHaveBeenCalled();
    });

    it('ngOnInit_ids_showsOnlyTheApplicationsJustSaved', () => {
      component.ngOnInit();

      expect(component.applications).toEqual([saved]);
      expect(component.isLoading).toBeFalse();
      expect(component.getInstitutionName()).toBe('UCT');
      expect(component.getYearOfStudyName()).toBe('Second');
    });

    it('ngOnInit_applicationsFail_warnsAndStopsLoading', () => {
      applicationService.getMyApplications.and.returnValue(failure() as never);

      component.ngOnInit();

      expect(showWarning).toHaveBeenCalled();
      expect(component.isLoading).toBeFalse();
    });

    it('ngOnInit_referenceDataFails_warnsAndStays', () => {
      studentService.getStudentProfile.and.returnValue(failure() as never);
      institutionService.getInstitutions.and.returnValue(failure() as never);
      yearOfStudyService.getYearsOfStudy.and.returnValue(failure() as never);

      component.ngOnInit();

      expect(showWarning).toHaveBeenCalledTimes(3);
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

  describe('buildingNames', () => {
    it('buildingNames_preferences_listedInPreferenceOrder', () => {
      const application = {
        preferredBuildings: [
          { buildingId: 2, buildingName: 'Beta', preferenceOrder: 2 },
          { buildingId: 1, buildingName: 'Alpha', preferenceOrder: 1 },
        ],
      } as Application;

      expect(component.buildingNames(application)).toBe('Alpha, Beta');
    });

    it('buildingNames_none_saysNoPreference', () => {
      expect(component.buildingNames({} as Application)).toBe('No preference');
    });
  });

  describe('finish', () => {
    it('finish_applicationsShown_goesToTheConfirmation', () => {
      component.applications = [saved];

      component.finish();

      expect(navigate).toHaveBeenCalledWith(['/application-confirmation', 42]);
    });

    it('finish_nothingShown_goesToTheDashboard', () => {
      component.finish();

      expect(navigate).toHaveBeenCalledWith(['/dashboard']);
    });
  });
});
