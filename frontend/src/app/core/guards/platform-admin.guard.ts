import { Injectable, inject } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RoleService } from '../services/role.service';

@Injectable({
  providedIn: 'root',
})
export class PlatformAdminGuard implements CanActivate {
  private authService = inject(AuthService);
  private roleService = inject(RoleService);
  private router = inject(Router);

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const currentUser = this.authService.currentUserValue;

    if (currentUser && this.roleService.isPlatformAdmin()) {
      return true;
    }

    // Redirect admins without governance access back to the admin dashboard. Platform staff land
    // here too: they hold /admin but not tenants, users, feature flags or platform defaults, and
    // the backend answers those with a 403 (GH-238).
    if (currentUser && (this.roleService.isTenantAdmin() || this.roleService.isPlatformStaff())) {
      this.router.navigate(['/admin']);
    } else if (currentUser) {
      this.router.navigate(['/dashboard']);
    } else {
      this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    }

    return false;
  }
}
