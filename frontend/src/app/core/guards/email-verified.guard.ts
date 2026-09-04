import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root',
})
export class EmailVerifiedGuard {
  private router = inject(Router);
  private authService = inject(AuthService);

  canActivate(): boolean {
    const user = this.authService.currentUserValue;

    if (!user) {
      this.router.navigate(['/login']);
      return false;
    }

    if (user.emailVerified === true) {
      return true;
    } else {
      this.router.navigate(['/email-verification-required']);
      return false;
    }
  }
}
