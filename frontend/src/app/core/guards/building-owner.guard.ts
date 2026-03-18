import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RoleService } from '../services/role.service';

export const BuildingOwnerGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const roleService = inject(RoleService);
  const router = inject(Router);

  if (authService.isTokenExpired()) {
    router.navigate(['/login']);
    return false;
  }

  if (roleService.isBuildingOwner()) {
    return true;
  }

  if (roleService.isPlatformAdmin()) {
    router.navigate(['/admin']);
  } else {
    router.navigate(['/dashboard']);
  }
  return false;
};
