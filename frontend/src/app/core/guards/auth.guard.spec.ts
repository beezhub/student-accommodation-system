import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { AuthGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

/**
 * GH-150: this guard sits on fifteen routes and declared `canActivate(): boolean`, with no route or
 * state parameters, so it could not say where the user had been headed. Every other guard wrote a
 * returnUrl that nothing read; this one could not write one at all.
 */
describe('AuthGuard', () => {
  let guard: AuthGuard;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockRoute = {} as ActivatedRouteSnapshot;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['isTokenExpired']);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        AuthGuard,
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });
    guard = TestBed.inject(AuthGuard);
  });

  it('canActivate_validToken_returnsTrueWithoutNavigating', () => {
    authServiceSpy.isTokenExpired.and.returnValue(false);

    expect(guard.canActivate(mockRoute, { url: '/admin/rooms' } as RouterStateSnapshot)).toBeTrue();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('canActivate_expiredToken_redirectsToLoginCarryingTheAttemptedUrl', () => {
    authServiceSpy.isTokenExpired.and.returnValue(true);

    expect(
      guard.canActivate(mockRoute, { url: '/admin/rooms' } as RouterStateSnapshot)
    ).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/admin/rooms' },
    });
  });

  it('canActivate_expiredToken_preservesQueryStringInReturnUrl', () => {
    authServiceSpy.isTokenExpired.and.returnValue(true);

    guard.canActivate(mockRoute, { url: '/admin/rooms?building=3' } as RouterStateSnapshot);

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/admin/rooms?building=3' },
    });
  });
});
