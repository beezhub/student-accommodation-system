import { TestBed } from '@angular/core/testing';
import { HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { of } from 'rxjs';

import { authInterceptor } from './auth.interceptor';
import { tokenExpiryInterceptor } from './token-expiry.interceptor';
import { TokenStorageService } from '../services/token-storage.service';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

/**
 * Every interceptor here was a deny-list: skip a few known public paths, treat everything else as
 * ours. That is only safe while every request goes to our backend, and one does not — the address
 * autocomplete on the building form calls nominatim.openstreetmap.org through the same HttpClient.
 *
 * <p>So `authInterceptor` attached the caller's bearer token to it. The caller is an administrator,
 * and the request really is transmitted: the host allows the `authorization` header and any origin,
 * and the content-security-policy already permits it.
 */
describe('interceptors: requests to third-party hosts', () => {
  const THIRD_PARTY = 'https://nominatim.openstreetmap.org/search?q=main+street';
  const OURS = `${environment.apiUrl}/building`;

  let tokenStorage: jasmine.SpyObj<TokenStorageService>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: { navigate: jasmine.Spy; url: string };

  /** Captures the request as it leaves the interceptor. */
  function run(interceptor: typeof authInterceptor, url: string): HttpRequest<unknown> {
    let seen!: HttpRequest<unknown>;
    const next: HttpHandlerFn = (req) => {
      seen = req;
      return of({} as HttpEvent<unknown>);
    };
    TestBed.runInInjectionContext(() => interceptor(new HttpRequest('GET', url), next)).subscribe();
    return seen;
  }

  beforeEach(() => {
    tokenStorage = jasmine.createSpyObj<TokenStorageService>('TokenStorageService', [
      'getToken',
      'isTokenValid',
      'removeToken',
    ]);
    // A real token in every case. Asserting the absence of a header proves nothing if the stub
    // returns nothing, because then no header is attached whatever the interceptor decides.
    tokenStorage.getToken.and.returnValue('a-real-looking-jwt');
    tokenStorage.isTokenValid.and.returnValue(true);

    authService = jasmine.createSpyObj<AuthService>('AuthService', ['logout']);
    router = { navigate: jasmine.createSpy('navigate'), url: '/admin/buildings' };

    TestBed.configureTestingModule({
      providers: [
        { provide: TokenStorageService, useValue: tokenStorage },
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
      ],
    });
  });

  // --------------------------------------------------------------- auth

  it('authInterceptor_ourApi_attachesTheToken', () => {
    expect(run(authInterceptor, OURS).headers.get('Authorization')).toBe(
      'Bearer a-real-looking-jwt'
    );
  });

  it('authInterceptor_thirdPartyHost_doesNotAttachTheToken', () => {
    expect(run(authInterceptor, THIRD_PARTY).headers.has('Authorization')).toBeFalse();
  });

  it('authInterceptor_publicAuthPath_doesNotAttachTheToken', () => {
    expect(
      run(authInterceptor, `${environment.apiUrl}/auth/login`).headers.has('Authorization')
    ).toBeFalse();
  });

  /** A relative URL is same-origin by definition, so it is ours whatever the base looks like. */
  it('authInterceptor_relativeUrl_attachesTheToken', () => {
    expect(run(authInterceptor, '/api/rooms').headers.has('Authorization')).toBeTrue();
  });

  /** Protocol-relative is off-origin despite the leading slashes. */
  it('authInterceptor_protocolRelativeUrl_doesNotAttachTheToken', () => {
    expect(run(authInterceptor, '//evil.example.com/x').headers.has('Authorization')).toBeFalse();
  });

  // ------------------------------------------------------- token expiry

  /**
   * A third-party URL is in none of the public lists, so an expired token turned typing in the
   * address field into a forced sign-out.
   */
  it('tokenExpiryInterceptor_thirdPartyHost_doesNotSignTheUserOut', () => {
    tokenStorage.isTokenValid.and.returnValue(false);

    run(tokenExpiryInterceptor, THIRD_PARTY);

    expect(authService.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('tokenExpiryInterceptor_ourApiWithAnExpiredToken_stillSignsTheUserOut', () => {
    tokenStorage.isTokenValid.and.returnValue(false);
    const next: HttpHandlerFn = () => of({} as HttpEvent<unknown>);

    TestBed.runInInjectionContext(() =>
      tokenExpiryInterceptor(new HttpRequest('GET', OURS), next)
    ).subscribe({ error: () => undefined });

    expect(authService.logout).toHaveBeenCalled();
  });
});
