import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { ApplicationDetailDialogComponent } from './application-detail-dialog.component';
import {
  ApplicationFullDetails,
  ApplicationService,
  DocumentChecklistItem,
  DocumentWithComments,
} from '../../../../core/services/application.service';
import { AdminService } from '../../../../core/services/admin.service';

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
});
