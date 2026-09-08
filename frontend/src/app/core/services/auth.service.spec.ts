import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from './auth.service';
import { TokenStorageService } from './token-storage.service';
import { NotificationService } from './notification.service';
import { InstitutionService } from './institution.service';
import { YearOfStudyService } from './year-of-study.service';
import { environment } from '../../../environments/environment';
import { User } from '../models/user.model';
import { SignupData } from '../models/signup-data.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let tokenStorageSpy: jasmine.SpyObj<TokenStorageService>;
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;
  let institutionServiceSpy: jasmine.SpyObj<InstitutionService>;
  let yearOfStudyServiceSpy: jasmine.SpyObj<YearOfStudyService>;

  const mockUser: User = {
    id: '1',
    firstName: 'John',
    lastName: 'Doe',
    email: 'john@example.com',
    profileCompleted: true,
    userRole: 'Student',
    emailVerified: true,
  };

  const apiUrl = `${environment.apiUrl}/auth`;

  beforeEach(() => {
    tokenStorageSpy = jasmine.createSpyObj<TokenStorageService>(
      'TokenStorageService',
      ['setToken', 'removeToken', 'getToken', 'isTokenValid'],
      {}
    );
    tokenStorageSpy.isTokenValid.and.returnValue(false);

    notificationServiceSpy = jasmine.createSpyObj<NotificationService>('NotificationService', [
      'initializeNotifications',
      'stopNotifications',
    ]);

    institutionServiceSpy = jasmine.createSpyObj<InstitutionService>('InstitutionService', [
      'clearCache',
    ]);

    yearOfStudyServiceSpy = jasmine.createSpyObj<YearOfStudyService>('YearOfStudyService', [
      'clearCache',
    ]);

    localStorage.clear();

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      providers: [
        AuthService,
        { provide: TokenStorageService, useValue: tokenStorageSpy },
        { provide: NotificationService, useValue: notificationServiceSpy },
        { provide: InstitutionService, useValue: institutionServiceSpy },
        { provide: YearOfStudyService, useValue: yearOfStudyServiceSpy },
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('login', () => {
    it('login_validCredentials_storesTokenAndUser', () => {
      const credentials = { email: 'john@example.com', password: 'password' };
      let result: User | undefined;

      service.login(credentials).subscribe((user) => (result = user));

      const req = httpMock.expectOne(`${apiUrl}/login`);
      expect(req.request.method).toBe('POST');
      req.flush({ token: 'test-token', user: mockUser });

      expect(result).toEqual(mockUser);
      expect(tokenStorageSpy.setToken).toHaveBeenCalledWith('test-token');
      expect(localStorage.getItem('currentUser')).toBeTruthy();
      expect(service.currentUserValue).toEqual(mockUser);
    });

    it('login_success_initializesNotifications', () => {
      service.login({ email: 'john@example.com', password: 'password' }).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/login`);
      req.flush({ token: 'test-token', user: mockUser });

      expect(notificationServiceSpy.initializeNotifications).toHaveBeenCalled();
    });
  });

  describe('signup', () => {
    it('signup_validData_storesTokenAndUser', () => {
      const signupData: SignupData = {
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        password: 'Password1!',
      };
      let result: User | undefined;

      service.signup(signupData).subscribe((user) => (result = user));

      const req = httpMock.expectOne(`${apiUrl}/signup`);
      expect(req.request.method).toBe('POST');
      req.flush({ token: 'test-token', user: mockUser });

      expect(result).toEqual(mockUser);
      expect(tokenStorageSpy.setToken).toHaveBeenCalledWith('test-token');
      expect(service.currentUserValue).toEqual(mockUser);
    });

    it('signup_success_initializesNotifications', () => {
      service.signup({} as SignupData).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/signup`);
      req.flush({ token: 'test-token', user: mockUser });
      expect(notificationServiceSpy.initializeNotifications).toHaveBeenCalled();
    });
  });

  describe('logout', () => {
    it('logout_clearsUserAndToken', () => {
      localStorage.setItem('currentUser', JSON.stringify(mockUser));

      service.logout();

      expect(tokenStorageSpy.removeToken).toHaveBeenCalled();
      expect(localStorage.getItem('currentUser')).toBeNull();
      expect(service.currentUserValue).toBeNull();
    });

    it('logout_stopsNotifications', () => {
      service.logout();
      expect(notificationServiceSpy.stopNotifications).toHaveBeenCalled();
    });

    it('logout_clearsCaches', () => {
      service.logout();
      expect(institutionServiceSpy.clearCache).toHaveBeenCalled();
      expect(yearOfStudyServiceSpy.clearCache).toHaveBeenCalled();
    });

    it('logout_withToken_callsBackendSoTheTokenIsBlacklisted', () => {
      tokenStorageSpy.getToken.and.returnValue('a.b.c');

      service.logout();

      const req = httpMock.expectOne(`${apiUrl}/logout`);
      expect(req.request.method).toBe('POST');
      req.flush({});
    });

    it('logout_backendFails_localSessionStillCleared', () => {
      tokenStorageSpy.getToken.and.returnValue('a.b.c');
      localStorage.setItem('currentUser', JSON.stringify(mockUser));

      service.logout();

      httpMock
        .expectOne(`${apiUrl}/logout`)
        .flush({ message: 'boom' }, { status: 500, statusText: 'Server Error' });

      expect(tokenStorageSpy.removeToken).toHaveBeenCalled();
      expect(localStorage.getItem('currentUser')).toBeNull();
      expect(service.currentUserValue).toBeNull();
    });

    it('logout_withoutToken_makesNoRequest', () => {
      tokenStorageSpy.getToken.and.returnValue(null);

      service.logout();

      httpMock.expectNone(`${apiUrl}/logout`);
      expect(tokenStorageSpy.removeToken).toHaveBeenCalled();
    });
  });

  describe('isLoggedIn', () => {
    it('isLoggedIn_validToken_returnsTrue', () => {
      tokenStorageSpy.isTokenValid.and.returnValue(true);
      expect(service.isLoggedIn()).toBeTrue();
    });

    it('isLoggedIn_noToken_returnsFalse', () => {
      tokenStorageSpy.isTokenValid.and.returnValue(false);
      expect(service.isLoggedIn()).toBeFalse();
    });
  });

  describe('changePassword', () => {
    it('changePassword_success_clearsMustChangePasswordFlag', () => {
      const user: User = { ...mockUser, mustChangePassword: true };
      localStorage.setItem('currentUser', JSON.stringify(user));
      // Re-initialize to pick up stored user
      service['currentUserSubject'].next(user);

      service.changePassword('oldPass', 'newPass').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/change-password`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ currentPassword: 'oldPass', newPassword: 'newPass' });
      req.flush({ message: 'Password changed' });

      expect(service.currentUserValue?.mustChangePassword).toBeFalse();
    });
  });

  describe('forgotPassword', () => {
    it('forgotPassword_sendsEmailToBackend', () => {
      let result: unknown;
      service.forgotPassword('john@example.com').subscribe((r) => (result = r));
      const req = httpMock.expectOne(`${apiUrl}/forgot-password`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ email: 'john@example.com' });
      req.flush({ message: 'Email sent' });

      expect(result).toEqual({ message: 'Email sent' });
    });
  });

  describe('verifyEmail', () => {
    it('verifyEmail_validToken_returnsSuccessMessage', () => {
      let result: unknown;
      service.verifyEmail('abc123').subscribe((r) => (result = r));
      const req = httpMock.expectOne(`${apiUrl}/verify-email`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ token: 'abc123' });
      req.flush({ message: 'Email verified' });

      expect(result).toEqual({ message: 'Email verified' });
    });
  });

  describe('isEmailVerified', () => {
    it('isEmailVerified_verifiedUser_returnsTrue', () => {
      service['currentUserSubject'].next({ ...mockUser, emailVerified: true });
      expect(service.isEmailVerified()).toBeTrue();
    });

    it('isEmailVerified_unverifiedUser_returnsFalse', () => {
      service['currentUserSubject'].next({ ...mockUser, emailVerified: false });
      expect(service.isEmailVerified()).toBeFalse();
    });

    it('isEmailVerified_noUser_returnsFalse', () => {
      service['currentUserSubject'].next(null);
      expect(service.isEmailVerified()).toBeFalse();
    });
  });

  describe('updateEmailVerificationStatus', () => {
    it('updateEmailVerificationStatus_setsVerifiedFlag', () => {
      service['currentUserSubject'].next({ ...mockUser, emailVerified: false });
      service.updateEmailVerificationStatus(true);
      expect(service.currentUserValue?.emailVerified).toBeTrue();
    });
  });

  describe('currentUserValue', () => {
    /** Rebuilds the service against the current localStorage and spy state. */
    function rebuildService(): AuthService {
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [HttpClientTestingModule, RouterTestingModule],
        providers: [
          AuthService,
          { provide: TokenStorageService, useValue: tokenStorageSpy },
          { provide: NotificationService, useValue: notificationServiceSpy },
          { provide: InstitutionService, useValue: institutionServiceSpy },
          { provide: YearOfStudyService, useValue: yearOfStudyServiceSpy },
        ],
      });
      return TestBed.inject(AuthService);
    }

    it('currentUserValue_validToken_initializedFromLocalStorage', () => {
      tokenStorageSpy.isTokenValid.and.returnValue(true);
      localStorage.setItem('currentUser', JSON.stringify(mockUser));

      expect(rebuildService().currentUserValue).toEqual(mockUser);
      TestBed.inject(HttpTestingController).verify();
    });

    /**
     * GH-151: the stored user was emitted whatever the token said, so an expired session still
     * rendered the signed-in toolbar until the guard redirect resolved.
     */
    it('currentUserValue_expiredToken_restoresNothing', () => {
      tokenStorageSpy.isTokenValid.and.returnValue(false);
      localStorage.setItem('currentUser', JSON.stringify(mockUser));

      expect(rebuildService().currentUserValue).toBeNull();
      TestBed.inject(HttpTestingController).verify();
    });

    /** And the profile data does not sit in localStorage indefinitely either. */
    it('currentUserValue_expiredToken_clearsTheStoredProfile', () => {
      tokenStorageSpy.isTokenValid.and.returnValue(false);
      localStorage.setItem('currentUser', JSON.stringify(mockUser));

      rebuildService();

      expect(localStorage.getItem('currentUser')).toBeNull();
      TestBed.inject(HttpTestingController).verify();
    });
  });
});
