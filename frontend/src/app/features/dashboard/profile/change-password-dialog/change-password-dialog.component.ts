import { Component, inject } from '@angular/core';

import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
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
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <div class="header-icon">
          <mat-icon>vpn_key</mat-icon>
        </div>
        <h2>Change Password</h2>
        <p>Update your password to keep your account secure</p>
      </div>

      <mat-dialog-content>
        <form [formGroup]="passwordForm">
          <div class="form-field-wrapper">
            <label class="field-label">
              <mat-icon>lock</mat-icon>
              Current Password
            </label>
            <mat-form-field appearance="outline" class="full-width">
              <input
                matInput
                [type]="hideCurrentPassword ? 'password' : 'text'"
                formControlName="currentPassword"
                placeholder="Enter current password"
                autocomplete="current-password"
              />
              <button
                mat-icon-button
                matSuffix
                (click)="hideCurrentPassword = !hideCurrentPassword"
                type="button"
                class="visibility-toggle"
              >
                <mat-icon>{{ hideCurrentPassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              @if (passwordForm.get('currentPassword')?.invalid &&
              passwordForm.get('currentPassword')?.touched) {
              <mat-error>Current password is required</mat-error>
              }
            </mat-form-field>
          </div>

          <div class="form-field-wrapper">
            <label class="field-label">
              <mat-icon>lock_open</mat-icon>
              New Password
            </label>
            <mat-form-field appearance="outline" class="full-width">
              <input
                matInput
                [type]="hideNewPassword ? 'password' : 'text'"
                formControlName="newPassword"
                placeholder="Enter new password"
                autocomplete="new-password"
              />
              <button
                mat-icon-button
                matSuffix
                (click)="hideNewPassword = !hideNewPassword"
                type="button"
                class="visibility-toggle"
              >
                <mat-icon>{{ hideNewPassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              @if (passwordForm.get('newPassword')?.hasError('required') &&
              passwordForm.get('newPassword')?.touched) {
              <mat-error>New password is required</mat-error>
              } @if (passwordForm.get('newPassword')?.hasError('minlength')) {
              <mat-error>Password must be at least 8 characters</mat-error>
              }
            </mat-form-field>
            <div class="password-hint">
              <mat-icon>info_outline</mat-icon>
              <span>Minimum 8 characters required</span>
            </div>
          </div>

          <div class="form-field-wrapper">
            <label class="field-label">
              <mat-icon>check_circle</mat-icon>
              Confirm Password
            </label>
            <mat-form-field appearance="outline" class="full-width">
              <input
                matInput
                [type]="hideConfirmPassword ? 'password' : 'text'"
                formControlName="confirmPassword"
                placeholder="Confirm new password"
                autocomplete="new-password"
              />
              <button
                mat-icon-button
                matSuffix
                (click)="hideConfirmPassword = !hideConfirmPassword"
                type="button"
                class="visibility-toggle"
              >
                <mat-icon>{{ hideConfirmPassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              @if (passwordForm.get('confirmPassword')?.hasError('required') &&
              passwordForm.get('confirmPassword')?.touched) {
              <mat-error>Please confirm your password</mat-error>
              } @if (passwordForm.get('confirmPassword')?.hasError('passwordMismatch')) {
              <mat-error>Passwords do not match</mat-error>
              }
            </mat-form-field>
          </div>

          @if (errorMessage) {
          <div class="error-message">
            <mat-icon>error_outline</mat-icon>
            <span>{{ errorMessage }}</span>
          </div>
          }
        </form>
      </mat-dialog-content>

      <mat-dialog-actions>
        <button
          mat-stroked-button
          (click)="onCancel()"
          [disabled]="isSubmitting"
          class="cancel-btn"
        >
          Cancel
        </button>
        <button
          mat-raised-button
          color="primary"
          (click)="onSubmit()"
          [disabled]="!passwordForm.valid || isSubmitting"
          class="submit-btn"
        >
          @if (isSubmitting) {
          <mat-spinner diameter="18" class="button-spinner"></mat-spinner>
          <span>Updating...</span>
          } @else {
          <mat-icon>vpn_key</mat-icon>
          <span>Change Password</span>
          }
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .dialog-container {
        min-width: 420px;
      }

      /* Dialog Header */
      .dialog-header {
        text-align: center;
        padding: 24px 24px 0 24px;
      }

      .header-icon {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 64px;
        height: 64px;
        border-radius: 16px;
        background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
        margin-bottom: 16px;
      }

      .header-icon mat-icon {
        font-size: 32px;
        width: 32px;
        height: 32px;
        color: #ffffff;
      }

      .dialog-header h2 {
        font-size: 22px;
        font-weight: 700;
        margin: 0 0 6px 0;
        color: #1e293b;
      }

      .dialog-header p {
        font-size: 13px;
        color: #64748b;
        margin: 0;
      }

      /* Dialog Content */
      mat-dialog-content {
        padding: 24px !important;
        max-height: 60vh;
      }

      /* Form Field Wrapper */
      .form-field-wrapper {
        margin-bottom: 16px;
      }

      .field-label {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 12px;
        font-weight: 600;
        color: #475569;
        margin-bottom: 6px;
        text-transform: uppercase;
        letter-spacing: 0.03em;
      }

      .field-label mat-icon {
        font-size: 14px;
        width: 14px;
        height: 14px;
        color: #64748b;
      }

      .full-width {
        width: 100%;
      }

      /* Visibility Toggle */
      .visibility-toggle {
        color: #94a3b8 !important;
      }

      .visibility-toggle:hover {
        color: #64748b !important;
      }

      /* Password Hint */
      .password-hint {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 11px;
        color: #94a3b8;
        margin-top: 4px;
      }

      .password-hint mat-icon {
        font-size: 12px;
        width: 12px;
        height: 12px;
      }

      /* Error Message */
      .error-message {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 12px 16px;
        background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
        border: 1px solid #fca5a5;
        border-radius: 10px;
        color: #dc2626;
        font-size: 13px;
        margin-top: 8px;
      }

      .error-message mat-icon {
        font-size: 18px;
        width: 18px;
        height: 18px;
        flex-shrink: 0;
      }

      /* Dialog Actions */
      mat-dialog-actions {
        padding: 16px 24px 24px 24px !important;
        gap: 12px;
        justify-content: flex-end;
      }

      .cancel-btn {
        height: 42px;
        border-radius: 10px !important;
        font-weight: 500;
        color: #64748b;
        border-color: #e2e8f0 !important;
        padding: 0 20px !important;
      }

      .cancel-btn:hover {
        background: #f8fafc !important;
      }

      .submit-btn {
        height: 42px;
        border-radius: 10px !important;
        font-weight: 600;
        padding: 0 20px !important;
        display: flex !important;
        align-items: center;
        gap: 6px;
      }

      .submit-btn mat-icon {
        font-size: 18px;
        width: 18px;
        height: 18px;
      }

      .button-spinner {
        margin-right: 6px;
      }

      /* Responsive */
      @media (max-width: 480px) {
        .dialog-container {
          min-width: 100%;
        }

        mat-dialog-actions {
          flex-direction: column-reverse;
        }

        .cancel-btn,
        .submit-btn {
          width: 100%;
        }
      }
    `,
  ],
})
export class ChangePasswordDialogComponent {
  private fb = inject(FormBuilder);
  private dialogRef = inject<MatDialogRef<ChangePasswordDialogComponent>>(MatDialogRef);
  private authService = inject(AuthService);

  passwordForm: FormGroup;
  hideCurrentPassword = true;
  hideNewPassword = true;
  hideConfirmPassword = true;
  isSubmitting = false;
  errorMessage = '';

  constructor() {
    this.passwordForm = this.fb.group(
      {
        currentPassword: ['', Validators.required],
        newPassword: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', Validators.required],
      },
      { validators: this.passwordMatchValidator }
    );
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
          this.errorMessage =
            error.error?.message ||
            'Failed to change password. Please check your current password.';
        },
      });
    }
  }

  onCancel() {
    this.dialogRef.close();
  }
}
