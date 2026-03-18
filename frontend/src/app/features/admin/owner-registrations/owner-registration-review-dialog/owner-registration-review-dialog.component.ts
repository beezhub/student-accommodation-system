import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { OwnerRegistrationAdminService, OwnerRegistrationSummary } from '../owner-registration-admin.service';

@Component({
  selector: 'app-owner-registration-review-dialog',
  templateUrl: './owner-registration-review-dialog.component.html',
  styleUrls: ['./owner-registration-review-dialog.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDividerModule,
  ],
})
export class OwnerRegistrationReviewDialogComponent {
  private dialogRef = inject<MatDialogRef<OwnerRegistrationReviewDialogComponent>>(MatDialogRef);
  private service = inject(OwnerRegistrationAdminService);
  private snackBar = inject(MatSnackBar);

  isApproving = false;
  isRejecting = false;
  showRejectForm = false;
  rejectReason = '';

  constructor(@Inject(MAT_DIALOG_DATA) public registration: OwnerRegistrationSummary) {}

  approve(): void {
    this.isApproving = true;
    this.service.approve(this.registration.id).subscribe({
      next: () => {
        this.isApproving = false;
        this.snackBar.open(`Registration for ${this.registration.fullName} approved.`, 'Close', {
          duration: 4000,
          panelClass: ['success-snackbar'],
        });
        this.dialogRef.close(true);
      },
      error: () => {
        this.isApproving = false;
        this.snackBar.open('Failed to approve registration.', 'Close', {
          duration: 4000,
          panelClass: ['error-snackbar'],
        });
      },
    });
  }

  confirmReject(): void {
    this.isRejecting = true;
    this.service.reject(this.registration.id, this.rejectReason || undefined).subscribe({
      next: () => {
        this.isRejecting = false;
        this.snackBar.open(`Registration for ${this.registration.fullName} rejected.`, 'Close', {
          duration: 4000,
          panelClass: ['success-snackbar'],
        });
        this.dialogRef.close(true);
      },
      error: () => {
        this.isRejecting = false;
        this.snackBar.open('Failed to reject registration.', 'Close', {
          duration: 4000,
          panelClass: ['error-snackbar'],
        });
      },
    });
  }

  close(): void {
    this.dialogRef.close();
  }
}
