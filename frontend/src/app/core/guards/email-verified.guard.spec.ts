import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { EmailVerifiedGuard } from './email-verified.guard';
import { AuthService } from '../services/auth.service';
import { User } from '../models/user.model';

/**
 * GH-159: no spec, despite this being the only place the app checks email verification at all.
 *
 * <p>Worth noting what these assert and what they cannot: the guard is a client-side routing
 * decision, and signup issues a token immediately, so a verified-looking client is not proof of
 * anything to the server. GH-153 covers enforcing it where it counts.
 */
describe('EmailVerifiedGuard', () => {
  let guard: EmailVerifiedGuard;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const route = {} as ActivatedRouteSnapshot;
  const state = { url: '/dashboard' } as RouterStateSnapshot;

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
        EmailVerifiedGuard,
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });
    guard = TestBed.inject(EmailVerifiedGuard);
  });

  it('canActivate_verifiedUser_returnsTrue', () => {
    currentUserIs({ emailVerified: true });

    expect(guard.canActivate(route, state)).toBeTrue();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('canActivate_unverifiedUser_redirectsToTheVerificationNotice', () => {
    currentUserIs({ emailVerified: false });

    expect(guard.canActivate(route, state)).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/email-verification-required'], {
      queryParams: { returnUrl: '/dashboard' },
    });
  });

  /**
   * The check is `=== true`, so a missing flag is treated as unverified. That is the safe reading
   * of an absent claim, and it is asserted here so a later change to `!== false` is a decision
   * rather than an accident.
   */
  it('canActivate_flagAbsent_isTreatedAsUnverified', () => {
    currentUserIs({ email: 'someone@example.com' });

    expect(guard.canActivate(route, state)).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/email-verification-required'], {
      queryParams: { returnUrl: '/dashboard' },
    });
  });

  it('canActivate_noUser_redirectsToLoginCarryingTheReturnUrl', () => {
    currentUserIs(null);

    expect(guard.canActivate(route, state)).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/dashboard' },
    });
  });
});
