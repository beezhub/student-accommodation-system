import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { NEVER, Observable, of, throwError } from 'rxjs';

import { NewApplicationComponent } from './new-application.component';
import { AuthService } from '../../../../core/services/auth.service';
import { BuildingService } from '../../../../core/services/building.service';
import { InstitutionService } from '../../../../core/services/institution.service';
import { StudentService } from '../../../../core/services/student.service';
import { StudentResponse } from '../../../../core/models/student.model';

/**
 * #465: /profile can only update a student row, so a student with no row who was sent there could
 * never save. /profile-setup is the only screen that creates one.
 *
 * <p>student.user_id has no unique constraint, so only a confirmed 404 may lead to the create
 * screen: a student who does have a profile would end up with a second row.
 */
describe('NewApplicationComponent with no student profile', () => {
  let navigate: jasmine.Spy;
  let getStudentProfile: jasmine.Spy<() => Observable<StudentResponse>>;
  let component: NewApplicationComponent;

  const failingWith = (status: number) =>
    throwError(() => new HttpErrorResponse({ status, error: { errors: ['Nope'] } }));

  beforeEach(() => {
    getStudentProfile = jasmine.createSpy('getStudentProfile');
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AuthService, useValue: { currentUserValue: { id: '1' } } },
        { provide: BuildingService, useValue: { getAllBuildingsNoPagination: () => of([]) } },
        { provide: InstitutionService, useValue: { getInstitutions: () => of([]) } },
        { provide: StudentService, useValue: { getStudentProfile } },
      ],
    });
    navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    component = TestBed.createComponent(NewApplicationComponent).componentInstance;
  });

  it('loadStudentProfile_noProfile_goesToProfileSetup', () => {
    getStudentProfile.and.returnValue(failingWith(404));

    component.ngOnInit();

    expect(navigate).toHaveBeenCalledOnceWith(['/profile-setup'], { replaceUrl: true });
  });

  it('loadStudentProfile_serverError_staysAndShowsTheError', () => {
    getStudentProfile.and.returnValue(failingWith(500));

    component.ngOnInit();

    expect(navigate).not.toHaveBeenCalled();
    expect(component.loadError?.statusCode).toBe(500);
  });

  it('onSubmit_profileStillLoading_doesNotGoToProfileSetup', () => {
    getStudentProfile.and.returnValue(NEVER);
    component.ngOnInit();

    component.onSubmit();

    expect(navigate).not.toHaveBeenCalled();
  });

  /** The load failed, so we do not know whether a row exists. Asking again is what settles it. */
  it('onSubmit_earlierLoadFailed_asksAgainAndGoesToProfileSetupOnlyOnA404', () => {
    getStudentProfile.and.returnValue(failingWith(500));
    component.ngOnInit();
    expect(navigate).not.toHaveBeenCalled();

    getStudentProfile.and.returnValue(failingWith(404));
    component.onSubmit();

    expect(navigate).toHaveBeenCalledOnceWith(['/profile-setup'], { replaceUrl: true });
  });
});
