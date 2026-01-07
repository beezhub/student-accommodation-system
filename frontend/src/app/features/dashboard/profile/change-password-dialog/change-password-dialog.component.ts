import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-change-password-dialog',
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
    <h2 mat-dialog-title>Change Password</h2>
    <mat-dialog-content>
      <form [formGroup]="passwordForm">
        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Current Password</mat-label>
          <input matInput [type]="hideCurrentPassword ? 'password' : 'text'"
                 formControlName="currentPassword" autocomplete="current-password">
          <button mat-icon-button matSuffix (click)="hideCurrentPassword = !hideCurrentPassword"
                  type="button" [attr.aria-label]="'Hide password'" [attr.aria-pressed]="!hideCurrentPassword">
            <mat-icon>{{hideCurrentPassword ? 'visibility_off' : 'visibility'}}</mat-icon>
          </button>
          <mat-error *ngIf="passwordForm.get('currentPassword')?.invalid && passwordForm.get('currentPassword')?.touched">
            Current password is required
          </mat-error>
        </mat-form-field>

        <mat-form-field appearance="fill" class="full-width">
          <mat-label>New Password</mat-label>
          <input matInput [type]="hideNewPassword ? 'password' : 'text'"
                 formControlName="newPassword" autocomplete="new-password">
          <button mat-icon-button matSuffix (click)="hideNewPassword = !hideNewPassword"
                  type="button" [attr.aria-label]="'Hide password'" [attr.aria-pressed]="!hideNewPassword">
            <mat-icon>{{hideNewPassword ? 'visibility_off' : 'visibility'}}</mat-icon>
          </button>
          <mat-error *ngIf="passwordForm.get('newPassword')?.hasError('required') && passwordForm.get('newPassword')?.touched">
            New password is required
          </mat-error>
          <mat-error *ngIf="passwordForm.get('newPassword')?.hasError('minlength')">
            Password must be at least 8 characters
          </mat-error>
        </mat-form-field>

        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Confirm New Password</mat-label>
          <input matInput [type]="hideConfirmPassword ? 'password' : 'text'"
                 formControlName="confirmPassword" autocomplete="new-password">
          <button mat-icon-button matSuffix (click)="hideConfirmPassword = !hideConfirmPassword"
                  type="button" [attr.aria-label]="'Hide password'" [attr.aria-pressed]="!hideConfirmPassword">
            <mat-icon>{{hideConfirmPassword ? 'visibility_off' : 'visibility'}}</mat-icon>
          </button>
          <mat-error *ngIf="passwordForm.get('confirmPassword')?.hasError('required') && passwordForm.get('confirmPassword')?.touched">
            Please confirm your password
          </mat-error>
          <mat-error *ngIf="passwordForm.get('confirmPassword')?.hasError('passwordMismatch')">
            Passwords do not match
          </mat-error>
        </mat-form-field>

        <div *ngIf="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()" [disabled]="isSubmitting">Cancel</button>
      <button mat-flat-button color="primary" (click)="onSubmit()"
              [disabled]="!passwordForm.valid || isSubmitting">
        <mat-progress-spinner *ngIf="isSubmitting" mode="indeterminate" diameter="18" strokeWidth="3"></mat-progress-spinner>
        <span>{{ isSubmitting ? 'Changing...' : 'Change Password' }}</span>
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .full-width {
      width: 100%;
      margin-bottom: 16px;
    }

    mat-dialog-content {
      min-height: 300px;
      padding: 24px;
    }

    mat-dialog-actions {
      padding: 16px 24px;
      gap: 12px;
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
export class ChangePasswordDialogComponent {
  passwordForm: FormGroup;
  hideCurrentPassword = true;
  hideNewPassword = true;
  hideConfirmPassword = true;
  isSubmitting = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<ChangePasswordDialogComponent>,
    private authService: AuthService
  ) {
    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const newPassword = control.get('newPassword');
    const confirmPassword = control.get('confirmPassword');

    if (!newPassword || !confirmPassword) {
      return null;
    }

    if (newPassword.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    } else {
      const errors = confirmPassword.errors;
      if (errors) {
        delete errors['passwordMismatch'];
        if (Object.keys(errors).length === 0) {
          confirmPassword.setErrors(null);
        }
      }
      return null;
    }
  }

  onSubmit() {
    if (this.passwordForm.valid) {
      this.isSubmitting = true;
      this.errorMessage = '';

      const { currentPassword, newPassword } = this.passwordForm.value;

      this.authService.changePassword(currentPassword, newPassword).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.dialogRef.close('success');
        },
        error: (error) => {
          this.isSubmitting = false;
          this.errorMessage = error.error?.message || 'Failed to change password. Please check your current password.';
        }
      });
    }
  }

  onCancel() {
    this.dialogRef.close();
  }
}
