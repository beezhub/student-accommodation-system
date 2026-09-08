import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard {
  private router = inject(Router);
  private authService = inject(AuthService);

  /**
   * Takes the route arguments purely to read state.url. Without them the guard could not say
   * where the user was headed, so every guard in the app wrote a returnUrl that nothing read and
   * this one could not write it at all (GH-150). Adding parameters is compatible with all
   * fifteen existing registrations.
   */
  canActivate(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    if (!this.authService.isTokenExpired()) {
      return true;
    } else {
      this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
      return false;
    }
  }
}
