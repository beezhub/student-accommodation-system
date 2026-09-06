import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { MustChangePasswordGuard } from './must-change-password.guard';
import { AuthService } from '../services/auth.service';
import { User } from '../models/user.model';

/**
 * GH-159: this guard gated every admin route and had no spec.
 *
 * <p>It is the frontend half of the backend's MustChangePasswordFilter, which rejects every
 * request except login, refresh, me, logout and change-password while the token carries the
 * claim. If the guard lets someone through, they reach a screen whose every call is refused.
 */
describe('MustChangePasswordGuard', () => {
  let guard: MustChangePasswordGuard;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const route = {} as ActivatedRouteSnapshot;
  const state = { url: '/admin' } as RouterStateSnapshot;

  function currentUserIs(user: Partial<User> | null): void {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue(user);
  }

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', [], {
      currentUserValue: null,
    });
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        MustChangePasswordGuard,
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });
    guard = TestBed.inject(MustChangePasswordGuard);
  });

  it('canActivate_userMustChangePassword_redirectsToChangePassword', () => {
    currentUserIs({ mustChangePassword: true });

    expect(guard.canActivate(route, state)).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/change-password'], {
      queryParams: { forced: 'true' },
    });
  });

  /** `forced` is what tells the change-password screen to hide its "cancel" route out. */
  it('canActivate_userMustChangePassword_marksTheRedirectAsForced', () => {
    currentUserIs({ mustChangePassword: true });

    guard.canActivate(route, state);

    const [, extras] = routerSpy.navigate.calls.mostRecent().args;
    expect(extras?.queryParams?.['forced']).toBe('true');
  });

  it('canActivate_userNeedNotChangePassword_returnsTrue', () => {
    currentUserIs({ mustChangePassword: false });

    expect(guard.canActivate(route, state)).toBeTrue();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  /** The claim is absent for every account that has never been force-reset. */
  it('canActivate_claimAbsent_returnsTrue', () => {
    currentUserIs({ email: 'someone@example.com' });

    expect(guard.canActivate(route, state)).toBeTrue();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  /**
   * Deliberately allowed through: this guard is only about the password claim, and AuthGuard is
   * what decides whether an anonymous visitor may proceed. Redirecting here too would send an
   * anonymous visitor to a change-password screen for an account they do not have.
   */
  it('canActivate_noUser_returnsTrueAndLeavesTheDecisionToAuthGuard', () => {
    currentUserIs(null);

    expect(guard.canActivate(route, state)).toBeTrue();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });
});
