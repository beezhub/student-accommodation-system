import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';

import {
  ApplicationService,
  ApplicationFullDetails,
  DocumentWithComments,
  DocumentChecklistItem,
  StudentProfile,
} from '../../../../core/services/application.service';
import { AdminService } from '../../../../core/services/admin.service';

import { ModernStatusBadgeComponent } from '../../../../shared/modern-status-badge/modern-status-badge.component';
import { DocumentCommentSectionComponent } from '../../../shared/components/document-comment-section/document-comment-section.component';
import { TimeagoPipe } from '../../../../shared/pipes/timeago.pipe';
import { DomSanitizer, SafeResourceUrl, SafeUrl } from '@angular/platform-browser';
import { Subscription } from 'rxjs';
import { confirmAction } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';
import { askForReason } from '../../../../shared/components/reason-dialog/reason-dialog.component';
import { ApplicationStatus, canApplicationTransition } from '../../../../core/models/status.model';

export interface ApplicationDetailDialogData {
  applicationId: number;
}

export interface ApplicationDetailDialogResult {
  action: 'closed' | 'saved' | 'allocated' | 'rejected';
  applicationId: number;
  newStatus?: string;
}

interface DocumentStatusChange {
  documentId: number;
  originalStatus: string;
  newStatus: string;
}

