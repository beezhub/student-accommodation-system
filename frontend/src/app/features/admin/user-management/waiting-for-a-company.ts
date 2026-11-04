/** The platform tenant. A tenant admin there has no company yet. */
const PLATFORM_TENANT_ID = 0;

/** What the rule needs to know about a user. */
export interface WaitingCandidate {
  userRole: string;
  tenantId?: number;
  accountLockedUntil?: string;
}

/**
 * True for a tenant admin made in user management with no company yet (#413). It waits on the
 * platform tenant, locked, and cannot sign in until a company's admin screen adds it, which also
 * unlocks it. A tenant admin with a company is never this, even when locked out for a while.
 */
export function isWaitingForACompany(user: WaitingCandidate, now: Date = new Date()): boolean {
  return (
    user.userRole === 'TENANT_ADMIN' &&
    user.tenantId === PLATFORM_TENANT_ID &&
    !!user.accountLockedUntil &&
    new Date(user.accountLockedUntil) > now
  );
}
