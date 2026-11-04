import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ApplicationDetailDialogComponent } from './application-detail-dialog.component';
import { Application } from '../../../../core/models/application.model';
import {
  ApplicationFullDetails,
  ApplicationService,
  DocumentChecklistItem,
  DocumentWithComments,
} from '../../../../core/services/application.service';
import { AdminService, ApplicationResponse } from '../../../../core/services/admin.service';

function checklistItem(documentId: number, status: string): DocumentChecklistItem {
  return {
    documentTypeId: documentId,
    type: `Type ${documentId}`,
    status: status as DocumentChecklistItem['status'],
    documentId,
    isRequired: true,
  };
}

function document(id: number, status: string): DocumentWithComments {
  return {
    id,
    fileName: `doc-${id}.pdf`,
    type: `Type ${id}`,
    status,
    uploadedAt: '2026-01-01',
    documentTypeId: id,
    documentTypeName: `Type ${id}`,
    comments: [],
  };
}

function details(status: string, docStatus = 'PENDING'): ApplicationFullDetails {
  return {
    id: 7,
    applicationCode: 'APP-7',
    status,
    applicationDate: '2026-01-01',
    createdAt: '2026-01-01',
    updatedAt: '2026-01-01',
    studentProfile: { firstName: 'Thandi', lastName: 'Mokoena' },
    documents: [document(1, docStatus), document(2, docStatus)],
    requiredDocumentsChecklist: {
      documents: [checklistItem(1, docStatus), checklistItem(2, docStatus)],
    },
  } as unknown as ApplicationFullDetails;
}

