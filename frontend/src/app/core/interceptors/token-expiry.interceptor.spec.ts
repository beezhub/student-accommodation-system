import { TestBed } from '@angular/core/testing';
import { HttpEvent, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { tokenExpiryInterceptor } from './token-expiry.interceptor';
import { AuthService } from '../services/auth.service';
import { TokenStorageService } from '../services/token-storage.service';
import { environment } from '../../../environments/environment';

/**
 * GH-159: the interceptor that decides when a session ends had no spec of its own.
 *
 * <p>It is the only thing standing between an expired token and a request that would be refused
 * with a 401 the user cannot interpret. Getting it wrong in either direction is bad: too eager and
 * it signs people out mid-form, too lax and every screen fills with errors.
 */
describe('tokenExpiryInterceptor', () => {
  const OURS = `${environment.apiUrl}/applications`;
  const LOGIN = `${environment.apiUrl}/auth/login`;
  const THIRD_PARTY = 'https://nominatim.openstreetmap.org/search?q=main+street';

  let authService: jasmine.SpyObj<AuthService>;
  let tokenStorage: jasmine.SpyObj<TokenStorageService>;
  let router: { navigate: jasmine.Spy; url: string };
  let forwarded: boolean;

  /** Runs the interceptor and reports whether the request was passed on or stopped. */
  function run(url: string): { forwarded: boolean; errored: boolean } {
    forwarded = false;
    let errored = false;
    const next: HttpHandlerFn = () => {
      forwarded = true;
      return of({} as HttpEvent<unknown>);
    };
    TestBed.runInInjectionContext(() =>
      tokenExpiryInterceptor(new HttpRequest('GET', url), next)
    ).subscribe({ error: () => (errored = true) });
    return { forwarded, errored };
  }

  beforeEach(() => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['logout']);
    tokenStorage = jasmine.createSpyObj<TokenStorageService>('TokenStorageService', [
      'getToken',
      'isTokenValid',
    ]);
    // A real-looking token throughout: asserting that nothing happens proves nothing if the stub
    // returns no token, because then the expiry branch is unreachable whatever the code does.
    tokenStorage.getToken.and.returnValue('a-real-looking-jwt');
    tokenStorage.isTokenValid.and.returnValue(true);
    router = { navigate: jasmine.createSpy('navigate'), url: '/dashboard/applications' };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: TokenStorageService, useValue: tokenStorage },
        { provide: Router, useValue: router },
      ],
    });
  });

  it('validToken_forwardsTheRequest', () => {
    const result = run(OURS);

    expect(result.forwarded).toBeTrue();
    expect(authService.logout).not.toHaveBeenCalled();
  });

  it('expiredToken_stopsTheRequestAndSignsTheUserOut', () => {
    tokenStorage.isTokenValid.and.returnValue(false);

    const result = run(OURS);

    expect(result.forwarded).toBeFalse();
    expect(result.errored).toBeTrue();
    expect(authService.logout).toHaveBeenCalled();
  });

  /**
   * The return URL has to be read before logout(), which navigates to /home. Reading it after
   * happened to work only because navigation is deferred — the regression GH-150 fixed.
   */
  it('expiredToken_carriesTheUrlTheUserWasOnIntoTheLoginRedirect', () => {
    tokenStorage.isTokenValid.and.returnValue(false);
    router.url = '/dashboard/applications';
    authService.logout.and.callFake(() => {
      // What logout does to the router, so a read after it would capture the wrong page.
      router.url = '/home';
    });

    run(OURS);

    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/dashboard/applications' },
    });
  });

  /** Login must work with an expired token in storage, or a lapsed session cannot be replaced. */
  it('expiredToken_onAPublicAuthEndpoint_stillForwards', () => {
    tokenStorage.isTokenValid.and.returnValue(false);

    const result = run(LOGIN);

    expect(result.forwarded).toBeTrue();
    expect(authService.logout).not.toHaveBeenCalled();
  });

  /**
   * A third-party host is in neither list, so before the isOurApiRequest check an expired token
   * turned typing an address into a forced sign-out.
   */
  it('expiredToken_onAThirdPartyHost_forwardsWithoutSigningOut', () => {
    tokenStorage.isTokenValid.and.returnValue(false);

    const result = run(THIRD_PARTY);

    expect(result.forwarded).toBeTrue();
    expect(authService.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  /** An anonymous visitor has no session to expire. */
  it('noToken_forwardsWithoutSigningOut', () => {
    tokenStorage.getToken.and.returnValue(null);
    tokenStorage.isTokenValid.and.returnValue(false);

    const result = run(OURS);

    expect(result.forwarded).toBeTrue();
    expect(authService.logout).not.toHaveBeenCalled();
  });
});
