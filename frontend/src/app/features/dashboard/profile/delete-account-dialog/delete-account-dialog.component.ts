import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-delete-account-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  template: `
    <h2 mat-dialog-title class="warning-title">
      <mat-icon>warning</mat-icon>
      Delete Account
    </h2>
    <mat-dialog-content>
      <div class="warning-box">
        <p><strong>Warning: This action cannot be undone!</strong></p>
        <p>Deleting your account will permanently remove:</p>
        <ul>
          <li>Your profile information</li>
          <li>All your applications</li>
          <li>Your documents</li>
          <li>Your room allocation (if any)</li>
        </ul>
      </div>

      <form [formGroup]="deleteForm">
        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Type "DELETE" to confirm</mat-label>
          <input matInput formControlName="confirmation" autocomplete="off" placeholder="DELETE">
          <mat-error *ngIf="deleteForm.get('confirmation')?.invalid && deleteForm.get('confirmation')?.touched">
            Please type DELETE to confirm
          </mat-error>
        </mat-form-field>

        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Enter your password</mat-label>
          <input matInput [type]="hidePassword ? 'password' : 'text'"
                 formControlName="password" autocomplete="current-password">
          <button mat-icon-button matSuffix (click)="hidePassword = !hidePassword"
                  type="button" [attr.aria-label]="'Hide password'" [attr.aria-pressed]="!hidePassword">
            <mat-icon>{{hidePassword ? 'visibility_off' : 'visibility'}}</mat-icon>
          </button>
          <mat-error *ngIf="deleteForm.get('password')?.invalid && deleteForm.get('password')?.touched">
            Password is required
          </mat-error>
        </mat-form-field>

        <div *ngIf="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()" [disabled]="isDeleting">Cancel</button>
      <button mat-flat-button color="warn" (click)="onDelete()"
              [disabled]="!deleteForm.valid || isDeleting">
        <mat-progress-spinner *ngIf="isDeleting" mode="indeterminate" diameter="18" strokeWidth="3"></mat-progress-spinner>
        <span>{{ isDeleting ? 'Deleting...' : 'Delete Account' }}</span>
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .full-width {
      width: 100%;
      margin-bottom: 16px;
    }

    mat-dialog-content {
      min-height: 350px;
      padding: 24px;
    }

    mat-dialog-actions {
      padding: 16px 24px;
      gap: 12px;
    }

    .warning-title {
      color: #f44336;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .warning-title mat-icon {
      font-size: 28px;
      width: 28px;
      height: 28px;
    }

    .warning-box {
      background-color: #fff3e0;
      border-left: 4px solid #ff9800;
      padding: 16px;
      margin-bottom: 24px;
      border-radius: 4px;
    }

    .warning-box p {
      margin: 0 0 12px 0;
    }

    .warning-box strong {
      color: #e65100;
    }

    .warning-box ul {
      margin: 8px 0 0 0;
      padding-left: 20px;
    }

    .warning-box li {
      margin: 4px 0;
    }

    .error-message {
      color: #f44336;
      font-size: 14px;
      margin-top: 8px;
      padding: 12px;
      background-color: #ffebee;
      border-radius: 4px;
    }

    button[mat-flat-button] {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    mat-progress-spinner {
      display: inline-block;
      margin-right: 8px;
    }
  `]
})
export class DeleteAccountDialogComponent {
  deleteForm: FormGroup;
  hidePassword = true;
  isDeleting = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<DeleteAccountDialogComponent>,
    private authService: AuthService
  ) {
    this.deleteForm = this.fb.group({
      confirmation: ['', [Validators.required, this.deleteConfirmationValidator]],
      password: ['', Validators.required]
    });
  }

  deleteConfirmationValidator(control: any) {
    return control.value === 'DELETE' ? null : { confirmationMismatch: true };
  }

  onDelete() {
    if (this.deleteForm.valid) {
      this.isDeleting = true;
      this.errorMessage = '';

      const password = this.deleteForm.value.password;

      this.authService.deleteAccount(password).subscribe({
        next: () => {
          this.isDeleting = false;
          this.dialogRef.close('deleted');
        },
        error: (error) => {
          this.isDeleting = false;
          this.errorMessage = error.error?.message || 'Failed to delete account. Please check your password.';
        }
      });
    }
  }

  onCancel() {
    this.dialogRef.close();
  }
}
