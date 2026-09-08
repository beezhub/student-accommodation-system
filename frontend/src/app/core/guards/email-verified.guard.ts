import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root',
})
export class EmailVerifiedGuard {
  private router = inject(Router);
  private authService = inject(AuthService);

  canActivate(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const user = this.authService.currentUserValue;

    if (!user) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
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
