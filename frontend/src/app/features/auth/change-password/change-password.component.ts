import { Component, OnInit } from '@angular/core';

import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-change-password',
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.css'],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
})
export class ChangePasswordComponent implements OnInit {
  changePasswordForm: FormGroup;
  isSubmitting = false;
  isForced = false;
  hideCurrentPassword = true;
  hideNewPassword = true;
  hideConfirmPassword = true;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {
    this.changePasswordForm = this.fb.group(
      {
        currentPassword: ['', [Validators.required]],
        newPassword: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required]],
      },
      {
        validators: this.passwordMatchValidator,
      }
    );
  }

  ngOnInit(): void {
    // Check if this is a forced password change
    this.route.queryParams.subscribe((params) => {
      this.isForced = params['forced'] === 'true';
    });
  }

  passwordMatchValidator(g: FormGroup) {
    const newPassword = g.get('newPassword')?.value;
    const confirmPassword = g.get('confirmPassword')?.value;

    if (newPassword === confirmPassword) {
      return null;
    }

    g.get('confirmPassword')?.setErrors({ mismatch: true });
    return { mismatch: true };
  }

  onSubmit(): void {
    if (this.changePasswordForm.valid) {
      this.isSubmitting = true;

      const currentPassword = this.changePasswordForm.get('currentPassword')?.value;
      const newPassword = this.changePasswordForm.get('newPassword')?.value;

      this.authService.changePassword(currentPassword, newPassword).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.snackBar.open('Password changed successfully!', 'Close', {
            duration: 3000,
            panelClass: ['success-snackbar'],
          });

          // Navigate based on user role after successful password change
          const currentUser = this.authService.currentUserValue;
          if (currentUser?.userRole === 'ADMIN') {
            this.router.navigate(['/admin']);
          } else {
            this.router.navigate(['/dashboard']);
          }
        },
        error: (error: { error: { message: string; errors: string[] } }) => {
          this.isSubmitting = false;
          const errorMessage =
            error.error.message ||
            error.error.errors?.[0] ||
            'Failed to change password. Please try again.';
          this.snackBar.open(errorMessage, 'Close', {
            duration: 5000,
            panelClass: ['error-snackbar'],
          });
        },
      });
    } else {
      this.changePasswordForm.markAllAsTouched();
      this.snackBar.open('Please fill in all required fields correctly.', 'Close', {
        duration: 3000,
        panelClass: ['warning-snackbar'],
      });
    }
  }

  toggleCurrentPasswordVisibility(): void {
    this.hideCurrentPassword = !this.hideCurrentPassword;
  }

  toggleNewPasswordVisibility(): void {
    this.hideNewPassword = !this.hideNewPassword;
  }

  toggleConfirmPasswordVisibility(): void {
    this.hideConfirmPassword = !this.hideConfirmPassword;
  }

  onCancel(): void {
    if (!this.isForced) {
      const currentUser = this.authService.currentUserValue;
      if (currentUser?.userRole === 'ADMIN') {
        this.router.navigate(['/admin']);
      } else {
        this.router.navigate(['/dashboard']);
      }
    }
  }
}
