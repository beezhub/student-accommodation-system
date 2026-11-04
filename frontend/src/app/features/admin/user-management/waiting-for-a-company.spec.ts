import { isWaitingForACompany } from './waiting-for-a-company';

/**
 * #413: a tenant admin created with no company waits on the platform tenant, locked, until a
 * company's admin screen adds it. In the user list it must not look like a working account.
 */
describe('isWaitingForACompany', () => {
  const now = new Date('2026-11-04T12:00:00');
  const waiting = {
    userRole: 'TENANT_ADMIN',
    tenantId: 0,
    accountLockedUntil: '9999-12-31T23:59:59',
  };

  it('isTrue_forATenantAdminWithNoCompanyThatIsStillLocked', () => {
    expect(isWaitingForACompany(waiting, now)).toBeTrue();
  });

  it('isFalse_onceACompanyHasAddedThem', () => {
    expect(
      isWaitingForACompany({ ...waiting, tenantId: 5, accountLockedUntil: undefined }, now)
    ).toBeFalse();
  });

  it('isFalse_forAnyOtherRole_onThePlatform', () => {
    expect(isWaitingForACompany({ ...waiting, userRole: 'PLATFORM_ADMIN' }, now)).toBeFalse();
    expect(isWaitingForACompany({ ...waiting, userRole: 'STUDENT' }, now)).toBeFalse();
  });

  it('isFalse_whenTheLockHasRunOut', () => {
    expect(
      isWaitingForACompany({ ...waiting, accountLockedUntil: '2026-11-04T11:00:00' }, now)
    ).toBeFalse();
  });

  it('isFalse_whenTheCompanyIsNotKnown', () => {
    expect(isWaitingForACompany({ ...waiting, tenantId: undefined }, now)).toBeFalse();
  });
});
