import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
} from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';

import { EmailVerificationRequiredComponent } from './email-verification-required.component';
import { EmailVerifiedGuard } from '../../../core/guards/email-verified.guard';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { TokenStorageService } from '../../../core/services/token-storage.service';
import { environment } from '../../../../environments/environment';

/**
 * #442: the button only re-read localStorage. A student who clicked the link on their phone stayed
 * stuck on the laptop, whose stored copy still said unverified, until they signed out and back in.
 *
 * <p>Uses the real AuthService and guard, because what has to hold is the whole path from the
 * backend's answer to the guard letting the student through.
 */
describe('EmailVerificationRequiredComponent', () => {
  const meUrl = `${environment.apiUrl}/auth/me`;
  const student = {
    id: '1',
    firstName: 'Ann',
    lastName: 'Lee',
    email: 'ann@example.com',
    userRole: 'STUDENT',
    emailVerified: false,
  };

  let httpMock: HttpTestingController;
  let navigate: jasmine.Spy;
  let open: jasmine.Spy;
  let component: EmailVerificationRequiredComponent;

  const storedUser = () => JSON.parse(localStorage.getItem('currentUser') as string);

  beforeEach(() => {
    localStorage.setItem('currentUser', JSON.stringify(student));

    const tokenStorage = jasmine.createSpyObj<TokenStorageService>('TokenStorageService', [
      'isTokenValid',
      'getToken',
      'removeToken',
    ]);
    tokenStorage.isTokenValid.and.returnValue(true);
    const notifications = jasmine.createSpyObj<NotificationService>('NotificationService', [
      'initializeNotifications',
      'stopNotifications',
    ]);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideNoopAnimations(),
        { provide: TokenStorageService, useValue: tokenStorage },
        { provide: NotificationService, useValue: notifications },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
    navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    open = spyOn(MatSnackBar.prototype, 'open').and.callThrough();
    component = TestBed.createComponent(EmailVerificationRequiredComponent).componentInstance;
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('resendVerificationEmail_sent_saysSo', () => {
    component.resendVerificationEmail();
    httpMock.expectOne((r) => r.url.includes('/auth/resend-verification')).flush({ message: 'ok' });

    expect(component.isResending).toBeFalse();
    expect(open).toHaveBeenCalledWith(
      jasmine.stringContaining('Verification email sent'),
      'Close',
      jasmine.any(Object)
    );
  });

  it('resendVerificationEmail_failed_saysSoAndAllowsARetry', () => {
    component.resendVerificationEmail();
    httpMock
      .expectOne((r) => r.url.includes('/auth/resend-verification'))
      .flush(null, { status: 500, statusText: 'Server Error' });

    expect(component.isResending).toBeFalse();
    expect(open).toHaveBeenCalledWith(
      jasmine.stringContaining('Failed to send'),
      'Close',
      jasmine.any(Object)
    );
  });

  it('logout_clearsTheSessionAndGoesToLogin', () => {
    component.logout();

    expect(localStorage.getItem('currentUser')).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('checkVerificationStatus_signedOutWhileWaiting_doesNotStoreTheUserAgain', () => {
    component.checkVerificationStatus();
    const request = httpMock.expectOne(meUrl);
    TestBed.inject(AuthService).logout();
    request.flush({ ...student, emailVerified: true });

    expect(localStorage.getItem('currentUser')).toBeNull();
    expect(TestBed.inject(AuthService).currentUserValue).toBeNull();
  });

  it('checkVerificationStatus_verifiedOnAnotherDevice_updatesTheUserAndGoesToTheDashboard', () => {
    component.checkVerificationStatus();
    httpMock.expectOne(meUrl).flush({ ...student, emailVerified: true });

    expect(storedUser().emailVerified).toBeTrue();
    expect(TestBed.inject(AuthService).currentUserValue?.emailVerified).toBeTrue();
    expect(navigate).toHaveBeenCalledWith(['/dashboard']);
    expect(
      TestBed.inject(EmailVerifiedGuard).canActivate(
        {} as ActivatedRouteSnapshot,
        { url: '/dashboard' } as RouterStateSnapshot
      )
    ).toBeTrue();
  });

  it('checkVerificationStatus_stillUnverified_staysAndSaysSo', () => {
    component.checkVerificationStatus();
    httpMock.expectOne(meUrl).flush(student);

    expect(navigate).not.toHaveBeenCalled();
    expect(storedUser().emailVerified).toBeFalse();
    expect(open).toHaveBeenCalledWith(
      jasmine.stringContaining('not yet verified'),
      'Close',
      jasmine.any(Object)
    );
  });

  it('checkVerificationStatus_backendUnreachable_staysAndSaysSo', () => {
    component.checkVerificationStatus();
    httpMock.expectOne(meUrl).flush(null, { status: 0, statusText: 'Unknown Error' });

    expect(navigate).not.toHaveBeenCalled();
    expect(storedUser().emailVerified).toBeFalse();
    expect(open).toHaveBeenCalledWith(
      jasmine.stringContaining('Unable to connect'),
      'Close',
      jasmine.any(Object)
    );
  });
});
