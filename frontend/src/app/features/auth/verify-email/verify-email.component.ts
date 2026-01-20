import { Component, OnInit } from '@angular/core';

import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-verify-email',
  templateUrl: './verify-email.component.html',
  styleUrls: ['./verify-email.component.css'],
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
export class VerifyEmailComponent implements OnInit {
  isVerifying = true;
  verificationSuccess = false;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.isVerifying = false;
      this.errorMessage = 'No verification token provided. Please check the link in your email.';
      return;
    }

    this.authService.verifyEmail(token).subscribe({
      next: () => {
        this.isVerifying = false;
        this.verificationSuccess = true;
        this.authService.updateEmailVerificationStatus(true);
        this.snackBar.open('Email verified successfully!', 'Close', {
          duration: 5000,
          panelClass: ['success-snackbar'],
        });
      },
      error: (error: { error?: { message?: string } }) => {
        this.isVerifying = false;
        this.verificationSuccess = false;
        this.errorMessage =
          error.error?.message || 'Email verification failed. The link may be expired or invalid.';
      },
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  resendVerification(): void {
    const currentUser = localStorage.getItem('currentUser');
    if (currentUser) {
      try {
        const user = JSON.parse(currentUser);
        this.authService.resendVerificationEmail(user.email).subscribe({
          next: () => {
            this.snackBar.open('Verification email sent! Please check your inbox.', 'Close', {
              duration: 5000,
              panelClass: ['success-snackbar'],
            });
          },
          error: (error: { error?: { message?: string } }) => {
            this.snackBar.open(
              error.error?.message || 'Failed to resend verification email.',
              'Close',
              {
                duration: 5000,
                panelClass: ['error-snackbar'],
              }
            );
          },
        });
      } catch {
        this.snackBar.open('Please log in to resend verification email.', 'Close', {
          duration: 5000,
          panelClass: ['warning-snackbar'],
        });
        this.router.navigate(['/login']);
      }
    } else {
      this.snackBar.open('Please log in to resend verification email.', 'Close', {
        duration: 5000,
        panelClass: ['warning-snackbar'],
      });
      this.router.navigate(['/login']);
    }
  }
}
