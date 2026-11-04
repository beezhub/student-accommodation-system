import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ApplicationDetailsComponent } from './application-details.component';
import {
  ApplicationFullDetails,
  ApplicationService,
} from '../../../../core/services/application.service';
import { Application } from '../../../../core/models/application.model';
import { AdminService } from '../../../../core/services/admin.service';
import { AuthService } from '../../../../core/services/auth.service';
import { RoleService } from '../../../../core/services/role.service';

const REASON = 'The proof of registration is for the wrong academic year.';

function details(status: string, remarks?: string): ApplicationFullDetails {
  return {
    id: 7,
    applicationCode: 'APP-7',
    status,
    remarks,
    applicationDate: '2026-01-01',
    createdAt: '2026-01-01',
    updatedAt: '2026-01-01',
    studentProfile: { id: 1, firstName: 'Thandi', lastName: 'Mokoena', email: 't@test.com' },
  } as unknown as ApplicationFullDetails;
}

describe('ApplicationDetailsComponent resubmit', () => {
  let fixture: ComponentFixture<ApplicationDetailsComponent>;
  let applicationService: jasmine.SpyObj<ApplicationService>;

  function create(application: ApplicationFullDetails, isStudent = true): void {
    applicationService = jasmine.createSpyObj<ApplicationService>('ApplicationService', [
      'getApplicationFullDetails',
      'resubmitApplication',
    ]);
    applicationService.getApplicationFullDetails.and.returnValue(of(application));

    TestBed.configureTestingModule({
      imports: [ApplicationDetailsComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { params: of({ id: '7' }) } },
        { provide: ApplicationService, useValue: applicationService },
        { provide: AdminService, useValue: {} },
        { provide: AuthService, useValue: { currentUserValue: null } },
        {
          provide: RoleService,
          useValue: { isPlatformAdmin: () => false, isStudent: () => isStudent },
        },
      ],
    });
    fixture = TestBed.createComponent(ApplicationDetailsComponent);
    fixture.detectChanges();
  }

  function resubmitButton(): HTMLButtonElement | undefined {
    fixture.detectChanges();
    const buttons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('button')
    );
    return buttons.find((b) => (b.textContent ?? '').includes('Resubmit'));
  }

  it('resubmit_changesRequested_showsTheReasonAndTheButton', () => {
    create(details('CHANGES_REQUESTED', REASON));

    expect(resubmitButton()).toBeDefined();
    expect(fixture.nativeElement.querySelector('.changes-requested-card').textContent).toContain(
      REASON
    );
  });

  ['PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED'].forEach((status) => {
    it(`resubmit_${status}_isNotOffered`, () => {
      create(details(status));

      expect(resubmitButton()).toBeUndefined();
    });
  });

  it('resubmit_notAStudent_isNotOffered', () => {
    create(details('CHANGES_REQUESTED', REASON), false);

    expect(resubmitButton()).toBeUndefined();
  });

  it('resubmit_clicked_callsTheBackendAndReloads', () => {
    create(details('CHANGES_REQUESTED', REASON));
    applicationService.resubmitApplication.and.returnValue(of({ id: 7 } as Application));
    applicationService.getApplicationFullDetails.and.returnValue(of(details('PENDING', REASON)));

    resubmitButton()!.click();

    expect(applicationService.resubmitApplication).toHaveBeenCalledOnceWith(7);
    expect(resubmitButton()).toBeUndefined();
    expect(fixture.nativeElement.textContent).toContain('Pending');
  });

  it('resubmit_backendRefuses_keepsTheButtonForARetry', () => {
    create(details('CHANGES_REQUESTED', REASON));
    applicationService.resubmitApplication.and.returnValue(
      throwError(() => ({ error: { message: 'Refused' } }))
    );

    resubmitButton()!.click();

    expect(resubmitButton()!.disabled).toBeFalse();
    expect(applicationService.getApplicationFullDetails).toHaveBeenCalledTimes(1);
  });
});
