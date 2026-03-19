import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth.service';
import { ROLE } from '../models/role.model';

/**
 * Pure facade over AuthService.currentUserValue for role checks.
 * Use this service instead of comparing userRole strings directly.
 */
@Injectable({
  providedIn: 'root',
})
export class RoleService {
  private authService = inject(AuthService);

  /** Returns the current user's role string, or null if not logged in. */
  get currentRole(): string | null {
    return this.authService.currentUserValue?.userRole ?? null;
  }

  /** True if the current user is a PLATFORM_ADMIN. */
  isPlatformAdmin(): boolean {
    return (
      this.currentRole?.toUpperCase() === ROLE.PLATFORM_ADMIN.toUpperCase() ||
      this.currentRole?.toUpperCase() === ROLE.ADMIN.toUpperCase()
    );
  }

  /** True if the current user is a TENANT_ADMIN. */
  isTenantAdmin(): boolean {
    return this.currentRole?.toUpperCase() === ROLE.TENANT_ADMIN.toUpperCase();
  }

  /** True if the current user is a BUILDING_STAFF. */
  isBuildingStaff(): boolean {
    return this.currentRole?.toUpperCase() === ROLE.BUILDING_STAFF.toUpperCase();
  }

  /** True if the current user is a STUDENT. */
  isStudent(): boolean {
    return this.currentRole?.toUpperCase() === ROLE.STUDENT.toUpperCase();
  }

  /**
   * True if the current user has any administrative role
   * (PLATFORM_ADMIN or BUILDING_STAFF).
   */
  isAdmin(): boolean {
    return this.isPlatformAdmin() || this.isTenantAdmin() || this.isBuildingStaff();
  }
}
