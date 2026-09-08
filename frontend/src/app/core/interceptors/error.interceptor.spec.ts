import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse, HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, throwError, of } from 'rxjs';
import { errorInterceptor } from './error.interceptor';
import { TokenStorageService } from '../services/token-storage.service';

/**
 * GH-150: a 403 navigated to `/unauthorized`, a route that does not exist, so the wildcard dropped
 * the user on the public landing page. Worse, `handleSpecificErrors` runs before the
 * `X-Silent-Error` check, so a background poll could evict a user mid-task with no message at all.
 *
 * <p>The backend returns 403 for refused actions, not only forbidden pages, so the user must stay
 * where they are and read the snackbar.
 */
describe('errorInterceptor', () => {
  // A plain object, deliberately not jasmine.createSpyObj with a property bag: that installs a
  // non-configurable get/set spy pair for `url`, so assigning to it does not change what the
  // interceptor reads and any assertion about the captured URL is vacuous.
  let router: { navigate: jasmine.Spy; navigateByUrl: jasmine.Spy; url: string };
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let tokenStorage: jasmine.SpyObj<TokenStorageService>;

  const run = (request: HttpRequest<unknown>, error: HttpErrorResponse): Promise<unknown> => {
    const next: HttpHandlerFn = () => throwError(() => error) as Observable<HttpEvent<unknown>>;
    return new Promise((resolve) => {
      TestBed.runInInjectionContext(() => errorInterceptor(request, next)).subscribe({
        next: () => resolve(null),
        error: (e) => resolve(e),
      });
    });
  };

  beforeEach(() => {
    router = {
      navigate: jasmine.createSpy('navigate'),
      navigateByUrl: jasmine.createSpy('navigateByUrl'),
      url: '/admin/system-settings',
    };
    snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    tokenStorage = jasmine.createSpyObj<TokenStorageService>('TokenStorageService', [
      'removeToken',
    ]);

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: TokenStorageService, useValue: tokenStorage },
      ],
    });
  });

  it('forbiddenResponse_leavesTheUserOnThePage', async () => {
    await run(
      new HttpRequest('GET', '/api/admin/system-settings'),
      new HttpErrorResponse({ status: 403, url: '/api/admin/system-settings' })
    );

    expect(router.navigate).not.toHaveBeenCalled();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('forbiddenResponse_stillShowsTheMessage', async () => {
    await run(
      new HttpRequest('GET', '/api/admin/system-settings'),
      new HttpErrorResponse({
        status: 403,
        error: { message: 'Only a platform admin can change this setting' },
      })
    );

    expect(snackBar.open).toHaveBeenCalled();
  });

  // GH-204: the backend's ErrorResponse puts its message in `errors`, a field nothing here read,
  // so every handled backend error showed the generic message for its status code instead.
  it('errorResponseBody_showsTheBackendMessageRatherThanTheGenericOne', async () => {
    await run(
      new HttpRequest('GET', '/api/applications'),
      new HttpErrorResponse({
        status: 409,
        error: {
          timestamp: '2026-09-06T10:00:00',
          status: 409,
          errors: ['You already have an active application'],
          path: '/api/applications',
        },
      })
    );

    expect(snackBar.open).toHaveBeenCalledWith(
      'You already have an active application',
      'Close',
      jasmine.anything()
    );
  });

  it('forbiddenResponse_onASilentRequest_doesNothingVisible', async () => {
    await run(
      new HttpRequest('GET', '/api/notifications', { headers: undefined }).clone({
        setHeaders: { 'X-Silent-Error': 'true' },
      }),
      new HttpErrorResponse({ status: 403 })
    );

    expect(router.navigate).not.toHaveBeenCalled();
    expect(snackBar.open).not.toHaveBeenCalled();
  });

  it('unauthorizedResponse_stillRedirectsToLoginWithTheCurrentUrl', async () => {
    await run(
      new HttpRequest('GET', '/api/admin/system-settings'),
      new HttpErrorResponse({ status: 401 })
    );

    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/admin/system-settings' },
    });
  });

  it('successfulResponse_passesThrough', async () => {
    const next: HttpHandlerFn = () => of({} as HttpEvent<unknown>);
    const result = await new Promise((resolve) => {
      TestBed.runInInjectionContext(() =>
        errorInterceptor(new HttpRequest('GET', '/api/rooms'), next)
      ).subscribe({ next: () => resolve('ok'), error: () => resolve('error') });
    });

    expect(result).toBe('ok');
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
