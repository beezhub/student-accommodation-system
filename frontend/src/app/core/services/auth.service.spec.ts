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
      const signupData: any = {
        firstName: 'John',
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
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
      service.signup({} as any).subscribe();
      const req = httpMock.expectOne(`${apiUrl}/signup`);
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      req.flush({ token: 'test-token', user: mockUser });
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
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
      let result: any;
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      service.forgotPassword('john@example.com').subscribe((r) => (result = r));

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const req = httpMock.expectOne(`${apiUrl}/forgot-password`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ email: 'john@example.com' });
      req.flush({ message: 'Email sent' });

      expect(result).toEqual({ message: 'Email sent' });
    });
  });

  describe('verifyEmail', () => {
    it('verifyEmail_validToken_returnsSuccessMessage', () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      let result: any;
      service.verifyEmail('abc123').subscribe((r) => (result = r));

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
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
    it('currentUserValue_initialized_fromLocalStorage', () => {
      localStorage.setItem('currentUser', JSON.stringify(mockUser));
      // Force re-init by creating a new instance via injection reset
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
      const freshService = TestBed.inject(AuthService);
      expect(freshService.currentUserValue).toEqual(mockUser);
      // Clean up extra HTTP requests
      TestBed.inject(HttpTestingController).verify();
    });
  });
});
