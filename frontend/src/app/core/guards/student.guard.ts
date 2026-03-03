import { Injectable, inject } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RoleService } from '../services/role.service';

@Injectable({
  providedIn: 'root',
})
export class StudentGuard implements CanActivate {
  private authService = inject(AuthService);
  private roleService = inject(RoleService);
  private router = inject(Router);

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const currentUser = this.authService.currentUserValue;

    // Check if user is logged in and has STUDENT role
    if (currentUser && this.roleService.isStudent()) {
      return true;
    }

    // Not authorized
    if (this.roleService.isPlatformAdmin()) {
      // Admin trying to access student routes, redirect to admin dashboard
      this.router.navigate(['/admin']);
    } else if (currentUser) {
      // Other roles, redirect to appropriate page
      this.router.navigate(['/home']);
    } else {
      // User not logged in
      this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    }

    return false;
  }
}
