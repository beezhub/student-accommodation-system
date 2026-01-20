import { Injectable } from '@angular/core';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root',
})
export class EmailVerifiedGuard {
  constructor(private router: Router) {}

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
        // Redirect to email verification required page
        this.router.navigate(['/email-verification-required']);
        return false;
      }
    } catch {
      this.router.navigate(['/login']);
      return false;
    }
  }
}
