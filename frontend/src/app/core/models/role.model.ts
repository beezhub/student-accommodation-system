/**
 * v2.0 Role model constants.
 *
 * Use these constants instead of raw strings throughout the application.
 * The deprecated ADMIN value is kept for backward compatibility
 * during the migration window only.
 */
export const ROLE = {
  STUDENT: 'Student',
  PLATFORM_ADMIN: 'Platform_Admin',
  TENANT_ADMIN: 'Tenant_Admin',
  BUILDING_STAFF: 'Building_Staff',
  /** @deprecated Use PLATFORM_ADMIN */
  ADMIN: 'Admin',
} as const;

export type UserRoleValue = (typeof ROLE)[keyof typeof ROLE];

/** Roles that have administrative access to the admin dashboard. */
export const ADMIN_ROLES: UserRoleValue[] = [
  ROLE.PLATFORM_ADMIN,
  ROLE.TENANT_ADMIN,
  ROLE.BUILDING_STAFF,
];
