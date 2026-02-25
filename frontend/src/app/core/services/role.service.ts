import { Injectable } from '@angular/core';
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
  constructor(private authService: AuthService) {}

  /** Returns the current user's role string, or null if not logged in. */
  get currentRole(): string | null {
    return this.authService.currentUserValue?.userRole ?? null;
  }

  /** True if the current user is a PLATFORM_ADMIN. */
  isPlatformAdmin(): boolean {
    return this.currentRole === ROLE.PLATFORM_ADMIN;
  }

  /** True if the current user is a BUILDING_OWNER. */
  isBuildingOwner(): boolean {
    return this.currentRole === ROLE.BUILDING_OWNER;
  }

  /** True if the current user is a BUILDING_STAFF. */
  isBuildingStaff(): boolean {
    return this.currentRole === ROLE.BUILDING_STAFF;
  }

  /** True if the current user is a STUDENT. */
  isStudent(): boolean {
    return this.currentRole === ROLE.STUDENT;
  }

  /**
   * True if the current user has any administrative role
   * (PLATFORM_ADMIN, BUILDING_OWNER, or BUILDING_STAFF).
   */
  isAdmin(): boolean {
    const role = this.currentRole;
    return (
      role === ROLE.PLATFORM_ADMIN || role === ROLE.BUILDING_OWNER || role === ROLE.BUILDING_STAFF
    );
  }
}