@Component({
  selector: 'app-application-detail-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatCardModule,
    MatDividerModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatExpansionModule,
    MatListModule,
    MatMenuModule,
    ModernStatusBadgeComponent,
    DocumentCommentSectionComponent,
    TimeagoPipe,
  ],
  templateUrl: './application-detail-dialog.component.html',
  styleUrls: ['./application-detail-dialog.component.scss'],
})
export class ApplicationDetailDialogComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private applicationService = inject(ApplicationService);
  private adminService = inject(AdminService);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);
  dialogRef = inject<MatDialogRef<ApplicationDetailDialogComponent>>(MatDialogRef);
  data = inject<ApplicationDetailDialogData>(MAT_DIALOG_DATA);
  private sanitizer = inject(DomSanitizer);

  isLoading = true;
  isSaving = false;
  isRejecting = false;
  isApproving = false;
  isMovingToAllocation = false;
  isMarkingUnderReview = false;
  isRequestingChanges = false;
  loadError: string | null = null;

  // Set when the status moved while the dialog stayed open, so closing still refreshes the list
  private statusMoved = false;

  applicationDetails: ApplicationFullDetails | null = null;
  student: StudentProfile | null = null;
  documents: DocumentWithComments[] = [];
  documentChecklist: DocumentChecklistItem[] = [];

  // Track document status changes
  documentStatusChanges: Map<number, DocumentStatusChange> = new Map();

  // Rejection form
  rejectionForm: FormGroup;
  showRejectionForm = false;

  // Expanded document panels
  expandedDocumentId: number | null = null;

  // Document preview states
  documentPreviews: Map<number, SafeResourceUrl | SafeUrl | null> = new Map();
  documentPreviewTypes: Map<number, 'pdf' | 'image' | 'unknown'> = new Map();
  documentLoadingStates: Map<number, boolean> = new Map();
  documentPreviewErrors: Map<number, boolean> = new Map();
  private documentPreviewSubscriptions: Map<number, Subscription> = new Map();

  // Expanded comments section
  expandedCommentsId: number | null = null;

  constructor() {
    this.rejectionForm = this.fb.group({
      reason: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]],
    });
  }

  ngOnInit(): void {
    this.loadApplicationDetails();
  }

  loadApplicationDetails(): void {
    this.isLoading = true;
    this.loadError = null;

    this.applicationService.getApplicationFullDetails(this.data.applicationId).subscribe({
      next: (details) => {
        this.applicationDetails = details;
        this.student = details.studentProfile;
        this.documents = details.documents || [];
        this.documentChecklist = details.requiredDocumentsChecklist?.documents || [];
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load application details:', error);
        this.loadError = error.error?.message || 'Failed to load application details';
        this.isLoading = false;
      },
    });
  }

  // Review flow. The reviewer claims a pending application, reads the documents, then decides here.
  get isBusy(): boolean {
    return (
      this.isSaving ||
      this.isMovingToAllocation ||
      this.isRejecting ||
      this.isApproving ||
      this.isMarkingUnderReview ||
      this.isRequestingChanges
    );
  }

  get canMarkUnderReview(): boolean {
    return canApplicationTransition(
      this.applicationDetails?.status,
      ApplicationStatus.UNDER_REVIEW
    );
  }

  get canRequestChanges(): boolean {
    return canApplicationTransition(
      this.applicationDetails?.status,
      ApplicationStatus.CHANGES_REQUESTED
    );
  }

  // Not the state machine alone: ALLOCATED may also move to AWAITING_ALLOCATION, but that is the
  // offer-cancelled path, and the move-to-allocation endpoint only accepts APPROVED.
  get canMoveToAllocation(): boolean {
    return this.applicationDetails?.status === ApplicationStatus.APPROVED;
  }

  get canReject(): boolean {
    return canApplicationTransition(this.applicationDetails?.status, ApplicationStatus.REJECTED);
  }

  // saveReviewProgress refuses these three statuses, so offering the controls only ends in an error
  get canReviewDocuments(): boolean {
    const status = this.applicationDetails?.status;
    return (
      status !== ApplicationStatus.REJECTED &&
      status !== ApplicationStatus.EXPIRED &&
      status !== ApplicationStatus.CANCELLED
    );
  }

  /** What happens next when this dialog has no decision to offer. */
  get nextStepHint(): string | null {
    switch (this.applicationDetails?.status) {
      case ApplicationStatus.CHANGES_REQUESTED:
        return 'Waiting for the student to make the changes and resubmit.';
      case ApplicationStatus.AWAITING_ALLOCATION:
        return 'Ready for a room. Use Manual or Auto Allocation in the row menu of the list.';
      case ApplicationStatus.ALLOCATED:
        return 'A room is allocated. Manage it from the allocations screen.';
      case ApplicationStatus.CHECKED_IN:
        return 'The student has checked in. The review is complete.';
      case ApplicationStatus.REJECTED:
      case ApplicationStatus.EXPIRED:
      case ApplicationStatus.CANCELLED:
        return 'This application is closed. No further action is possible.';
      default:
        return null;
    }
  }

  markUnderReview(): void {
    this.isMarkingUnderReview = true;
    this.applicationService.markUnderReview(this.data.applicationId).subscribe({
      next: (updated) => {
        this.snackBar.open('Application marked as under review', 'Close', { duration: 3000 });
        this.isMarkingUnderReview = false;
        this.recordStatus(updated.status);
      },
      error: (error) => {
        console.error('Failed to mark under review:', error);
        this.snackBar.open(error.error?.message || 'Failed to mark under review', 'Close', {
          duration: 5000,
        });
        this.isMarkingUnderReview = false;
      },
    });
  }

  requestChanges(): void {
    // The reason reaches the student as the whole explanation of what to fix, so it is validated
    // rather than accepted as whatever `prompt` returned, which included a single space (GH-157).
    askForReason(this.dialog, {
      title: 'Request changes',
      message: 'The student sees this, and it is the only guidance they get on what to change.',
      label: 'What needs to change?',
      placeholder: 'e.g. The proof of registration is for the wrong academic year.',
      confirmLabel: 'Request changes',
    }).subscribe((reason) => {
      if (!reason) return;
      this.isRequestingChanges = true;
      this.applicationService.requestChanges(this.data.applicationId, reason).subscribe({
        next: (updated) => {
          this.snackBar.open('Changes requested from the student', 'Close', { duration: 3000 });
          this.isRequestingChanges = false;
          this.dialogRef.close({
            action: 'saved',
            applicationId: this.data.applicationId,
            newStatus: updated.status,
          } as ApplicationDetailDialogResult);
        },
        error: (error) => {
          console.error('Failed to request changes:', error);
          this.snackBar.open(error.error?.message || 'Failed to request changes', 'Close', {
            duration: 5000,
          });
          this.isRequestingChanges = false;
        },
      });
    });
  }

  private recordStatus(status: string): void {
    if (this.applicationDetails) {
      this.applicationDetails = { ...this.applicationDetails, status };
    }
    this.statusMoved = true;
  }

  // Document status management
  getDocumentCurrentStatus(doc: DocumentWithComments): string {
    const change = this.documentStatusChanges.get(doc.id);
    return change ? change.newStatus : doc.status;
  }

  onDocumentStatusChange(doc: DocumentWithComments, newStatus: string): void {
    if (doc.status === newStatus) {
      // If reverting to original, remove from changes
      this.documentStatusChanges.delete(doc.id);
    } else {
      this.documentStatusChanges.set(doc.id, {
        documentId: doc.id,
        originalStatus: doc.status,
        newStatus: newStatus,
      });
    }
    this.updateChecklistFromChanges();
  }

  private updateChecklistFromChanges(): void {
    // Update the checklist based on current status changes
    if (!this.applicationDetails?.requiredDocumentsChecklist?.documents) return;

    this.documentChecklist = this.applicationDetails.requiredDocumentsChecklist.documents.map(
      (item) => {
        if (item.documentId) {
          const change = this.documentStatusChanges.get(item.documentId);
          if (change) {
            return { ...item, status: change.newStatus as typeof item.status };
          }
        }
        return item;
      }
    );
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  }

  hasUnsavedChanges(): boolean {
    return this.documentStatusChanges.size > 0;
  }

  // Checklist calculations
  get requiredApprovedCount(): number {
    return this.documentChecklist.filter((c) => c.isRequired && c.status === 'APPROVED').length;
  }

  get requiredTotalCount(): number {
    return this.documentChecklist.filter((c) => c.isRequired).length;
  }

  get allRequiredApproved(): boolean {
    return this.requiredTotalCount > 0 && this.requiredApprovedCount === this.requiredTotalCount;
  }

  get checklistProgress(): number {
    if (this.requiredTotalCount === 0) return 0;
    return (this.requiredApprovedCount / this.requiredTotalCount) * 100;
  }

  getChecklistIcon(item: DocumentChecklistItem): string {
    switch (item.status) {
      case 'APPROVED':
        return 'check_circle';
      case 'PENDING':
        return 'hourglass_empty';
      case 'REJECTED':
        return 'cancel';
      case 'NOT_UPLOADED':
        return 'cloud_upload';
      default:
        return 'help';
    }
  }

  getChecklistClass(item: DocumentChecklistItem): string {
    return item.status.toLowerCase().replace('_', '-');
  }

  // Document actions
  downloadDocument(doc: DocumentWithComments): void {
    this.adminService.downloadDocument(doc.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = doc.fileName;
        link.click();
        window.URL.revokeObjectURL(url);
        this.snackBar.open('Document downloaded', 'Close', { duration: 2000 });
      },
      error: (error) => {
        console.error('Failed to download document:', error);
        this.snackBar.open('Failed to download document', 'Close', { duration: 3000 });
      },
    });
  }

  toggleDocumentExpanded(docId: number): void {
    this.expandedDocumentId = this.expandedDocumentId === docId ? null : docId;
  }

  toggleComments(docId: number): void {
    this.expandedCommentsId = this.expandedCommentsId === docId ? null : docId;
  }

  // Save progress
  saveProgress(): void {
    if (!this.hasUnsavedChanges()) {
      this.snackBar.open('No changes to save', 'Close', { duration: 2000 });
      return;
    }

    this.isSaving = true;
    const documentUpdates = Array.from(this.documentStatusChanges.values()).map((change) => ({
      documentId: change.documentId,
      newStatus: change.newStatus,
    }));

    const request = {
      applicationId: this.data.applicationId,
      documentUpdates: documentUpdates,
    };

    this.applicationService.saveReviewProgress(request).subscribe({
      next: (response) => {
        if (response.success) {
          this.snackBar.open('Progress saved successfully', 'Close', { duration: 3000 });
          // Update local document statuses
          this.documents = this.documents.map((doc) => {
            const change = this.documentStatusChanges.get(doc.id);
            if (change) {
              return { ...doc, status: change.newStatus };
            }
            return doc;
          });
          // documentChecklist already has these changes applied. Make it the list the next change
          // rebuilds from, or the next rebuild drops what was just saved.
          if (this.applicationDetails?.requiredDocumentsChecklist) {
            this.applicationDetails.requiredDocumentsChecklist.documents = this.documentChecklist;
          }
          this.documentStatusChanges.clear();
        } else {
          this.snackBar.open('Failed to save progress', 'Close', { duration: 3000 });
        }
        this.isSaving = false;
      },
      error: (error) => {
        console.error('Failed to save progress:', error);
        this.snackBar.open(error.error?.message || 'Failed to save progress', 'Close', {
          duration: 5000,
        });
        this.isSaving = false;
      },
    });
  }
  moveToAllocation(): void {
    if (!this.allRequiredApproved) {
      this.snackBar.open('All required documents must be approved first', 'Close', {
        duration: 3000,
      });
      return;
    }

    // Save any pending changes first
    if (this.hasUnsavedChanges()) {
      this.snackBar.open('Please save your progress before moving to allocation', 'Close', {
        duration: 3000,
      });
      return;
    }

    this.isMovingToAllocation = true;
    this.applicationService.moveToAllocationPhase(this.data.applicationId).subscribe({
      next: () => {
        this.snackBar.open('Application moved to allocation phase', 'Close', { duration: 3000 });
        this.isMovingToAllocation = false;
        this.dialogRef.close({
          action: 'allocated',
          applicationId: this.data.applicationId,
          newStatus: 'AWAITING_ALLOCATION',
        } as ApplicationDetailDialogResult);
      },
      error: (error) => {
        console.error('Failed to move to allocation:', error);
        this.snackBar.open(error.error?.message || 'Failed to move to allocation phase', 'Close', {
          duration: 5000,
        });
        this.isMovingToAllocation = false;
      },
    });
  }

  // Approve Application and move to allocation
  approveApplication(): void {
    if (!this.allRequiredApproved) {
      this.snackBar.open('All required documents must be approved first', 'Close', {
        duration: 3000,
      });
      return;
    }

    if (this.hasUnsavedChanges()) {
      this.snackBar.open('Please save your progress before approving', 'Close', {
        duration: 3000,
      });
      return;
    }

    this.isApproving = true;

    // First approve the application, then move to allocation
    this.adminService.updateApplicationStatus(this.data.applicationId, 'APPROVED').subscribe({
      next: (updatedApp) => {
        // If auto-allocation already moved the app past APPROVED, skip moveToAllocationPhase
        if (updatedApp.status !== 'APPROVED') {
          this.snackBar.open('Application approved and moved to allocation', 'Close', {
            duration: 3000,
          });
          this.isApproving = false;
          this.dialogRef.close({
            action: 'allocated',
            applicationId: this.data.applicationId,
            newStatus: updatedApp.status,
          } as ApplicationDetailDialogResult);
          return;
        }
        // Application is APPROVED but not yet allocated — explicitly move to allocation phase
        this.applicationService.moveToAllocationPhase(this.data.applicationId).subscribe({
          next: () => {
            this.snackBar.open('Application approved and moved to allocation', 'Close', {
              duration: 3000,
            });
            this.isApproving = false;
            this.dialogRef.close({
              action: 'allocated',
              applicationId: this.data.applicationId,
              newStatus: 'AWAITING_ALLOCATION',
            } as ApplicationDetailDialogResult);
          },
          error: (error) => {
            console.error('Failed to move to allocation:', error);
            this.snackBar.open(
              `Application approved, but not moved to allocation: ${
                error.error?.message || 'please try again'
              }`,
              'Close',
              {
                duration: 5000,
              }
            );
            this.isApproving = false;
            // Stay open: the approval stands, and Move to Allocation is the way to finish it.
            this.recordStatus(ApplicationStatus.APPROVED);
          },
        });
      },
      error: (error) => {
        console.error('Failed to approve application:', error);
        this.snackBar.open(error.error?.message || 'Failed to approve application', 'Close', {
          duration: 5000,
        });
        this.isApproving = false;
      },
    });
  }

  // Rejection
  toggleRejectionForm(): void {
    this.showRejectionForm = !this.showRejectionForm;
    if (!this.showRejectionForm) {
      this.rejectionForm.reset();
    }
  }

  rejectApplication(): void {
    if (this.rejectionForm.invalid) {
      return;
    }

    this.isRejecting = true;
    const reason = this.rejectionForm.get('reason')?.value;

    this.adminService
      .updateApplicationStatus(this.data.applicationId, 'REJECTED', reason)
      .subscribe({
        next: () => {
          this.snackBar.open('Application rejected', 'Close', { duration: 3000 });
          this.isRejecting = false;
          this.dialogRef.close({
            action: 'rejected',
            applicationId: this.data.applicationId,
            newStatus: 'REJECTED',
          } as ApplicationDetailDialogResult);
        },
        error: (error) => {
          console.error('Failed to reject application:', error);
          this.snackBar.open(error.error?.message || 'Failed to reject application', 'Close', {
            duration: 5000,
          });
          this.isRejecting = false;
        },
      });
  }

  // Close dialog
  closeDialog(): void {
    if (!this.hasUnsavedChanges()) {
      this.close();
      return;
    }
    confirmAction(this.dialog, {
      title: 'Discard your changes?',
      message: 'This review has unsaved changes. Closing now loses them.',
      confirmLabel: 'Discard and close',
      cancelLabel: 'Keep editing',
      destructive: true,
    }).subscribe((discard) => {
      if (discard) this.close();
    });
  }

  private close(): void {
    const result: ApplicationDetailDialogResult = this.statusMoved
      ? {
          action: 'saved',
          applicationId: this.data.applicationId,
          newStatus: this.applicationDetails?.status,
        }
      : { action: 'closed', applicationId: this.data.applicationId };
    this.dialogRef.close(result);
  }

  // Utility methods
  getInitials(): string {
    if (!this.student) return '?';
    const first = (this.student.firstName || '').charAt(0);
    const last = (this.student.lastName || '').charAt(0);
    return (first + last).toUpperCase() || '?';
  }

  formatDate(date: string | undefined): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  }

  // Document preview methods
  viewDocument(doc: DocumentWithComments): void {
    // Toggle preview off if already showing
    if (this.documentPreviews.has(doc.id)) {
      this.cleanupDocumentPreview(doc.id);
      return;
    }

    // Start loading the preview
    this.loadDocumentPreview(doc);
  }

  retryPreview(doc: DocumentWithComments): void {
    this.loadDocumentPreview(doc);
  }

  private loadDocumentPreview(doc: DocumentWithComments): void {
    // Cancel any in-flight request for this doc
    this.documentPreviewSubscriptions.get(doc.id)?.unsubscribe();

    this.documentLoadingStates.set(doc.id, true);
    this.documentPreviewErrors.set(doc.id, false);
    this.documentPreviews.set(doc.id, null); // Mark as loading

    const sub = this.adminService.downloadDocument(doc.id).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);

        // Determine file type: prefer Content-Type header via blob.type,
        // fall back to filename extension (backend may serve application/octet-stream)
        let fileType: 'pdf' | 'image' | 'unknown' = 'unknown';
        const mimeType = blob.type.toLowerCase();
        if (mimeType.includes('pdf')) {
          fileType = 'pdf';
        } else if (mimeType.startsWith('image/')) {
          fileType = 'image';
        } else {
          const fileName = (doc.fileName ?? '').toLowerCase();
          if (fileName.endsWith('.pdf')) {
            fileType = 'pdf';
          } else if (/\.(jpe?g|png|gif|webp|bmp|svg)$/.test(fileName)) {
            fileType = 'image';
          }
        }

        this.documentPreviewTypes.set(doc.id, fileType);
        // Use SafeUrl for images (SecurityContext.URL), SafeResourceUrl for iframes (SecurityContext.RESOURCE_URL)
        const safeUrl =
          fileType === 'image'
            ? this.sanitizer.bypassSecurityTrustUrl(url)
            : this.sanitizer.bypassSecurityTrustResourceUrl(url);
        this.documentPreviews.set(doc.id, safeUrl);
        this.documentLoadingStates.set(doc.id, false);
        this.documentPreviewSubscriptions.delete(doc.id);
      },
      error: (error) => {
        console.error('Failed to load document preview:', error);
        this.documentLoadingStates.set(doc.id, false);
        this.documentPreviewErrors.set(doc.id, true);
        this.documentPreviewSubscriptions.delete(doc.id);
      },
    });

    this.documentPreviewSubscriptions.set(doc.id, sub);
  }

  private cleanupDocumentPreview(docId: number): void {
    // Cancel any in-flight request
    this.documentPreviewSubscriptions.get(docId)?.unsubscribe();
    this.documentPreviewSubscriptions.delete(docId);

    // Revoke the blob URL to free memory
    const previewUrl = this.documentPreviews.get(docId);
    if (previewUrl) {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const urlString = (previewUrl as any).changingThisBreaksApplicationSecurity;
      if (typeof urlString === 'string' && urlString.startsWith('blob:')) {
        URL.revokeObjectURL(urlString);
      }
    }

    this.documentPreviews.delete(docId);
    this.documentPreviewTypes.delete(docId);
    this.documentLoadingStates.delete(docId);
    this.documentPreviewErrors.delete(docId);
  }

  ngOnDestroy(): void {
    // Cancel all in-flight preview requests and revoke blob URLs
    const docIds = new Set([
      ...this.documentPreviews.keys(),
      ...this.documentPreviewSubscriptions.keys(),
    ]);
    docIds.forEach((docId) => this.cleanupDocumentPreview(docId));
  }
}
