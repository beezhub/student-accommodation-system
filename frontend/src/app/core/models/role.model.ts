/**
 * v2.0 Role model constants.
 *
 * Use these constants instead of raw strings throughout the application.
 * The deprecated ADMIN and LANDLORD values are kept for backward compatibility
 * during the migration window only.
 */
export const ROLE = {
  STUDENT: 'Student',
  PLATFORM_ADMIN: 'Platform_Admin',
  BUILDING_OWNER: 'Building_Owner',
  BUILDING_STAFF: 'Building_Staff',
  /** @deprecated Use PLATFORM_ADMIN */
  ADMIN: 'Admin',
  /** @deprecated Use BUILDING_OWNER */
  LANDLORD: 'Landlord',
} as const;

export type UserRoleValue = (typeof ROLE)[keyof typeof ROLE];

/** Roles that have administrative access to the admin dashboard. */
export const ADMIN_ROLES: UserRoleValue[] = [
  ROLE.PLATFORM_ADMIN,
  ROLE.BUILDING_OWNER,
  ROLE.BUILDING_STAFF,
];
