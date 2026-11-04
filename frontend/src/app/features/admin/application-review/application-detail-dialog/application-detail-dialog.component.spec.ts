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

  describe('loading', () => {
    it('loadApplicationDetails_backendFails_showsTheServerMessage', () => {
      create(details('PENDING'));
      applicationService.getApplicationFullDetails.and.returnValue(
        throwError(() => ({ error: { message: 'Not yours' } }))
      );

      component.loadApplicationDetails();

      expect(component.loadError).toBe('Not yours');
      expect(component.isLoading).toBeFalse();
    });

    it('loadApplicationDetails_noServerMessage_fallsBackToAGenericOne', () => {
      create(details('PENDING'));
      applicationService.getApplicationFullDetails.and.returnValue(throwError(() => ({})));

      component.loadApplicationDetails();

      expect(component.loadError).toBe('Failed to load application details');
    });
  });

  describe('checklist display', () => {
    it('getChecklistIcon_coversEveryStatus', () => {
      create(details('PENDING'));

      expect(component.getChecklistIcon(checklistItem(1, 'APPROVED'))).toBe('check_circle');
      expect(component.getChecklistIcon(checklistItem(1, 'PENDING'))).toBe('hourglass_empty');
      expect(component.getChecklistIcon(checklistItem(1, 'REJECTED'))).toBe('cancel');
      expect(component.getChecklistIcon(checklistItem(1, 'NOT_UPLOADED'))).toBe('cloud_upload');
      expect(component.getChecklistIcon(checklistItem(1, 'OTHER'))).toBe('help');
      expect(component.getChecklistClass(checklistItem(1, 'NOT_UPLOADED'))).toBe('not-uploaded');
    });

    it('checklistProgress_tracksApprovals', () => {
      create(details('PENDING', 'APPROVED'));
      expect(component.checklistProgress).toBe(100);

      component.documentChecklist = [];
      expect(component.checklistProgress).toBe(0);
    });

    it('toggles_openAndCloseOneDocumentAtATime', () => {
      create(details('PENDING'));

      component.toggleDocumentExpanded(1);
      expect(component.expandedDocumentId).toBe(1);
      component.toggleDocumentExpanded(1);
      expect(component.expandedDocumentId).toBeNull();

      component.toggleComments(2);
      expect(component.expandedCommentsId).toBe(2);
      component.toggleComments(2);
      expect(component.expandedCommentsId).toBeNull();
    });

    it('getInitials_handlesAMissingStudent', () => {
      create(details('PENDING'));

      expect(component.getInitials()).toBe('TM');
      component.student = null;
      expect(component.getInitials()).toBe('?');
    });

    it('formatDate_handlesAMissingDate', () => {
      create(details('PENDING'));

      expect(component.formatDate(undefined)).toBe('N/A');
      expect(component.formatDate('2026-01-15')).toContain('2026');
    });
  });

  describe('saveProgress failures', () => {
    it('saveProgress_nothingChanged_callsNothing', () => {
      create(details('UNDER_REVIEW'));

      component.saveProgress();

      expect(applicationService.saveReviewProgress).not.toHaveBeenCalled();
    });

    it('saveProgress_backendFails_keepsTheChanges', () => {
      create(details('UNDER_REVIEW'));
      applicationService.saveReviewProgress.and.returnValue(
        throwError(() => ({ error: { message: 'Refused' } }))
      );

      approveAndSave(1);

      expect(component.hasUnsavedChanges()).toBeTrue();
      expect(component.isSaving).toBeFalse();
    });

    it('onDocumentStatusChange_backToOriginal_dropsTheChange', () => {
      create(details('UNDER_REVIEW'));
      const doc = component.documents[0];

      component.onDocumentStatusChange(doc, 'APPROVED');
      expect(component.getDocumentCurrentStatus(doc)).toBe('APPROVED');
      component.onDocumentStatusChange(doc, 'PENDING');

      expect(component.hasUnsavedChanges()).toBeFalse();
      expect(component.getDocumentCurrentStatus(doc)).toBe('PENDING');
    });
  });

  describe('documents', () => {
    let downloadedBlob: Blob;

    beforeEach(() => {
      downloadedBlob = new Blob(['x'], { type: 'application/pdf' });
      spyOn(URL, 'createObjectURL').and.returnValue('blob:test');
      spyOn(URL, 'revokeObjectURL');
    });

    it('downloadDocument_success_savesTheFile', () => {
      create(details('PENDING'));
      adminService.downloadDocument.and.returnValue(of(downloadedBlob));
      const click = spyOn(HTMLAnchorElement.prototype, 'click');

      component.downloadDocument(component.documents[0]);

      expect(click).toHaveBeenCalled();
    });

    it('downloadDocument_backendFails_doesNotThrow', () => {
      create(details('PENDING'));
      adminService.downloadDocument.and.returnValue(throwError(() => new Error('boom')));

      expect(() => component.downloadDocument(component.documents[0])).not.toThrow();
    });

    function previewOf(type: string, fileName: string): string | undefined {
      adminService.downloadDocument.and.returnValue(of(new Blob(['x'], { type })));
      const doc = { ...component.documents[0], fileName };
      component.viewDocument(doc);
      const previewType = component.documentPreviewTypes.get(doc.id);
      component.viewDocument(doc);
      return previewType;
    }

    it('viewDocument_picksThePreviewFromTheTypeThenTheFileName', () => {
      create(details('PENDING'));

      expect(previewOf('application/pdf', 'a.bin')).toBe('pdf');
      expect(previewOf('image/png', 'a.bin')).toBe('image');
      expect(previewOf('application/octet-stream', 'a.pdf')).toBe('pdf');
      expect(previewOf('application/octet-stream', 'a.JPG')).toBe('image');
      expect(previewOf('application/octet-stream', 'a.bin')).toBe('unknown');
    });

    it('viewDocument_secondCall_closesThePreviewAndFreesTheUrl', () => {
      create(details('PENDING'));
      adminService.downloadDocument.and.returnValue(of(downloadedBlob));
      const doc = component.documents[0];

      component.viewDocument(doc);
      expect(component.documentPreviews.has(doc.id)).toBeTrue();
      component.viewDocument(doc);

      expect(component.documentPreviews.has(doc.id)).toBeFalse();
      expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:test');
    });

    it('retryPreview_afterAFailure_loadsAgain', () => {
      create(details('PENDING'));
      adminService.downloadDocument.and.returnValue(throwError(() => new Error('boom')));
      const doc = component.documents[0];

      component.viewDocument(doc);
      expect(component.documentPreviewErrors.get(doc.id)).toBeTrue();

      adminService.downloadDocument.and.returnValue(of(downloadedBlob));
      component.retryPreview(doc);

      expect(component.documentPreviewErrors.get(doc.id)).toBeFalse();
      expect(component.documentPreviewTypes.get(doc.id)).toBe('pdf');
    });

    it('ngOnDestroy_freesOpenPreviews', () => {
      create(details('PENDING'));
      adminService.downloadDocument.and.returnValue(of(downloadedBlob));
      component.viewDocument(component.documents[0]);

      component.ngOnDestroy();

      expect(component.documentPreviews.size).toBe(0);
    });
  });

  describe('rejection', () => {
    it('toggleRejectionForm_closing_clearsTheReason', () => {
      create(details('PENDING'));
      component.toggleRejectionForm();
      component.rejectionForm.patchValue({ reason: 'A long enough reason' });

      component.toggleRejectionForm();

      expect(component.showRejectionForm).toBeFalse();
      expect(component.rejectionForm.value.reason).toBeNull();
    });

    it('rejectApplication_noValidReason_callsNothing', () => {
      create(details('PENDING'));

      component.rejectApplication();

      expect(adminService.updateApplicationStatus).not.toHaveBeenCalled();
    });

    it('rejectApplication_validReason_sendsItAndCloses', () => {
      create(details('PENDING'));
      adminService.updateApplicationStatus.and.returnValue(of({} as ApplicationResponse));
      component.rejectionForm.patchValue({ reason: 'A long enough reason' });

      component.rejectApplication();

      expect(adminService.updateApplicationStatus).toHaveBeenCalledOnceWith(
        7,
        'REJECTED',
        'A long enough reason'
      );
      expect(TestBed.inject(MatDialogRef).close).toHaveBeenCalledOnceWith({
        action: 'rejected',
        applicationId: 7,
        newStatus: 'REJECTED',
      });
    });

    it('rejectApplication_backendFails_staysOpen', () => {
      create(details('PENDING'));
      adminService.updateApplicationStatus.and.returnValue(throwError(() => ({ error: {} })));
      component.rejectionForm.patchValue({ reason: 'A long enough reason' });

      component.rejectApplication();

      expect(TestBed.inject(MatDialogRef).close).not.toHaveBeenCalled();
      expect(component.isRejecting).toBeFalse();
    });
  });

  describe('approval guards', () => {
    it('approveApplication_documentsNotApproved_callsNothing', () => {
      create(details('UNDER_REVIEW'));

      component.approveApplication();

      expect(adminService.updateApplicationStatus).not.toHaveBeenCalled();
    });

    it('approveApplication_unsavedChanges_callsNothing', () => {
      create(details('UNDER_REVIEW', 'APPROVED'));
      component.onDocumentStatusChange(component.documents[0], 'REJECTED');
      component.documentChecklist =
        component.applicationDetails!.requiredDocumentsChecklist.documents;

      component.approveApplication();

      expect(adminService.updateApplicationStatus).not.toHaveBeenCalled();
    });

    it('approveApplication_autoAllocationAlreadyMovedIt_closesWithThatStatus', () => {
      create(details('UNDER_REVIEW', 'APPROVED'));
      adminService.updateApplicationStatus.and.returnValue(
        of({ id: 7, status: 'AWAITING_ALLOCATION' } as ApplicationResponse)
      );

      component.approveApplication();

      expect(applicationService.moveToAllocationPhase).not.toHaveBeenCalled();
      expect(TestBed.inject(MatDialogRef).close).toHaveBeenCalledOnceWith({
        action: 'allocated',
        applicationId: 7,
        newStatus: 'AWAITING_ALLOCATION',
      });
    });

    it('approveApplication_firstCallFails_staysAsItWas', () => {
      create(details('UNDER_REVIEW', 'APPROVED'));
      adminService.updateApplicationStatus.and.returnValue(throwError(() => ({ error: {} })));

      component.approveApplication();

      expect(component.applicationDetails!.status).toBe('UNDER_REVIEW');
      expect(component.isApproving).toBeFalse();
    });

    it('moveToAllocation_documentsNotApproved_callsNothing', () => {
      create(details('APPROVED'));

      component.moveToAllocation();

      expect(applicationService.moveToAllocationPhase).not.toHaveBeenCalled();
    });

    it('moveToAllocation_unsavedChanges_callsNothing', () => {
      create(details('APPROVED', 'APPROVED'));
      component.onDocumentStatusChange(component.documents[0], 'REJECTED');
      component.documentChecklist =
        component.applicationDetails!.requiredDocumentsChecklist.documents;

      component.moveToAllocation();

      expect(applicationService.moveToAllocationPhase).not.toHaveBeenCalled();
    });

    it('moveToAllocation_backendFails_staysOpen', () => {
      create(details('APPROVED', 'APPROVED'));
      applicationService.moveToAllocationPhase.and.returnValue(throwError(() => ({ error: {} })));

      component.moveToAllocation();

      expect(TestBed.inject(MatDialogRef).close).not.toHaveBeenCalled();
      expect(component.isMovingToAllocation).toBeFalse();
    });

    it('closeDialog_unsavedChangesKept_staysOpen', () => {
      create(details('PENDING'));
      component.onDocumentStatusChange(component.documents[0], 'APPROVED');
      spyOn(fixture.debugElement.injector.get(MatDialog), 'open').and.returnValue({
        afterClosed: () => of(false),
      } as unknown as MatDialogRef<unknown>);

      component.closeDialog();

      expect(TestBed.inject(MatDialogRef).close).not.toHaveBeenCalled();
    });

    it('closeDialog_unsavedChangesDiscarded_closes', () => {
      create(details('PENDING'));
      component.onDocumentStatusChange(component.documents[0], 'APPROVED');
      spyOn(fixture.debugElement.injector.get(MatDialog), 'open').and.returnValue({
        afterClosed: () => of(true),
      } as unknown as MatDialogRef<unknown>);

      component.closeDialog();

      expect(TestBed.inject(MatDialogRef).close).toHaveBeenCalledOnceWith({
        action: 'closed',
        applicationId: 7,
      });
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

    it('requestChanges_backendFails_staysOpen', () => {
      create(details('UNDER_REVIEW'));
      answerReasonDialog('Proof of registration is for the wrong year');
      applicationService.requestChanges.and.returnValue(throwError(() => ({ error: {} })));

      component.requestChanges();

      expect(TestBed.inject(MatDialogRef).close).not.toHaveBeenCalled();
      expect(component.isRequestingChanges).toBeFalse();
    });

    it('markUnderReview_backendFails_keepsTheStatus', () => {
      create(details('PENDING'));
      applicationService.markUnderReview.and.returnValue(throwError(() => ({ error: {} })));

      component.markUnderReview();

      expect(component.applicationDetails!.status).toBe('PENDING');
      expect(component.isMarkingUnderReview).toBeFalse();
    });

    it('nextStepHint_explainsEveryStatusWithoutADecision', () => {
      const hinted = [
        'CHANGES_REQUESTED',
        'AWAITING_ALLOCATION',
        'ALLOCATED',
        'CHECKED_IN',
        'REJECTED',
        'EXPIRED',
        'CANCELLED',
      ];
      create(details('PENDING'));
      expect(component.nextStepHint).toBeNull();

      hinted.forEach((status) => {
        component.applicationDetails = details(status);
        expect(component.nextStepHint).withContext(status).toBeTruthy();
      });
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
