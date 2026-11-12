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
    // Reads the stored copy only, so a link opened on another device is not seen here; the
    // verify page asks the server and brings the student back to state.url.
    const user = this.authService.currentUserValue;

    if (!user) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
      return false;
    }

    if (user.emailVerified === true) {
      return true;
    } else {
      this.router.navigate(['/email-verification-required'], {
        queryParams: { returnUrl: state.url },
      });
      return false;
    }
  }
}
