import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({
  providedIn: 'root',
})
export class EmailVerifiedGuard {
  constructor(private router: Router, private snackBar: MatSnackBar) {}

  canActivate(): boolean {
    const currentUser = localStorage.getItem('currentUser');

    if (!currentUser) {
      this.router.navigate(['/login']);
      return false;
    }

    try {
      const user = JSON.parse(currentUser);

      if (user.emailVerified === true) {
        return true;
      } else {
        this.snackBar.open(
          'Please verify your email address before accessing this feature. Check your inbox for the verification link.',
          'Close',
          {
            duration: 6000,
            panelClass: ['warning-snackbar'],
          }
        );
        this.router.navigate(['/dashboard'], {
          queryParams: { showVerificationBanner: true },
        });
        return false;
      }
    } catch {
      this.router.navigate(['/login']);
      return false;
    }
  }
}
