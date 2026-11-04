import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ApplicationDetailsComponent } from './application-details.component';
import {
  ApplicationFullDetails,
  ApplicationService,
} from '../../../../core/services/application.service';
import { Application } from '../../../../core/models/application.model';
import { AdminService, ApplicationResponse } from '../../../../core/services/admin.service';
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

  function create(application: ApplicationFullDetails, isStudent = true, overridden = false): void {
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
        { provide: AuthService, useValue: { currentUserValue: null } },
      ],
    });
    if (!overridden) {
      TestBed.overrideProvider(AdminService, { useValue: {} });
      TestBed.overrideProvider(RoleService, {
        useValue: { isPlatformAdmin: () => false, isStudent: () => isStudent },
      });
    }
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

  it('timeline_explainsEachStatus', () => {
    const expected: Record<string, string> = {
      APPROVED: 'Application Approved',
      REJECTED: 'Application Rejected',
      PENDING: 'Under Review',
      UNDER_REVIEW: 'Under Review',
      CHANGES_REQUESTED: 'Changes Requested',
      ALLOCATED: 'Room Allocated',
    };
    create(details('PENDING'));
    const component = fixture.componentInstance;

    Object.entries(expected).forEach(([status, label]) => {
      applicationService.getApplicationFullDetails.and.returnValue(of(details(status, REASON)));
      component.loadApplicationDetails();
      expect(component.timeline.map((e) => e.status))
        .withContext(status)
        .toContain(label);
    });
  });

  it('displayHelpers_coverEachStatusShape', () => {
    create(details('PENDING'));
    const component = fixture.componentInstance;

    expect(component.getStatusClass('approved')).toBe('status-success');
    expect(component.getStatusClass('Under Review')).toBe('status-warning');
    expect(component.getStatusClass('rejected')).toBe('status-danger');
    expect(component.getStatusClass('other')).toBe('status-default');
    expect(component.getTimelineIcon('Application Submitted')).toBe('send');
    expect(component.getTimelineIcon('Document Verified')).toBe('fact_check');
    expect(component.getTimelineIcon('Under Review')).toBe('hourglass_empty');
    expect(component.getTimelineIcon('Application Approved')).toBe('check_circle');
    expect(component.getTimelineIcon('Application Rejected')).toBe('cancel');
    expect(component.getTimelineIcon('Room Allocated')).toBe('home');
    expect(component.getTimelineIcon('Something else')).toBe('info');
  });

  it('goBack_returnsTheStudentToTheirList', () => {
    create(details('PENDING'));
    const navigate = spyOn(TestBed.inject(Router), 'navigate');

    fixture.componentInstance.goBack();

    expect(navigate).toHaveBeenCalledOnceWith(['/applications']);
  });

  it('loadApplicationDetails_backendFails_stopsLoading', () => {
    create(details('PENDING'));
    applicationService.getApplicationFullDetails.and.returnValue(
      throwError(() => new Error('boom'))
    );

    fixture.componentInstance.loadApplicationDetails();

    expect(fixture.componentInstance.isLoading).toBeFalse();
  });

  describe('platform admin actions', () => {
    let adminService: jasmine.SpyObj<AdminService>;

    beforeEach(() => {
      adminService = jasmine.createSpyObj<AdminService>('AdminService', [
        'updateApplicationStatus',
      ]);
      TestBed.overrideProvider(AdminService, { useValue: adminService });
      TestBed.overrideProvider(RoleService, {
        useValue: { isPlatformAdmin: () => true, isStudent: () => false },
      });
      create(details('PENDING'), false, true);
    });

    it('canApproveOrReject_pending_isTrueForAnAdmin', () => {
      expect(fixture.componentInstance.canApproveOrReject()).toBeTrue();
    });

    it('openApproveDialog_makesRemarksOptional', () => {
      const component = fixture.componentInstance;

      component.openApproveDialog();

      expect(component.showApproveDialog).toBeTrue();
      expect(component.remarksForm.valid).toBeTrue();
    });

    it('openRejectDialog_makesRemarksRequired', () => {
      const component = fixture.componentInstance;

      component.openRejectDialog();

      expect(component.showRejectDialog).toBeTrue();
      expect(component.remarksForm.valid).toBeFalse();

      component.closeDialogs();
      expect(component.showRejectDialog).toBeFalse();
    });

    it('approveApplication_success_reloads', () => {
      const component = fixture.componentInstance;
      adminService.updateApplicationStatus.and.returnValue(of({} as ApplicationResponse));
      applicationService.getApplicationFullDetails.calls.reset();

      component.approveApplication();

      expect(adminService.updateApplicationStatus).toHaveBeenCalledOnceWith(7, 'APPROVED', '');
      expect(applicationService.getApplicationFullDetails).toHaveBeenCalled();
    });

    it('approveApplication_backendFails_doesNotReload', () => {
      const component = fixture.componentInstance;
      adminService.updateApplicationStatus.and.returnValue(throwError(() => ({ error: {} })));
      applicationService.getApplicationFullDetails.calls.reset();

      component.approveApplication();

      expect(applicationService.getApplicationFullDetails).not.toHaveBeenCalled();
    });

    it('rejectApplication_withRemarks_sendsThem', () => {
      const component = fixture.componentInstance;
      adminService.updateApplicationStatus.and.returnValue(of({} as ApplicationResponse));
      component.openRejectDialog();
      component.remarksForm.patchValue({ remarks: 'Not eligible' });

      component.rejectApplication();

      expect(adminService.updateApplicationStatus).toHaveBeenCalledOnceWith(
        7,
        'REJECTED',
        'Not eligible'
      );
    });

    it('rejectApplication_noRemarks_callsNothing', () => {
      const component = fixture.componentInstance;
      component.openRejectDialog();

      component.rejectApplication();

      expect(adminService.updateApplicationStatus).not.toHaveBeenCalled();
    });

    it('rejectApplication_backendFails_doesNotReload', () => {
      const component = fixture.componentInstance;
      adminService.updateApplicationStatus.and.returnValue(throwError(() => ({ error: {} })));
      component.openRejectDialog();
      component.remarksForm.patchValue({ remarks: 'Not eligible' });
      applicationService.getApplicationFullDetails.calls.reset();

      component.rejectApplication();

      expect(applicationService.getApplicationFullDetails).not.toHaveBeenCalled();
    });

    it('goBack_returnsTheAdminToTheAdminList', () => {
      const navigate = spyOn(TestBed.inject(Router), 'navigate');

      fixture.componentInstance.goBack();

      expect(navigate).toHaveBeenCalledOnceWith(['/admin/applications']);
    });
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
