import { Component, Inject, OnInit } from '@angular/core';
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
  ApplicationDocument,
  DocumentTypeChecklist,
  StudentProfile,
} from '../../../../core/services/application.service';
import { AdminService } from '../../../../core/services/admin.service';
import { Application } from '../../../../core/models/application.model';
import { ModernStatusBadgeComponent } from '../../../../shared/modern-status-badge/modern-status-badge.component';
import { DocumentCommentSectionComponent } from '../../../shared/components/document-comment-section/document-comment-section.component';
import { TimeagoPipe } from '../../../../shared/pipes/timeago.pipe';

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
  styleUrls: ['./application-detail-dialog.component.css'],
})
export class ApplicationDetailDialogComponent implements OnInit {
  isLoading = true;
  isSaving = false;
  isRejecting = false;
  isMovingToAllocation = false;
  loadError: string | null = null;

  applicationDetails: ApplicationFullDetails | null = null;
  application: Application | null = null;
  student: StudentProfile | null = null;
  documents: ApplicationDocument[] = [];
  documentChecklist: DocumentTypeChecklist[] = [];

  // Track document status changes
  documentStatusChanges: Map<number, DocumentStatusChange> = new Map();

  // Rejection form
  rejectionForm: FormGroup;
  showRejectionForm = false;

  // Expanded document panels
  expandedDocumentId: number | null = null;

  constructor(
    private fb: FormBuilder,
    private applicationService: ApplicationService,
    private adminService: AdminService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    public dialogRef: MatDialogRef<ApplicationDetailDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ApplicationDetailDialogData
  ) {
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
        this.application = details.application;
        this.student = details.student;
        this.documents = details.documents;
        this.documentChecklist = details.documentChecklist;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load application details:', error);
        this.loadError = error.error?.message || 'Failed to load application details';
        this.isLoading = false;
      },
    });
  }

  // Document status management
  getDocumentCurrentStatus(doc: ApplicationDocument): string {
    const change = this.documentStatusChanges.get(doc.id);
    return change ? change.newStatus : doc.documentStatus;
  }

  onDocumentStatusChange(doc: ApplicationDocument, newStatus: string): void {
    if (doc.documentStatus === newStatus) {
      // If reverting to original, remove from changes
      this.documentStatusChanges.delete(doc.id);
    } else {
      this.documentStatusChanges.set(doc.id, {
        documentId: doc.id,
        originalStatus: doc.documentStatus,
        newStatus: newStatus,
      });
    }
    this.updateChecklistFromChanges();
  }

  private updateChecklistFromChanges(): void {
    // Update the checklist based on current status changes
    this.documentChecklist = this.documentChecklist.map((item) => {
      if (item.documentId) {
        const change = this.documentStatusChanges.get(item.documentId);
        if (change) {
          return { ...item, status: change.newStatus as any };
        }
      }
      return item;
    });
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
    return (
      this.requiredTotalCount > 0 && this.requiredApprovedCount === this.requiredTotalCount
    );
  }

  get checklistProgress(): number {
    if (this.requiredTotalCount === 0) return 0;
    return (this.requiredApprovedCount / this.requiredTotalCount) * 100;
  }

  getChecklistIcon(item: DocumentTypeChecklist): string {
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

  getChecklistClass(item: DocumentTypeChecklist): string {
    return item.status.toLowerCase().replace('_', '-');
  }

  // Document actions
  downloadDocument(doc: ApplicationDocument): void {
    this.adminService.downloadDocument(doc.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = doc.documentName;
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

  // Save progress
  saveProgress(): void {
    if (!this.hasUnsavedChanges()) {
      this.snackBar.open('No changes to save', 'Close', { duration: 2000 });
      return;
    }

    this.isSaving = true;
    const documentUpdates = Array.from(this.documentStatusChanges.values()).map((change) => ({
      documentId: change.documentId,
      status: change.newStatus,
    }));

    // Save each document status change
    let completed = 0;
    let failed = 0;
    const total = documentUpdates.length;

    documentUpdates.forEach((update) => {
      this.adminService.updateDocumentStatus(update.documentId, update.status).subscribe({
        next: () => {
          completed++;
          if (completed + failed === total) {
            this.finalizeSaveProgress(completed, failed);
          }
        },
        error: (error) => {
          console.error('Failed to update document status:', error);
          failed++;
          if (completed + failed === total) {
            this.finalizeSaveProgress(completed, failed);
          }
        },
      });
    });
  }

  private finalizeSaveProgress(completed: number, failed: number): void {
    this.isSaving = false;
    if (failed === 0) {
      this.snackBar.open('Progress saved successfully', 'Close', { duration: 3000 });
      // Update local document statuses
      this.documents = this.documents.map((doc) => {
        const change = this.documentStatusChanges.get(doc.id);
        if (change) {
          return { ...doc, documentStatus: change.newStatus };
        }
        return doc;
      });
      this.documentStatusChanges.clear();
    } else {
      this.snackBar.open(`${completed} saved, ${failed} failed`, 'Close', { duration: 5000 });
    }
  }

  // Move to allocation
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
      next: (updatedApp) => {
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
        this.snackBar.open(
          error.error?.message || 'Failed to move to allocation phase',
          'Close',
          { duration: 5000 }
        );
        this.isMovingToAllocation = false;
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
          this.snackBar.open(
            error.error?.message || 'Failed to reject application',
            'Close',
            { duration: 5000 }
          );
          this.isRejecting = false;
        },
      });
  }

  // Close dialog
  closeDialog(): void {
    if (this.hasUnsavedChanges()) {
      const confirmClose = confirm(
        'You have unsaved changes. Are you sure you want to close?'
      );
      if (!confirmClose) {
        return;
      }
    }
    this.dialogRef.close({
      action: 'closed',
      applicationId: this.data.applicationId,
    } as ApplicationDetailDialogResult);
  }

  // Utility methods
  formatDate(date: string | undefined): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  }
}
