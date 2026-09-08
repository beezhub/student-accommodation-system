import { Injectable, inject } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RoleService } from '../services/role.service';

@Injectable({
  providedIn: 'root',
})
export class AdminGuard implements CanActivate {
  private authService = inject(AuthService);
  private roleService = inject(RoleService);
  private router = inject(Router);

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const currentUser = this.authService.currentUserValue;

    // Platform staff hold the same shared admin surface as a tenant admin, only across every
    // company, so /admin as a whole is theirs; the governance children are held back by
    // PlatformAdminGuard instead (GH-238).
    if (
      currentUser &&
      (this.roleService.isPlatformAdmin() ||
        this.roleService.isPlatformStaff() ||
        this.roleService.isTenantAdmin())
    ) {
      return true;
    }

    // Not authorized, redirect to dashboard or login
    if (currentUser) {
      // User is logged in but not an admin
      this.router.navigate(['/dashboard']);
    } else {
      // User not logged in
      this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    }

    return false;
  }
}
