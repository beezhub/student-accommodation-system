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
import {
  PASSWORD_RULES_HINT,
  strongPasswordValidator,
} from '../../../../shared/validators/strong-password.validator';

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
        <div class="header-icon-wrap">
          <mat-icon class="header-icon" aria-hidden="true">lock</mat-icon>
        </div>
        <div>
          <h2>Change Password</h2>
          <p>Keep your account secure with a strong password</p>
        </div>
      </div>

      <mat-dialog-content>
        <form [formGroup]="passwordForm">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Current Password</mat-label>
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
            >
              <mat-icon>{{ hideCurrentPassword ? 'visibility_off' : 'visibility' }}</mat-icon>
            </button>
            @if (passwordForm.get('currentPassword')?.invalid &&
            passwordForm.get('currentPassword')?.touched) {
            <mat-error>Current password is required</mat-error>
            }
          </mat-form-field>

          <div class="field-gap"></div>

          <mat-form-field appearance="outline" class="full-width" subscriptSizing="dynamic">
            <mat-label>New Password</mat-label>
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
            >
              <mat-icon>{{ hideNewPassword ? 'visibility_off' : 'visibility' }}</mat-icon>
            </button>
            @if (passwordForm.get('newPassword')?.hasError('required') &&
            passwordForm.get('newPassword')?.touched) {
            <mat-error>New password is required</mat-error>
            }
            <mat-hint>{{ passwordRulesHint }}</mat-hint>
            @if (passwordForm.get('newPassword')?.getError('strongPassword'); as rule) {
            <mat-error>Password needs {{ rule.missing.join(', ') }}</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Confirm New Password</mat-label>
            <input
              matInput
              [type]="hideConfirmPassword ? 'password' : 'text'"
              formControlName="confirmPassword"
              placeholder="Re-enter new password"
              autocomplete="new-password"
            />
            <button
              mat-icon-button
              matSuffix
              (click)="hideConfirmPassword = !hideConfirmPassword"
              type="button"
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

          @if (errorMessage) {
          <div class="error-alert" role="alert">
            <mat-icon aria-hidden="true">error_outline</mat-icon>
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
          mat-flat-button
          color="primary"
          (click)="onSubmit()"
          [disabled]="!passwordForm.valid || isSubmitting"
          class="submit-btn"
        >
          @if (isSubmitting) {
          <mat-spinner diameter="16" class="btn-spinner"></mat-spinner>
          <span>Updating...</span>
          } @else {
          <span>Update Password</span>
          }
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .dialog-container {
        min-width: 440px;
        max-width: 100%;
      }

      .dialog-header {
        display: flex;
        align-items: center;
        gap: 1rem;
        padding: 1.5rem 1.5rem 0;
      }

      .header-icon-wrap {
        width: 44px;
        height: 44px;
        border-radius: 12px;
        background: #f0f9ff;
        border: 1px solid #bae6fd;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }

      .header-icon {
        color: #0891b2;
        font-size: 22px;
        width: 22px;
        height: 22px;
      }

      .dialog-header h2 {
        font-size: 1.0625rem;
        font-weight: 700;
        color: #0f172a;
        margin: 0 0 0.2rem;
      }

      .dialog-header p {
        font-size: 0.8125rem;
        color: #64748b;
        margin: 0;
      }

      mat-dialog-content {
        padding: 1.5rem !important;
        max-height: 60vh;
      }

      .full-width {
        width: 100%;
      }

      .field-gap {
        height: 0.5rem;
      }

      .error-alert {
        display: flex;
        align-items: center;
        gap: 0.625rem;
        padding: 0.75rem 1rem;
        background: #fef2f2;
        border: 1px solid #fca5a5;
        border-radius: 8px;
        color: #dc2626;
        font-size: 0.875rem;
        margin-top: 0.5rem;
      }

      .error-alert mat-icon {
        font-size: 1.125rem;
        width: 1.125rem;
        height: 1.125rem;
        flex-shrink: 0;
      }

      mat-dialog-actions {
        padding: 1rem 1.5rem 1.5rem !important;
        gap: 0.75rem;
        justify-content: flex-end;
      }

      .cancel-btn {
        height: 40px;
        border-radius: 8px !important;
        font-weight: 500;
        color: #64748b;
        border-color: #e2e8f0 !important;
        padding: 0 1.25rem !important;
        cursor: pointer;
      }

      .cancel-btn:hover {
        background: #f8fafc !important;
      }

      .submit-btn {
        height: 40px;
        border-radius: 8px !important;
        font-weight: 600;
        padding: 0 1.5rem !important;
        display: flex !important;
        align-items: center;
        gap: 0.5rem;
        cursor: pointer;
      }

      .btn-spinner {
        display: inline-block;
      }

      @media (max-width: 480px) {
        .dialog-container {
          min-width: unset;
        }

        mat-dialog-actions {
          flex-direction: column-reverse;
        }

        .cancel-btn,
        .submit-btn {
          width: 100%;
          justify-content: center;
        }
      }
    `,
  ],
})
export class ChangePasswordDialogComponent {
  private fb = inject(FormBuilder);
  private dialogRef = inject<MatDialogRef<ChangePasswordDialogComponent>>(MatDialogRef);
  private authService = inject(AuthService);

  readonly passwordRulesHint = PASSWORD_RULES_HINT;
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
        newPassword: ['', [Validators.required, strongPasswordValidator]],
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
            error.error?.errors?.[0] ||
            'Failed to change password. Please check your current password.';
        },
      });
    }
  }

  onCancel() {
    this.dialogRef.close();
  }
}
