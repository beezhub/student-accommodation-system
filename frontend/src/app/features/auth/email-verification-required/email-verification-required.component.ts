import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

@Component({
  selector: 'app-email-verification-required',
  templateUrl: './email-verification-required.component.html',
  styleUrls: ['./email-verification-required.component.css'],
  standalone: true,
  imports: [
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
})
export class EmailVerificationRequiredComponent {
  isResending = false;
  currentUser = this.authService.currentUserValue;

  constructor(
    private router: Router,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {
    // If user is already verified, redirect to dashboard
    if (this.currentUser?.emailVerified === true) {
      this.router.navigate(['/dashboard']);
    }
  }

  resendVerificationEmail(): void {
    if (this.isResending || !this.currentUser?.email) return;

    this.isResending = true;
    this.authService.resendVerificationEmail(this.currentUser.email).subscribe({
      next: () => {
        this.isResending = false;
        this.snackBar.open('Verification email sent! Please check your inbox.', 'Close', {
          duration: 5000,
          panelClass: ['success-snackbar'],
        });
      },
      error: (error: { error?: { message?: string } }) => {
        this.isResending = false;
        this.snackBar.open(
          error.error?.message || 'Failed to send verification email. Please try again.',
          'Close',
          {
            duration: 5000,
            panelClass: ['error-snackbar'],
          }
        );
      },
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  checkVerificationStatus(): void {
    // Refresh user data from the /me endpoint
    this.snackBar.open('Checking verification status...', 'Close', {
      duration: 2000,
    });

    // For now, check localStorage - in production you'd want to call /api/auth/me
    const storedUser = localStorage.getItem('currentUser');
    if (storedUser) {
      try {
        const user = JSON.parse(storedUser);
        if (user.emailVerified === true) {
          this.snackBar.open('Email verified! Redirecting...', 'Close', {
            duration: 2000,
            panelClass: ['success-snackbar'],
          });
          this.router.navigate(['/dashboard']);
        } else {
          this.snackBar.open(
            'Email not yet verified. Please check your inbox and click the verification link.',
            'Close',
            {
              duration: 4000,
              panelClass: ['warning-snackbar'],
            }
          );
        }
      } catch {
        // Invalid JSON, ignore
      }
    }
  }
}
