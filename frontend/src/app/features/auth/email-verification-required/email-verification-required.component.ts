import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { safeReturnUrl } from '../../../core/utils/safe-return-url';
import { AuthService } from '../../../core/services/auth.service';
import { ErrorHandlingService } from '../../../core/services/error-handling.service';

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
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);
  private errorService = inject(ErrorHandlingService);

  isResending = false;
  currentUser = this.authService.currentUserValue;

  constructor() {
    if (this.currentUser?.emailVerified === true) {
      this.goOn();
    }
  }

  /** Back to the page that sent the student here (step 4, or a guarded route), else the dashboard. */
  private goOn(): void {
    const target = safeReturnUrl(this.route.snapshot.queryParamMap.get('returnUrl'));
    if (target) {
      this.router.navigateByUrl(target);
      return;
    }
    this.router.navigate(['/dashboard']);
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
    this.snackBar.open('Checking verification status...', 'Close', {
      duration: 2000,
    });

    // Asks the backend, because the stored copy cannot know about a link clicked on another
    // device, and refreshing it is what lets EmailVerifiedGuard through (#442).
    this.authService.refreshCurrentUser().subscribe({
      next: (user) => {
        if (user.emailVerified === true) {
          this.snackBar.open('Email verified! Redirecting...', 'Close', {
            duration: 2000,
            panelClass: ['success-snackbar'],
          });
          this.goOn();
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
      },
      error: (error: unknown) => {
        this.snackBar.open(this.errorService.parseError(error).message, 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar'],
        });
      },
    });
  }
}
