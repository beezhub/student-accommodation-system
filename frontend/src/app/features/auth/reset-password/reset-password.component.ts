import { Component, OnInit, inject } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
  AbstractControl,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
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
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.css'],
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
export class ResetPasswordComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);

  resetPasswordForm: FormGroup;
  isSubmitting = false;
  isVerifying = true;
  tokenValid = false;
  resetSuccess = false;
  hidePassword = true;
  hideConfirmPassword = true;
  token: string = '';

  constructor() {
    this.resetPasswordForm = this.fb.group(
      {
        newPassword: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required]],
      },
      { validators: this.passwordMatchValidator }
    );
  }

  ngOnInit(): void {
    // Get token from query parameters
    this.route.queryParams.subscribe((params) => {
      this.token = params['token'] || '';
      if (this.token) {
        this.verifyToken();
      } else {
        this.isVerifying = false;
        this.tokenValid = false;
      }
    });
  }

  passwordMatchValidator(control: AbstractControl): { [key: string]: boolean } | null {
    const password = control.get('newPassword');
    const confirmPassword = control.get('confirmPassword');

    if (password && confirmPassword && password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }
    return null;
  }

  verifyToken(): void {
    this.isVerifying = true;
    this.authService.verifyResetToken(this.token).subscribe({
      next: (response) => {
        this.isVerifying = false;
        this.tokenValid = response.valid;
        if (!response.valid) {
          this.snackBar.open(response.message || 'Invalid or expired token', 'Close', {
            duration: 5000,
            panelClass: ['error-snackbar'],
          });
        }
      },
      error: (error: any) => {
        this.isVerifying = false;
        this.tokenValid = false;
        const errorMessage = error?.error?.message || 'Token verification failed';
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        this.snackBar.open(errorMessage, 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar'],
        });
      },
    });
  }

  onSubmit(): void {
    if (this.resetPasswordForm.valid) {
      this.isSubmitting = true;

      const newPassword = this.resetPasswordForm.get('newPassword')?.value;

      this.authService.resetPassword(this.token, newPassword).subscribe({
        next: (response) => {
          this.isSubmitting = false;
          this.resetSuccess = true;
          this.snackBar.open(response.message || 'Password reset successfully!', 'Close', {
            duration: 5000,
            panelClass: ['success-snackbar'],
          });
        },
        error: (error: any) => {
          this.isSubmitting = false;
          const errorMessage =
            error?.error?.message ||
            error?.message ||
            'Failed to reset password. Please try again.';
          this.snackBar.open(errorMessage, 'Close', {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            duration: 5000,
            panelClass: ['error-snackbar'],
          });
        },
      });
    } else {
      this.resetPasswordForm.markAllAsTouched();
      this.snackBar.open('Please fill in all fields correctly.', 'Close', {
        duration: 3000,
        panelClass: ['warning-snackbar'],
      });
    }
  }

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }

  toggleConfirmPasswordVisibility(): void {
    this.hideConfirmPassword = !this.hideConfirmPassword;
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