describe('ApplicationDetailDialogComponent', () => {
  let fixture: ComponentFixture<ApplicationDetailDialogComponent>;
  let component: ApplicationDetailDialogComponent;
  let applicationService: jasmine.SpyObj<ApplicationService>;
  let adminService: jasmine.SpyObj<AdminService>;

  function create(application: ApplicationFullDetails): void {
    applicationService = jasmine.createSpyObj<ApplicationService>('ApplicationService', [
      'getApplicationFullDetails',
      'saveReviewProgress',
      'moveToAllocationPhase',
      'markUnderReview',
      'requestChanges',
    ]);
    applicationService.getApplicationFullDetails.and.returnValue(of(application));
    adminService = jasmine.createSpyObj<AdminService>('AdminService', [
      'updateApplicationStatus',
      'downloadDocument',
    ]);

    TestBed.configureTestingModule({
      imports: [ApplicationDetailDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: ApplicationService, useValue: applicationService },
        { provide: AdminService, useValue: adminService },
        { provide: MatDialogRef, useValue: { close: jasmine.createSpy('close') } },
        { provide: MAT_DIALOG_DATA, useValue: { applicationId: 7 } },
      ],
    });
    fixture = TestBed.createComponent(ApplicationDetailDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function approveAndSave(id: number): void {
    const doc = component.documents.find((d) => d.id === id)!;
    component.onDocumentStatusChange(doc, 'APPROVED');
    component.saveProgress();
  }

  describe('saveProgress', () => {
    it('saveProgress_secondSave_keepsStatusesSavedEarlier', () => {
      create(details('UNDER_REVIEW'));
      applicationService.saveReviewProgress.and.returnValue(of({ success: true }));

      approveAndSave(1);
      expect(component.requiredApprovedCount).toBe(1);

      approveAndSave(2);

      expect(component.requiredApprovedCount).toBe(2);
      expect(component.allRequiredApproved).toBeTrue();
    });

    it('onDocumentStatusChange_revertAfterSave_returnsToSavedStatusNotOriginal', () => {
      create(details('UNDER_REVIEW'));
      applicationService.saveReviewProgress.and.returnValue(of({ success: true }));
      approveAndSave(1);

      const doc = component.documents.find((d) => d.id === 1)!;
      component.onDocumentStatusChange(doc, 'REJECTED');
      expect(component.requiredApprovedCount).toBe(0);
      component.onDocumentStatusChange(component.documents.find((d) => d.id === 1)!, 'APPROVED');

      expect(component.requiredApprovedCount).toBe(1);
    });

    it('saveProgress_serverRefuses_keepsChangesUnsaved', () => {
      create(details('UNDER_REVIEW'));
      applicationService.saveReviewProgress.and.returnValue(of({ success: false }));

      approveAndSave(1);

      expect(component.hasUnsavedChanges()).toBeTrue();
      expect(component.applicationDetails!.requiredDocumentsChecklist.documents[0].status).toBe(
        'PENDING'
      );
    });
  });

  describe('review actions', () => {
    const MARK = 'Mark Under Review';
    const REQUEST = 'Request Changes';
    const MOVE = 'Move to Allocation';

    function footerButton(label: string): HTMLButtonElement | undefined {
      fixture.detectChanges();
      const buttons: HTMLButtonElement[] = Array.from(
        fixture.nativeElement.querySelectorAll('mat-dialog-actions button')
      );
      return buttons.find((b) => (b.textContent ?? '').includes(label));
    }

    const expectedActions: Record<string, string[]> = {
      PENDING: [MARK],
      UNDER_REVIEW: [REQUEST],
      CHANGES_REQUESTED: [],
      APPROVED: [MOVE],
      AWAITING_ALLOCATION: [],
      ALLOCATED: [],
      CHECKED_IN: [],
      REJECTED: [],
      EXPIRED: [],
      CANCELLED: [],
    };

    Object.entries(expectedActions).forEach(([status, expected]) => {
      it(`footer_${status}_offersOnlyLegalReviewActions`, () => {
        create(details(status, 'APPROVED'));

        const offered = [MARK, REQUEST, MOVE].filter((label) => footerButton(label));

        expect(offered).toEqual(expected);
      });
    });

    it('markUnderReview_pending_movesStatusAndKeepsDialogOpen', () => {
      create(details('PENDING'));
      applicationService.markUnderReview.and.returnValue(
        of({ id: 7, status: 'UNDER_REVIEW' } as Application)
      );

      footerButton(MARK)!.click();

      expect(applicationService.markUnderReview).toHaveBeenCalledOnceWith(7);
      expect(TestBed.inject(MatDialogRef).close).not.toHaveBeenCalled();
      expect(footerButton(REQUEST)).toBeDefined();
      expect(footerButton(MARK)).toBeUndefined();
    });

    it('closeDialog_afterStatusMoved_tellsTheListToReload', () => {
      create(details('PENDING'));
      applicationService.markUnderReview.and.returnValue(
        of({ id: 7, status: 'UNDER_REVIEW' } as Application)
      );
      component.markUnderReview();

      component.closeDialog();

      expect(TestBed.inject(MatDialogRef).close).toHaveBeenCalledOnceWith({
        action: 'saved',
        applicationId: 7,
        newStatus: 'UNDER_REVIEW',
      });
    });

    it('closeDialog_nothingMoved_reportsClosed', () => {
      create(details('PENDING'));

      component.closeDialog();

      expect(TestBed.inject(MatDialogRef).close).toHaveBeenCalledOnceWith({
        action: 'closed',
        applicationId: 7,
      });
    });

    function answerReasonDialog(reason: string | null): void {
      // The component's own instance: its imports provide MatDialog separately from the root one.
      spyOn(fixture.debugElement.injector.get(MatDialog), 'open').and.returnValue({
        afterClosed: () => of(reason),
      } as unknown as MatDialogRef<unknown>);
    }

    it('requestChanges_reasonGiven_sendsItAndCloses', () => {
      create(details('UNDER_REVIEW'));
      answerReasonDialog('Proof of registration is for the wrong year');
      applicationService.requestChanges.and.returnValue(
        of({ id: 7, status: 'CHANGES_REQUESTED' } as Application)
      );

      footerButton(REQUEST)!.click();

      expect(applicationService.requestChanges).toHaveBeenCalledOnceWith(
        7,
        'Proof of registration is for the wrong year'
      );
      expect(TestBed.inject(MatDialogRef).close).toHaveBeenCalledOnceWith({
        action: 'saved',
        applicationId: 7,
        newStatus: 'CHANGES_REQUESTED',
      });
    });

    it('requestChanges_reasonDialogDismissed_sendsNothing', () => {
      create(details('UNDER_REVIEW'));
      answerReasonDialog(null);

      component.requestChanges();

      expect(applicationService.requestChanges).not.toHaveBeenCalled();
      expect(TestBed.inject(MatDialogRef).close).not.toHaveBeenCalled();
    });

    it('requestChanges_unsavedDocumentChanges_isDisabled', () => {
      create(details('UNDER_REVIEW'));

      component.onDocumentStatusChange(component.documents[0], 'REJECTED');

      expect(footerButton(REQUEST)!.disabled).toBeTrue();
    });

    it('moveToAllocation_approved_callsTheBackendAndCloses', () => {
      create(details('APPROVED', 'APPROVED'));
      applicationService.moveToAllocationPhase.and.returnValue(of({ id: 7 } as Application));

      footerButton(MOVE)!.click();

      expect(applicationService.moveToAllocationPhase).toHaveBeenCalledOnceWith(7);
      expect(TestBed.inject(MatDialogRef).close).toHaveBeenCalledOnceWith({
        action: 'allocated',
        applicationId: 7,
        newStatus: 'AWAITING_ALLOCATION',
      });
    });

    it('moveToAllocation_documentsNotApproved_isDisabled', () => {
      create(details('APPROVED', 'PENDING'));

      expect(footerButton(MOVE)!.disabled).toBeTrue();
    });

    const rejectable: Record<string, boolean> = {
      PENDING: true,
      UNDER_REVIEW: true,
      APPROVED: true,
      AWAITING_ALLOCATION: true,
      CHANGES_REQUESTED: false,
      ALLOCATED: false,
      CHECKED_IN: false,
      REJECTED: false,
      EXPIRED: false,
      CANCELLED: false,
    };

    Object.entries(rejectable).forEach(([status, expected]) => {
      it(`reject_${status}_isOfferedOnlyWhereTheBackendAllowsIt`, () => {
        create(details(status));

        expect(!!footerButton('Reject')).toBe(expected);
      });
    });

    const closed: Record<string, boolean> = {
      PENDING: false,
      UNDER_REVIEW: false,
      CHANGES_REQUESTED: false,
      APPROVED: false,
      AWAITING_ALLOCATION: false,
      ALLOCATED: false,
      CHECKED_IN: false,
      REJECTED: true,
      EXPIRED: true,
      CANCELLED: true,
    };

    Object.entries(closed).forEach(([status, isClosed]) => {
      it(`documentReview_${status}_isHiddenOnlyOnceTheBackendRefusesIt`, () => {
        create(details(status));

        expect(!!footerButton('Save Progress')).toBe(!isClosed);
        expect(component.canReviewDocuments).toBe(!isClosed);
      });
    });

    it('approve_secondCallFails_keepsDialogOpenWithAWayForward', () => {
      create(details('UNDER_REVIEW', 'APPROVED'));
      adminService.updateApplicationStatus.and.returnValue(
        of({ id: 7, status: 'APPROVED' } as ApplicationResponse)
      );
      applicationService.moveToAllocationPhase.and.returnValue(
        throwError(() => ({ error: { message: 'Temporarily unavailable' } }))
      );

      footerButton('Approve Application')!.click();

      expect(TestBed.inject(MatDialogRef).close).not.toHaveBeenCalled();
      expect(component.applicationDetails!.status).toBe('APPROVED');
      expect(footerButton('Approve Application')).toBeUndefined();
      expect(footerButton(MOVE)!.disabled).toBeFalse();
      expect(footerButton('Reject')).toBeDefined();
    });

    it('approve_secondCallFailsThenRetrySucceeds_movesToAllocation', () => {
      create(details('UNDER_REVIEW', 'APPROVED'));
      adminService.updateApplicationStatus.and.returnValue(
        of({ id: 7, status: 'APPROVED' } as ApplicationResponse)
      );
      applicationService.moveToAllocationPhase.and.returnValues(
        throwError(() => ({ error: {} })),
        of({ id: 7 } as Application)
      );

      component.approveApplication();
      footerButton(MOVE)!.click();

      expect(applicationService.moveToAllocationPhase).toHaveBeenCalledTimes(2);
      expect(TestBed.inject(MatDialogRef).close).toHaveBeenCalledOnceWith({
        action: 'allocated',
        applicationId: 7,
        newStatus: 'AWAITING_ALLOCATION',
      });
    });

    it('closeDialog_afterApproveStranded_tellsTheListToReload', () => {
      create(details('UNDER_REVIEW', 'APPROVED'));
      adminService.updateApplicationStatus.and.returnValue(
        of({ id: 7, status: 'APPROVED' } as ApplicationResponse)
      );
      applicationService.moveToAllocationPhase.and.returnValue(throwError(() => ({ error: {} })));
      component.approveApplication();

      component.closeDialog();

      expect(TestBed.inject(MatDialogRef).close).toHaveBeenCalledOnceWith({
        action: 'saved',
        applicationId: 7,
        newStatus: 'APPROVED',
      });
    });
  });
});
