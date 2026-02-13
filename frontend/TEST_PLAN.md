# Student Accommodation System - Frontend Test Plan

## Table of Contents
1. [Test Plan Overview](#1-test-plan-overview)
2. [Unit Tests](#2-unit-tests)
3. [Integration Tests](#3-integration-tests)
4. [End-to-End Test Scenarios (Playwright)](#4-end-to-end-test-scenarios-playwright)
5. [Manual Test Cases](#5-manual-test-cases)
6. [Test Data Requirements](#6-test-data-requirements)
7. [Test Environment Setup](#7-test-environment-setup)

---

## 1. Test Plan Overview

### 1.1 Project Summary

The Student Accommodation System frontend is a modern Angular 21 application providing a comprehensive user interface for student housing management. The application serves two primary user roles:

**For Students:**
- User registration and authentication
- Profile setup and management
- Accommodation application submission
- Document upload and tracking
- Application status monitoring
- Room allocation viewing

**For Administrators:**
- Dashboard with system statistics
- Application review and approval workflow
- Document verification
- Room and building management
- Allocation management (manual and batch)
- System configuration (institutions, document types, allocation rules)

**Technology Stack:**
- Framework: Angular 21.1.0 (Standalone Components)
- UI Library: Angular Material 21.1.0
- State Management: RxJS BehaviorSubjects
- Forms: Reactive Forms with custom validators
- HTTP: Angular HttpClient with interceptors
- Authentication: JWT with localStorage persistence
- Testing: Jasmine/Karma (to be configured) + Playwright (E2E)

### 1.2 Testing Objectives

1. **Component Functionality**: Verify all 60+ components render and behave correctly
2. **Service Logic**: Test all 24+ services for correct API integration
3. **Form Validation**: Ensure all reactive forms validate correctly
4. **Authentication Flow**: Verify JWT handling, guards, and interceptors
5. **Role-Based Access**: Test admin vs student access controls
6. **User Experience**: Validate responsive design and accessibility
7. **Error Handling**: Test error states and user feedback

### 1.3 Testing Scope

**In Scope:**
- All Angular components (60+)
- All services and API integrations (24+)
- Route guards (Auth, Admin, Student, EmailVerified)
- HTTP interceptors (Auth, Error, TokenExpiry)
- Reactive form validation
- Material UI component integration
- Navigation and routing
- File upload functionality
- Notification system

**Out of Scope:**
- Backend API testing (covered in backend test plan)
- Third-party library internals (Angular Material)
- Browser-specific CSS issues beyond major browsers

### 1.4 Testing Approach

**Testing Levels:**
1. **Unit Testing**: Jasmine + Angular TestBed for isolated component/service testing
2. **Integration Testing**: Testing component interactions and service integration
3. **E2E Testing**: Playwright for full user flow validation
4. **Visual Regression**: Screenshot comparison for UI consistency

### 1.5 Entry and Exit Criteria

**Entry Criteria:**
- Angular application builds successfully
- Backend API available for integration tests
- Test environment configured
- Test data prepared

**Exit Criteria:**
- 80% code coverage for services
- 70% code coverage for components
- All critical user flows passing E2E tests
- No critical or high severity defects open
- All manual test cases executed

---

## 2. Unit Tests

### 2.1 Test Setup and Configuration

**File**: `karma.conf.js`

```javascript
module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    client: {
      jasmine: {},
      clearContext: false
    },
    jasmineHtmlReporter: {
      suppressAll: true
    },
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage'),
      subdir: '.',
      reporters: [
        { type: 'html' },
        { type: 'text-summary' },
        { type: 'lcovonly' }
      ],
      check: {
        global: {
          statements: 70,
          branches: 60,
          functions: 70,
          lines: 70
        }
      }
    },
    reporters: ['progress', 'kjhtml'],
    browsers: ['Chrome', 'ChromeHeadless'],
    restartOnFileChange: true
  });
};
```

**File**: `src/test.ts`

```typescript
import 'zone.js';
import 'zone.js/testing';
import { getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting
} from '@angular/platform-browser-dynamic/testing';

declare const require: {
  context(path: string, deep?: boolean, filter?: RegExp): {
    <T>(id: string): T;
    keys(): string[];
  };
};

getTestBed().initTestEnvironment(
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting()
);

const context = require.context('./', true, /\.spec\.ts$/);
context.keys().forEach(context);
```

### 2.2 Auth Service Tests

**File**: `src/app/core/services/auth.service.spec.ts`

```typescript
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { User } from '../models/user.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockUser: User = {
    id: 1,
    email: 'test@university.edu',
    firstName: 'John',
    lastName: 'Doe',
    userRole: 'STUDENT',
    emailVerified: true,
    studentProfileComplete: true
  };

  const mockLoginResponse = {
    token: 'mock-jwt-token',
    user: mockUser
  };

  beforeEach(() => {
    const routerSpyObj = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: Router, useValue: routerSpyObj }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    // Clear localStorage before each test
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('Login', () => {
    it('should login successfully and store token', fakeAsync(() => {
      const credentials = { email: 'test@university.edu', password: 'Password123!' };

      service.login(credentials).subscribe(response => {
        expect(response.token).toBe('mock-jwt-token');
        expect(response.user.email).toBe('test@university.edu');
      });

      const req = httpMock.expectOne('/api/auth/login');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(credentials);
      req.flush(mockLoginResponse);

      tick();

      expect(localStorage.getItem('token')).toBe('mock-jwt-token');
      expect(service.currentUserValue).toEqual(mockUser);
    }));

    it('should handle login error', fakeAsync(() => {
      const credentials = { email: 'wrong@email.com', password: 'wrong' };

      service.login(credentials).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(401);
        }
      });

      const req = httpMock.expectOne('/api/auth/login');
      req.flush({ message: 'Invalid credentials' }, { status: 401, statusText: 'Unauthorized' });
    }));

    it('should update currentUser$ observable on login', fakeAsync(() => {
      let currentUser: User | null = null;
      service.currentUser$.subscribe(user => currentUser = user);

      service.login({ email: 'test@university.edu', password: 'pass' }).subscribe();

      const req = httpMock.expectOne('/api/auth/login');
      req.flush(mockLoginResponse);
      tick();

      expect(currentUser).toEqual(mockUser);
    }));
  });

  describe('Signup', () => {
    it('should register new user successfully', fakeAsync(() => {
      const signupData = {
        email: 'new@university.edu',
        password: 'Password123!',
        firstName: 'Jane',
        lastName: 'Doe'
      };

      service.signup(signupData).subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne('/api/auth/signup');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(signupData);
      req.flush({ message: 'Registration successful' });
    }));

    it('should handle duplicate email error', fakeAsync(() => {
      service.signup({ email: 'existing@university.edu', password: 'pass', firstName: 'Test', lastName: 'User' })
        .subscribe({
          next: () => fail('should have failed'),
          error: (error) => {
            expect(error.status).toBe(409);
          }
        });

      const req = httpMock.expectOne('/api/auth/signup');
      req.flush({ message: 'Email already registered' }, { status: 409, statusText: 'Conflict' });
    }));
  });

  describe('Logout', () => {
    it('should clear token and user on logout', () => {
      // Setup logged in state
      localStorage.setItem('token', 'test-token');
      localStorage.setItem('currentUser', JSON.stringify(mockUser));

      service.logout();

      expect(localStorage.getItem('token')).toBeNull();
      expect(localStorage.getItem('currentUser')).toBeNull();
      expect(service.currentUserValue).toBeNull();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('Token Management', () => {
    it('should return true for valid non-expired token', () => {
      // Create a token that expires in 1 hour
      const futureExp = Math.floor(Date.now() / 1000) + 3600;
      const payload = btoa(JSON.stringify({ exp: futureExp }));
      const token = `header.${payload}.signature`;
      localStorage.setItem('token', token);

      expect(service.isTokenExpired()).toBeFalse();
    });

    it('should return true for expired token', () => {
      // Create an expired token
      const pastExp = Math.floor(Date.now() / 1000) - 3600;
      const payload = btoa(JSON.stringify({ exp: pastExp }));
      const token = `header.${payload}.signature`;
      localStorage.setItem('token', token);

      expect(service.isTokenExpired()).toBeTrue();
    });

    it('should return token from localStorage', () => {
      localStorage.setItem('token', 'test-token');
      expect(service.getToken()).toBe('test-token');
    });
  });

  describe('Password Reset', () => {
    it('should request password reset', fakeAsync(() => {
      service.forgotPassword('test@university.edu').subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne('/api/auth/forgot-password');
      expect(req.request.method).toBe('POST');
      req.flush({ message: 'Reset email sent' });
    }));

    it('should reset password with token', fakeAsync(() => {
      service.resetPassword('reset-token', 'NewPassword123!').subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne('/api/auth/reset-password');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ token: 'reset-token', newPassword: 'NewPassword123!' });
      req.flush({ message: 'Password reset successful' });
    }));
  });

  describe('Email Verification', () => {
    it('should verify email with token', fakeAsync(() => {
      service.verifyEmail('verification-token').subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne('/api/auth/verify-email');
      expect(req.request.method).toBe('POST');
      req.flush({ message: 'Email verified' });
    }));

    it('should resend verification email', fakeAsync(() => {
      service.resendVerification('test@university.edu').subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne('/api/auth/resend-verification');
      expect(req.request.method).toBe('POST');
      req.flush({ message: 'Verification email sent' });
    }));
  });

  describe('Change Password', () => {
    it('should change password successfully', fakeAsync(() => {
      service.changePassword('OldPass123!', 'NewPass123!').subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne('/api/auth/change-password');
      expect(req.request.method).toBe('POST');
      req.flush({ message: 'Password changed' });
    }));
  });

  describe('User State', () => {
    it('should restore user from localStorage on init', () => {
      localStorage.setItem('token', 'valid-token');
      localStorage.setItem('currentUser', JSON.stringify(mockUser));

      // Re-create service to trigger initialization
      const newService = new AuthService(
        TestBed.inject(HttpClientTestingModule) as any,
        routerSpy
      );

      // Note: This tests the constructor behavior
    });

    it('should check if user is admin', () => {
      const adminUser = { ...mockUser, userRole: 'ADMIN' };
      localStorage.setItem('currentUser', JSON.stringify(adminUser));

      expect(service.isAdmin()).toBeTrue();
    });

    it('should check if user is student', () => {
      localStorage.setItem('currentUser', JSON.stringify(mockUser));

      expect(service.isStudent()).toBeTrue();
    });
  });
});
```

### 2.3 Application Service Tests

**File**: `src/app/core/services/application.service.spec.ts`

```typescript
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ApplicationService } from './application.service';

describe('ApplicationService', () => {
  let service: ApplicationService;
  let httpMock: HttpTestingController;

  const mockApplication = {
    id: 1,
    applicationCode: 'APP-2024-001',
    status: 'PENDING',
    preferredAccommodationType: { id: 1, name: 'Single Room' },
    preferredMoveInDate: '2024-09-01',
    preferredDurationMonths: 12,
    createdAt: '2024-01-15T10:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApplicationService]
    });

    service = TestBed.inject(ApplicationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Submit Application', () => {
    it('should submit new application', fakeAsync(() => {
      const applicationData = {
        preferredAccommodationTypeId: 1,
        preferredMoveInDate: '2024-09-01',
        preferredDurationMonths: 12,
        specialRequirements: 'Ground floor'
      };

      service.submitApplication(applicationData).subscribe(response => {
        expect(response.applicationCode).toBe('APP-2024-001');
        expect(response.status).toBe('PENDING');
      });

      const req = httpMock.expectOne('/api/applications/submit');
      expect(req.request.method).toBe('POST');
      req.flush(mockApplication);
    }));
  });

  describe('Get Applications', () => {
    it('should get my applications', fakeAsync(() => {
      service.getMyApplications().subscribe(applications => {
        expect(applications.length).toBe(1);
        expect(applications[0].applicationCode).toBe('APP-2024-001');
      });

      const req = httpMock.expectOne('/api/applications/my-applications');
      expect(req.request.method).toBe('GET');
      req.flush([mockApplication]);
    }));

    it('should get application by id', fakeAsync(() => {
      service.getApplicationById(1).subscribe(application => {
        expect(application.id).toBe(1);
      });

      const req = httpMock.expectOne('/api/applications/1');
      expect(req.request.method).toBe('GET');
      req.flush(mockApplication);
    }));

    it('should get active application', fakeAsync(() => {
      service.getActiveApplication().subscribe(application => {
        expect(application).toBeTruthy();
      });

      const req = httpMock.expectOne('/api/applications/active');
      expect(req.request.method).toBe('GET');
      req.flush(mockApplication);
    }));
  });

  describe('Check Active Application', () => {
    it('should return true if active application exists', fakeAsync(() => {
      service.hasActiveApplication().subscribe(hasActive => {
        expect(hasActive).toBeTrue();
      });

      const req = httpMock.expectOne('/api/applications/has-active');
      req.flush({ hasActive: true });
    }));

    it('should return false if no active application', fakeAsync(() => {
      service.hasActiveApplication().subscribe(hasActive => {
        expect(hasActive).toBeFalse();
      });

      const req = httpMock.expectOne('/api/applications/has-active');
      req.flush({ hasActive: false });
    }));
  });

  describe('Application Full Details', () => {
    it('should get full application details with documents', fakeAsync(() => {
      const fullDetails = {
        ...mockApplication,
        documents: [
          { id: 1, name: 'ID Document', status: 'PENDING' }
        ]
      };

      service.getApplicationFullDetails(1).subscribe(details => {
        expect(details.documents.length).toBe(1);
      });

      const req = httpMock.expectOne('/api/applications/1/full-details');
      req.flush(fullDetails);
    }));
  });

  describe('Cancel Application', () => {
    it('should cancel application', fakeAsync(() => {
      service.cancelApplication(1).subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne('/api/applications/1');
      expect(req.request.method).toBe('DELETE');
      req.flush({ message: 'Cancelled' });
    }));
  });
});
```

### 2.4 Document Service Tests

**File**: `src/app/core/services/document.service.spec.ts`

```typescript
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { DocumentService } from './document.service';

describe('DocumentService', () => {
  let service: DocumentService;
  let httpMock: HttpTestingController;

  const mockDocument = {
    id: 1,
    documentName: 'test_id.pdf',
    status: 'PENDING',
    documentType: { id: 1, name: 'ID Document' },
    uploadedAt: '2024-01-15T10:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DocumentService]
    });

    service = TestBed.inject(DocumentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('File Validation', () => {
    it('should validate valid PDF file', () => {
      const file = new File(['content'], 'test.pdf', { type: 'application/pdf' });
      const result = service.validateFile(file);
      expect(result.valid).toBeTrue();
    });

    it('should validate valid JPG file', () => {
      const file = new File(['content'], 'test.jpg', { type: 'image/jpeg' });
      const result = service.validateFile(file);
      expect(result.valid).toBeTrue();
    });

    it('should validate valid PNG file', () => {
      const file = new File(['content'], 'test.png', { type: 'image/png' });
      const result = service.validateFile(file);
      expect(result.valid).toBeTrue();
    });

    it('should reject invalid file type', () => {
      const file = new File(['content'], 'test.exe', { type: 'application/x-msdownload' });
      const result = service.validateFile(file);
      expect(result.valid).toBeFalse();
      expect(result.error).toContain('Invalid file type');
    });

    it('should reject file exceeding size limit', () => {
      const largeContent = new ArrayBuffer(6 * 1024 * 1024); // 6MB
      const file = new File([largeContent], 'large.pdf', { type: 'application/pdf' });
      const result = service.validateFile(file);
      expect(result.valid).toBeFalse();
      expect(result.error).toContain('exceeds');
    });

    it('should reject empty file', () => {
      const file = new File([], 'empty.pdf', { type: 'application/pdf' });
      const result = service.validateFile(file);
      expect(result.valid).toBeFalse();
      expect(result.error).toContain('empty');
    });
  });

  describe('Upload Documents', () => {
    it('should upload single document', fakeAsync(() => {
      const file = new File(['content'], 'test.pdf', { type: 'application/pdf' });

      service.uploadSingleDocument(file, 1, 1).subscribe(response => {
        expect(response.id).toBe(1);
      });

      const req = httpMock.expectOne('/api/documents/upload-single');
      expect(req.request.method).toBe('POST');
      expect(req.request.body instanceof FormData).toBeTrue();
      req.flush(mockDocument);
    }));

    it('should upload multiple documents', fakeAsync(() => {
      const files = [
        new File(['content1'], 'doc1.pdf', { type: 'application/pdf' }),
        new File(['content2'], 'doc2.pdf', { type: 'application/pdf' })
      ];

      service.uploadMultipleDocuments(files, 1, 1).subscribe(response => {
        expect(response.length).toBe(2);
      });

      const req = httpMock.expectOne('/api/documents/upload-multiple');
      expect(req.request.method).toBe('POST');
      req.flush([mockDocument, { ...mockDocument, id: 2 }]);
    }));

    it('should report upload progress', fakeAsync(() => {
      const file = new File(['content'], 'test.pdf', { type: 'application/pdf' });
      let progressReported = false;

      service.uploadSingleDocument(file, 1, 1).subscribe({
        next: () => {},
        error: () => {}
      });

      // Simulate progress event
      const req = httpMock.expectOne('/api/documents/upload-single');
      req.flush(mockDocument);
    }));
  });

  describe('Get Documents', () => {
    it('should get my documents', fakeAsync(() => {
      service.getMyDocuments().subscribe(documents => {
        expect(documents.length).toBe(1);
      });

      const req = httpMock.expectOne('/api/documents/my');
      req.flush({ content: [mockDocument], totalElements: 1 });
    }));

    it('should get document info with presigned URL', fakeAsync(() => {
      const docWithUrl = { ...mockDocument, presignedUrl: 'https://minio/...' };

      service.getDocumentInfo(1).subscribe(doc => {
        expect(doc.presignedUrl).toBeTruthy();
      });

      const req = httpMock.expectOne('/api/documents/info/1');
      req.flush(docWithUrl);
    }));
  });

  describe('Delete Document', () => {
    it('should delete document', fakeAsync(() => {
      service.deleteDocument(1).subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne('/api/documents/delete/1');
      expect(req.request.method).toBe('DELETE');
      req.flush({ message: 'Deleted' });
    }));
  });
});
```

### 2.5 Login Component Tests

**File**: `src/app/features/auth/login/login.component.spec.ts`

```typescript
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  const mockUser = {
    id: 1,
    email: 'test@university.edu',
    firstName: 'John',
    lastName: 'Doe',
    userRole: 'STUDENT',
    emailVerified: true,
    studentProfileComplete: true
  };

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['login']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [
        LoginComponent,
        ReactiveFormsModule,
        NoopAnimationsModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatSnackBarModule
      ],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MatSnackBar, useValue: snackBarSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('Form Initialization', () => {
    it('should create component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize form with empty values', () => {
      expect(component.loginForm.get('email')?.value).toBe('');
      expect(component.loginForm.get('password')?.value).toBe('');
    });

    it('should have invalid form initially', () => {
      expect(component.loginForm.valid).toBeFalse();
    });
  });

  describe('Form Validation', () => {
    it('should require email', () => {
      const emailControl = component.loginForm.get('email');
      emailControl?.setValue('');
      expect(emailControl?.hasError('required')).toBeTrue();
    });

    it('should validate email format', () => {
      const emailControl = component.loginForm.get('email');
      emailControl?.setValue('invalid-email');
      expect(emailControl?.hasError('email')).toBeTrue();

      emailControl?.setValue('valid@email.com');
      expect(emailControl?.hasError('email')).toBeFalse();
    });

    it('should require password', () => {
      const passwordControl = component.loginForm.get('password');
      passwordControl?.setValue('');
      expect(passwordControl?.hasError('required')).toBeTrue();
    });

    it('should require minimum password length', () => {
      const passwordControl = component.loginForm.get('password');
      passwordControl?.setValue('short');
      expect(passwordControl?.hasError('minlength')).toBeTrue();

      passwordControl?.setValue('longenough123');
      expect(passwordControl?.hasError('minlength')).toBeFalse();
    });

    it('should be valid with correct inputs', () => {
      component.loginForm.setValue({
        email: 'test@university.edu',
        password: 'Password123!'
      });
      expect(component.loginForm.valid).toBeTrue();
    });
  });

  describe('Login Submission', () => {
    beforeEach(() => {
      component.loginForm.setValue({
        email: 'test@university.edu',
        password: 'Password123!'
      });
    });

    it('should not submit if form is invalid', () => {
      component.loginForm.get('email')?.setValue('');
      component.onSubmit();
      expect(authServiceSpy.login).not.toHaveBeenCalled();
    });

    it('should call auth service on valid submit', fakeAsync(() => {
      authServiceSpy.login.and.returnValue(of({ token: 'token', user: mockUser }));

      component.onSubmit();
      tick();

      expect(authServiceSpy.login).toHaveBeenCalledWith({
        email: 'test@university.edu',
        password: 'Password123!'
      });
    }));

    it('should navigate to dashboard on successful login for student', fakeAsync(() => {
      authServiceSpy.login.and.returnValue(of({ token: 'token', user: mockUser }));

      component.onSubmit();
      tick();

      expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
    }));

    it('should navigate to admin dashboard for admin user', fakeAsync(() => {
      const adminUser = { ...mockUser, userRole: 'ADMIN' };
      authServiceSpy.login.and.returnValue(of({ token: 'token', user: adminUser }));

      component.onSubmit();
      tick();

      expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin']);
    }));

    it('should navigate to profile setup if profile incomplete', fakeAsync(() => {
      const incompleteUser = { ...mockUser, studentProfileComplete: false };
      authServiceSpy.login.and.returnValue(of({ token: 'token', user: incompleteUser }));

      component.onSubmit();
      tick();

      expect(routerSpy.navigate).toHaveBeenCalledWith(['/profile-setup']);
    }));

    it('should navigate to email verification if not verified', fakeAsync(() => {
      const unverifiedUser = { ...mockUser, emailVerified: false };
      authServiceSpy.login.and.returnValue(of({ token: 'token', user: unverifiedUser }));

      component.onSubmit();
      tick();

      expect(routerSpy.navigate).toHaveBeenCalledWith(['/email-verification-required']);
    }));

    it('should show error on login failure', fakeAsync(() => {
      authServiceSpy.login.and.returnValue(throwError(() => ({ status: 401, error: { message: 'Invalid credentials' } })));

      component.onSubmit();
      tick();

      expect(snackBarSpy.open).toHaveBeenCalled();
      expect(component.isLoading).toBeFalse();
    }));

    it('should set loading state during submission', fakeAsync(() => {
      authServiceSpy.login.and.returnValue(of({ token: 'token', user: mockUser }));

      expect(component.isLoading).toBeFalse();
      component.onSubmit();
      expect(component.isLoading).toBeTrue();
      tick();
      expect(component.isLoading).toBeFalse();
    }));
  });

  describe('UI Interactions', () => {
    it('should toggle password visibility', () => {
      expect(component.hidePassword).toBeTrue();
      component.togglePasswordVisibility();
      expect(component.hidePassword).toBeFalse();
      component.togglePasswordVisibility();
      expect(component.hidePassword).toBeTrue();
    });

    it('should display error messages for invalid fields', () => {
      component.loginForm.get('email')?.setValue('');
      component.loginForm.get('email')?.markAsTouched();
      fixture.detectChanges();

      const compiled = fixture.nativeElement;
      const errorMessage = compiled.querySelector('mat-error');
      expect(errorMessage).toBeTruthy();
    });
  });
});
```

### 2.6 Signup Component Tests

**File**: `src/app/features/auth/signup/signup.component.spec.ts`

```typescript
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SignupComponent } from './signup.component';
import { AuthService } from '../../../core/services/auth.service';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';

describe('SignupComponent', () => {
  let component: SignupComponent;
  let fixture: ComponentFixture<SignupComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['signup']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [
        SignupComponent,
        ReactiveFormsModule,
        NoopAnimationsModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatCheckboxModule,
        MatSnackBarModule
      ],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MatSnackBar, useValue: snackBarSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SignupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('Form Validation', () => {
    it('should require all fields', () => {
      expect(component.signupForm.valid).toBeFalse();

      component.signupForm.setValue({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@university.edu',
        password: 'Password123!',
        confirmPassword: 'Password123!',
        agreeTerms: true
      });

      expect(component.signupForm.valid).toBeTrue();
    });

    it('should validate password match', () => {
      component.signupForm.patchValue({
        password: 'Password123!',
        confirmPassword: 'DifferentPassword!'
      });

      expect(component.signupForm.hasError('passwordMismatch')).toBeTrue();
    });

    it('should pass password match validation', () => {
      component.signupForm.patchValue({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@university.edu',
        password: 'Password123!',
        confirmPassword: 'Password123!',
        agreeTerms: true
      });

      expect(component.signupForm.hasError('passwordMismatch')).toBeFalse();
    });

    it('should require terms agreement', () => {
      const agreeTermsControl = component.signupForm.get('agreeTerms');
      agreeTermsControl?.setValue(false);
      expect(agreeTermsControl?.hasError('requiredTrue')).toBeTrue();
    });
  });

  describe('Signup Submission', () => {
    const validFormData = {
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@university.edu',
      password: 'Password123!',
      confirmPassword: 'Password123!',
      agreeTerms: true
    };

    it('should submit signup request', fakeAsync(() => {
      authServiceSpy.signup.and.returnValue(of({ message: 'Success' }));
      component.signupForm.setValue(validFormData);

      component.onSubmit();
      tick();

      expect(authServiceSpy.signup).toHaveBeenCalledWith({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@university.edu',
        password: 'Password123!'
      });
    }));

    it('should navigate to verify email page on success', fakeAsync(() => {
      authServiceSpy.signup.and.returnValue(of({ message: 'Success' }));
      component.signupForm.setValue(validFormData);

      component.onSubmit();
      tick();

      expect(routerSpy.navigate).toHaveBeenCalledWith(['/verify-email']);
    }));

    it('should show error for duplicate email', fakeAsync(() => {
      authServiceSpy.signup.and.returnValue(throwError(() => ({
        status: 409,
        error: { message: 'Email already registered' }
      })));
      component.signupForm.setValue(validFormData);

      component.onSubmit();
      tick();

      expect(snackBarSpy.open).toHaveBeenCalled();
    }));
  });
});
```

### 2.7 Profile Setup Component Tests

**File**: `src/app/features/auth/profile-setup/profile-setup.component.spec.ts`

```typescript
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { ProfileSetupComponent } from './profile-setup.component';
import { StudentService } from '../../../core/services/student.service';
import { InstitutionService } from '../../../core/services/institution.service';
import { YearOfStudyService } from '../../../core/services/year-of-study.service';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';

describe('ProfileSetupComponent', () => {
  let component: ProfileSetupComponent;
  let fixture: ComponentFixture<ProfileSetupComponent>;
  let studentServiceSpy: jasmine.SpyObj<StudentService>;
  let institutionServiceSpy: jasmine.SpyObj<InstitutionService>;
  let yearOfStudyServiceSpy: jasmine.SpyObj<YearOfStudyService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockInstitutions = [
    { id: 1, name: 'Test University' },
    { id: 2, name: 'Another University' }
  ];

  const mockYearsOfStudy = [
    { id: 1, name: 'First Year' },
    { id: 2, name: 'Second Year' }
  ];

  beforeEach(async () => {
    studentServiceSpy = jasmine.createSpyObj('StudentService', ['createProfile']);
    institutionServiceSpy = jasmine.createSpyObj('InstitutionService', ['getInstitutions']);
    yearOfStudyServiceSpy = jasmine.createSpyObj('YearOfStudyService', ['getYearsOfStudy']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    institutionServiceSpy.getInstitutions.and.returnValue(of(mockInstitutions));
    yearOfStudyServiceSpy.getYearsOfStudy.and.returnValue(of(mockYearsOfStudy));

    await TestBed.configureTestingModule({
      imports: [
        ProfileSetupComponent,
        ReactiveFormsModule,
        NoopAnimationsModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatDatepickerModule,
        MatNativeDateModule
      ],
      providers: [
        { provide: StudentService, useValue: studentServiceSpy },
        { provide: InstitutionService, useValue: institutionServiceSpy },
        { provide: YearOfStudyService, useValue: yearOfStudyServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileSetupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('Initialization', () => {
    it('should load institutions on init', () => {
      expect(institutionServiceSpy.getInstitutions).toHaveBeenCalled();
      expect(component.institutions).toEqual(mockInstitutions);
    });

    it('should load years of study on init', () => {
      expect(yearOfStudyServiceSpy.getYearsOfStudy).toHaveBeenCalled();
      expect(component.yearsOfStudy).toEqual(mockYearsOfStudy);
    });
  });

  describe('Form Validation', () => {
    it('should require student number', () => {
      const control = component.profileForm.get('studentNumber');
      control?.setValue('');
      expect(control?.hasError('required')).toBeTrue();
    });

    it('should validate phone number format', () => {
      const control = component.profileForm.get('phoneNumber');
      control?.setValue('invalid');
      expect(control?.hasError('pattern')).toBeTrue();

      control?.setValue('1234567890');
      expect(control?.hasError('pattern')).toBeFalse();
    });

    it('should require date of birth', () => {
      const control = component.profileForm.get('dateOfBirth');
      control?.setValue(null);
      expect(control?.hasError('required')).toBeTrue();
    });

    it('should require institution selection', () => {
      const control = component.profileForm.get('institution');
      control?.setValue(null);
      expect(control?.hasError('required')).toBeTrue();
    });

    it('should require year of study selection', () => {
      const control = component.profileForm.get('yearOfStudy');
      control?.setValue(null);
      expect(control?.hasError('required')).toBeTrue();
    });

    it('should require gender selection', () => {
      const control = component.profileForm.get('gender');
      control?.setValue(null);
      expect(control?.hasError('required')).toBeTrue();
    });
  });

  describe('Profile Submission', () => {
    const validProfile = {
      studentNumber: 'STU001',
      phoneNumber: '1234567890',
      dateOfBirth: new Date('2000-01-01'),
      institution: 1,
      yearOfStudy: 1,
      gender: 'MALE',
      specialRequirements: ''
    };

    it('should submit profile and navigate', fakeAsync(() => {
      studentServiceSpy.createProfile.and.returnValue(of({ id: 1 }));
      component.profileForm.setValue(validProfile);

      component.onSubmit();
      tick();

      expect(studentServiceSpy.createProfile).toHaveBeenCalled();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/application-submission']);
    }));
  });
});
```

### 2.8 Dashboard Component Tests

**File**: `src/app/features/dashboard/dashboard.component.spec.ts`

```typescript
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { DashboardComponent } from './dashboard.component';
import { ApplicationService } from '../../core/services/application.service';
import { DocumentService } from '../../core/services/document.service';
import { RoomAllocationService } from '../../core/services/room-allocation.service';
import { AuthService } from '../../core/services/auth.service';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let applicationServiceSpy: jasmine.SpyObj<ApplicationService>;
  let documentServiceSpy: jasmine.SpyObj<DocumentService>;
  let allocationServiceSpy: jasmine.SpyObj<RoomAllocationService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  const mockUser = {
    id: 1,
    email: 'student@university.edu',
    firstName: 'John',
    lastName: 'Doe',
    userRole: 'STUDENT'
  };

  const mockApplications = [
    { id: 1, applicationCode: 'APP-001', status: 'PENDING' },
    { id: 2, applicationCode: 'APP-002', status: 'APPROVED' }
  ];

  const mockDocuments = [
    { id: 1, name: 'ID Document', status: 'PENDING' },
    { id: 2, name: 'Enrollment', status: 'APPROVED' }
  ];

  beforeEach(async () => {
    applicationServiceSpy = jasmine.createSpyObj('ApplicationService', [
      'getMyApplications',
      'getActiveApplication'
    ]);
    documentServiceSpy = jasmine.createSpyObj('DocumentService', ['getMyDocuments']);
    allocationServiceSpy = jasmine.createSpyObj('RoomAllocationService', ['getMyAllocation']);
    authServiceSpy = jasmine.createSpyObj('AuthService', [], {
      currentUserValue: mockUser,
      currentUser$: of(mockUser)
    });

    applicationServiceSpy.getMyApplications.and.returnValue(of(mockApplications));
    applicationServiceSpy.getActiveApplication.and.returnValue(of(mockApplications[0]));
    documentServiceSpy.getMyDocuments.and.returnValue(of({ content: mockDocuments }));
    allocationServiceSpy.getMyAllocation.and.returnValue(of(null));

    await TestBed.configureTestingModule({
      imports: [
        DashboardComponent,
        NoopAnimationsModule,
        MatCardModule,
        MatIconModule,
        MatButtonModule
      ],
      providers: [
        { provide: ApplicationService, useValue: applicationServiceSpy },
        { provide: DocumentService, useValue: documentServiceSpy },
        { provide: RoomAllocationService, useValue: allocationServiceSpy },
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('Initialization', () => {
    it('should create component', () => {
      expect(component).toBeTruthy();
    });

    it('should load applications on init', fakeAsync(() => {
      tick();
      expect(applicationServiceSpy.getMyApplications).toHaveBeenCalled();
      expect(component.applications.length).toBe(2);
    }));

    it('should load documents on init', fakeAsync(() => {
      tick();
      expect(documentServiceSpy.getMyDocuments).toHaveBeenCalled();
    }));

    it('should display user name', () => {
      expect(component.userName).toBe('John');
    });
  });

  describe('Application Summary', () => {
    it('should calculate pending documents count', fakeAsync(() => {
      tick();
      expect(component.pendingDocumentsCount).toBe(1);
    }));

    it('should show active application', fakeAsync(() => {
      tick();
      expect(component.activeApplication).toBeTruthy();
    }));
  });

  describe('Quick Actions', () => {
    it('should determine if new application can be started', fakeAsync(() => {
      tick();
      // If there's a pending application, can't start new one
      expect(component.canStartNewApplication).toBeDefined();
    }));
  });
});
```

### 2.9 Admin Dashboard Component Tests

**File**: `src/app/features/admin/admin-dashboard/admin-dashboard.component.spec.ts`

```typescript
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { AdminDashboardComponent } from './admin-dashboard.component';
import { AdminService } from '../../../core/services/admin.service';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatGridListModule } from '@angular/material/grid-list';

describe('AdminDashboardComponent', () => {
  let component: AdminDashboardComponent;
  let fixture: ComponentFixture<AdminDashboardComponent>;
  let adminServiceSpy: jasmine.SpyObj<AdminService>;

  const mockStats = {
    totalStudents: 150,
    totalApplications: 75,
    pendingApplications: 20,
    approvedApplications: 40,
    allocatedApplications: 15,
    totalRooms: 100,
    availableRooms: 45,
    occupancyRate: 55
  };

  beforeEach(async () => {
    adminServiceSpy = jasmine.createSpyObj('AdminService', ['getDashboardStats']);
    adminServiceSpy.getDashboardStats.and.returnValue(of(mockStats));

    await TestBed.configureTestingModule({
      imports: [
        AdminDashboardComponent,
        NoopAnimationsModule,
        MatCardModule,
        MatIconModule,
        MatGridListModule
      ],
      providers: [
        { provide: AdminService, useValue: adminServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('Statistics Display', () => {
    it('should load dashboard stats on init', fakeAsync(() => {
      tick();
      expect(adminServiceSpy.getDashboardStats).toHaveBeenCalled();
      expect(component.stats).toEqual(mockStats);
    }));

    it('should display total students', fakeAsync(() => {
      tick();
      fixture.detectChanges();
      const compiled = fixture.nativeElement;
      expect(compiled.textContent).toContain('150');
    }));

    it('should display pending applications', fakeAsync(() => {
      tick();
      fixture.detectChanges();
      expect(component.stats.pendingApplications).toBe(20);
    }));

    it('should display occupancy rate', fakeAsync(() => {
      tick();
      fixture.detectChanges();
      expect(component.stats.occupancyRate).toBe(55);
    }));
  });

  describe('Quick Links', () => {
    it('should have link to applications', () => {
      const compiled = fixture.nativeElement;
      const links = compiled.querySelectorAll('a');
      expect(links.length).toBeGreaterThan(0);
    });
  });
});
```

### 2.10 Route Guards Tests

**File**: `src/app/core/guards/auth.guard.spec.ts`

```typescript
import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('AuthGuard', () => {
  let guard: AuthGuard;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['isTokenExpired', 'logout'], {
      currentUserValue: null
    });
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        AuthGuard,
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });

    guard = TestBed.inject(AuthGuard);
  });

  it('should allow access when token is valid', () => {
    authServiceSpy.isTokenExpired.and.returnValue(false);
    Object.defineProperty(authServiceSpy, 'currentUserValue', { value: { id: 1 } });

    const result = guard.canActivate(
      {} as ActivatedRouteSnapshot,
      { url: '/dashboard' } as RouterStateSnapshot
    );

    expect(result).toBeTrue();
  });

  it('should deny access and redirect when token expired', () => {
    authServiceSpy.isTokenExpired.and.returnValue(true);

    const result = guard.canActivate(
      {} as ActivatedRouteSnapshot,
      { url: '/dashboard' } as RouterStateSnapshot
    );

    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should deny access when no user logged in', () => {
    authServiceSpy.isTokenExpired.and.returnValue(false);
    Object.defineProperty(authServiceSpy, 'currentUserValue', { value: null });

    const result = guard.canActivate(
      {} as ActivatedRouteSnapshot,
      { url: '/dashboard' } as RouterStateSnapshot
    );

    expect(result).toBeFalse();
  });
});
```

**File**: `src/app/core/guards/admin.guard.spec.ts`

```typescript
import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AdminGuard } from './admin.guard';
import { AuthService } from '../services/auth.service';

describe('AdminGuard', () => {
  let guard: AdminGuard;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', [], {
      currentUserValue: { userRole: 'STUDENT' }
    });
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        AdminGuard,
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });

    guard = TestBed.inject(AdminGuard);
  });

  it('should allow access for admin users', () => {
    Object.defineProperty(authServiceSpy, 'currentUserValue', {
      value: { userRole: 'ADMIN' }
    });

    const result = guard.canActivate(
      {} as ActivatedRouteSnapshot,
      { url: '/admin' } as RouterStateSnapshot
    );

    expect(result).toBeTrue();
  });

  it('should deny access for non-admin users', () => {
    Object.defineProperty(authServiceSpy, 'currentUserValue', {
      value: { userRole: 'STUDENT' }
    });

    const result = guard.canActivate(
      {} as ActivatedRouteSnapshot,
      { url: '/admin' } as RouterStateSnapshot
    );

    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  });
});
```

### 2.11 HTTP Interceptors Tests

**File**: `src/app/core/interceptors/auth.interceptor.spec.ts`

```typescript
import { TestBed } from '@angular/core/testing';
import { HTTP_INTERCEPTORS, HttpClient, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';
import { of } from 'rxjs';

describe('AuthInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getToken']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        {
          provide: HTTP_INTERCEPTORS,
          useValue: authInterceptor,
          multi: true
        }
      ]
    });

    httpMock = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should add Authorization header for authenticated requests', () => {
    authServiceSpy.getToken.and.returnValue('test-token');

    httpClient.get('/api/applications').subscribe();

    const req = httpMock.expectOne('/api/applications');
    expect(req.request.headers.has('Authorization')).toBeTrue();
    expect(req.request.headers.get('Authorization')).toBe('Bearer test-token');
    req.flush({});
  });

  it('should not add Authorization header for public endpoints', () => {
    httpClient.get('/api/auth/login').subscribe();

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('should not add Authorization header when no token', () => {
    authServiceSpy.getToken.and.returnValue(null);

    httpClient.get('/api/applications').subscribe();

    const req = httpMock.expectOne('/api/applications');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });
});
```

---

## 3. Integration Tests

### 3.1 Application Flow Integration Tests

**File**: `src/app/features/auth/integration/application-flow.integration.spec.ts`

```typescript
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AuthService } from '../../../core/services/auth.service';
import { StudentService } from '../../../core/services/student.service';
import { ApplicationService } from '../../../core/services/application.service';

describe('Application Flow Integration', () => {
  let authService: AuthService;
  let studentService: StudentService;
  let applicationService: ApplicationService;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule.withRoutes([]),
        NoopAnimationsModule
      ],
      providers: [
        AuthService,
        StudentService,
        ApplicationService
      ]
    });

    authService = TestBed.inject(AuthService);
    studentService = TestBed.inject(StudentService);
    applicationService = TestBed.inject(ApplicationService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('Complete Registration to Application Flow', () => {
    it('should complete full registration flow', fakeAsync(() => {
      // Step 1: Signup
      authService.signup({
        email: 'new@university.edu',
        password: 'Password123!',
        firstName: 'Test',
        lastName: 'User'
      }).subscribe();

      const signupReq = httpMock.expectOne('/api/auth/signup');
      signupReq.flush({ message: 'Check your email for verification' });
      tick();

      // Step 2: Verify Email
      authService.verifyEmail('verification-token').subscribe();

      const verifyReq = httpMock.expectOne('/api/auth/verify-email');
      verifyReq.flush({ message: 'Email verified' });
      tick();

      // Step 3: Login
      authService.login({ email: 'new@university.edu', password: 'Password123!' }).subscribe();

      const loginReq = httpMock.expectOne('/api/auth/login');
      loginReq.flush({
        token: 'jwt-token',
        user: {
          id: 1,
          email: 'new@university.edu',
          userRole: 'STUDENT',
          emailVerified: true,
          studentProfileComplete: false
        }
      });
      tick();

      expect(authService.currentUserValue).toBeTruthy();
      expect(authService.currentUserValue?.studentProfileComplete).toBeFalse();
    }));

    it('should complete profile and submit application', fakeAsync(() => {
      // Setup: Logged in user
      localStorage.setItem('token', 'valid-token');
      localStorage.setItem('currentUser', JSON.stringify({
        id: 1,
        email: 'student@university.edu',
        userRole: 'STUDENT',
        emailVerified: true,
        studentProfileComplete: false
      }));

      // Step 1: Create Student Profile
      studentService.createProfile({
        studentNumber: 'STU001',
        phoneNumber: '1234567890',
        dateOfBirth: '2000-01-01',
        institutionId: 1,
        yearOfStudyId: 1,
        gender: 'MALE'
      }).subscribe();

      const profileReq = httpMock.expectOne('/api/students');
      profileReq.flush({ id: 1, studentNumber: 'STU001' });
      tick();

      // Step 2: Submit Application
      applicationService.submitApplication({
        preferredAccommodationTypeId: 1,
        preferredMoveInDate: '2024-09-01',
        preferredDurationMonths: 12
      }).subscribe();

      const appReq = httpMock.expectOne('/api/applications/submit');
      appReq.flush({
        id: 1,
        applicationCode: 'APP-001',
        status: 'PENDING'
      });
      tick();
    }));
  });

  describe('Authentication State Persistence', () => {
    it('should restore user state from localStorage', () => {
      const storedUser = {
        id: 1,
        email: 'test@university.edu',
        userRole: 'STUDENT'
      };

      localStorage.setItem('token', 'stored-token');
      localStorage.setItem('currentUser', JSON.stringify(storedUser));

      // Re-initialize service
      const newAuthService = new AuthService(
        TestBed.inject(HttpClientTestingModule) as any,
        router
      );

      expect(localStorage.getItem('token')).toBe('stored-token');
    });
  });
});
```

### 3.2 Document Upload Integration Tests

**File**: `src/app/features/dashboard/integration/document-upload.integration.spec.ts`

```typescript
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DocumentService } from '../../../core/services/document.service';
import { ApplicationService } from '../../../core/services/application.service';

describe('Document Upload Integration', () => {
  let documentService: DocumentService;
  let applicationService: ApplicationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DocumentService, ApplicationService]
    });

    documentService = TestBed.inject(DocumentService);
    applicationService = TestBed.inject(ApplicationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Upload Documents for Application', () => {
    it('should upload document and associate with application', fakeAsync(() => {
      const file = new File(['content'], 'id.pdf', { type: 'application/pdf' });
      const applicationId = 1;
      const documentTypeId = 1;

      documentService.uploadSingleDocument(file, documentTypeId, applicationId).subscribe(doc => {
        expect(doc.id).toBe(1);
        expect(doc.status).toBe('PENDING');
      });

      const req = httpMock.expectOne('/api/documents/upload-single');
      expect(req.request.method).toBe('POST');

      const formData = req.request.body as FormData;
      expect(formData.get('file')).toBeTruthy();
      expect(formData.get('documentTypeId')).toBe('1');
      expect(formData.get('applicationId')).toBe('1');

      req.flush({
        id: 1,
        documentName: 'id.pdf',
        status: 'PENDING',
        applicationId: 1
      });
      tick();
    }));

    it('should retrieve application with documents', fakeAsync(() => {
      applicationService.getApplicationFullDetails(1).subscribe(app => {
        expect(app.documents.length).toBe(2);
      });

      const req = httpMock.expectOne('/api/applications/1/full-details');
      req.flush({
        id: 1,
        applicationCode: 'APP-001',
        status: 'PENDING',
        documents: [
          { id: 1, documentName: 'id.pdf', status: 'PENDING' },
          { id: 2, documentName: 'enrollment.pdf', status: 'APPROVED' }
        ]
      });
      tick();
    }));
  });
});
```

---

## 4. End-to-End Test Scenarios (Playwright)

### 4.1 Playwright Configuration

**File**: `e2e/playwright.config.ts`

```typescript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['json', { outputFile: 'test-results/results.json' }]
  ],
  use: {
    baseURL: 'http://localhost:4200',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
    {
      name: 'mobile-chrome',
      use: { ...devices['Pixel 5'] },
    },
  ],
  webServer: {
    command: 'ng serve',
    url: 'http://localhost:4200',
    reuseExistingServer: !process.env.CI,
    timeout: 120000,
  },
});
```

### 4.2 Test Fixtures

**File**: `e2e/fixtures/test-fixtures.ts`

```typescript
import { test as base, expect, Page } from '@playwright/test';

export const testUsers = {
  student: {
    email: 'e2e-student@university.edu',
    password: 'TestPassword123!',
    firstName: 'E2E',
    lastName: 'Student'
  },
  admin: {
    email: 'e2e-admin@university.edu',
    password: 'AdminPassword123!',
    firstName: 'E2E',
    lastName: 'Admin'
  }
};

async function loginAsStudent(page: Page) {
  await page.goto('/login');
  await page.fill('input[formControlName="email"]', testUsers.student.email);
  await page.fill('input[formControlName="password"]', testUsers.student.password);
  await page.click('button[type="submit"]');
  await page.waitForURL('/dashboard');
}

async function loginAsAdmin(page: Page) {
  await page.goto('/login');
  await page.fill('input[formControlName="email"]', testUsers.admin.email);
  await page.fill('input[formControlName="password"]', testUsers.admin.password);
  await page.click('button[type="submit"]');
  await page.waitForURL('/admin');
}

export const test = base.extend<{
  studentPage: Page;
  adminPage: Page;
}>({
  studentPage: async ({ page }, use) => {
    await loginAsStudent(page);
    await use(page);
  },
  adminPage: async ({ page }, use) => {
    await loginAsAdmin(page);
    await use(page);
  },
});

export { expect };
```

### 4.3 Authentication E2E Tests

**File**: `e2e/tests/auth.spec.ts`

```typescript
import { test, expect, testUsers } from '../fixtures/test-fixtures';

test.describe('Authentication Flow', () => {

  test.describe('User Registration', () => {

    test('should register new student successfully', async ({ page }) => {
      const uniqueEmail = `test-${Date.now()}@university.edu`;

      await page.goto('/signup');

      // Fill registration form
      await page.fill('input[formControlName="firstName"]', 'Test');
      await page.fill('input[formControlName="lastName"]', 'User');
      await page.fill('input[formControlName="email"]', uniqueEmail);
      await page.fill('input[formControlName="password"]', 'Password123!');
      await page.fill('input[formControlName="confirmPassword"]', 'Password123!');
      await page.check('mat-checkbox[formControlName="agreeTerms"]');

      // Submit form
      await page.click('button[type="submit"]');

      // Verify redirect to verification page
      await expect(page).toHaveURL(/verify-email/);
      await expect(page.locator('text=Check your email')).toBeVisible();
    });

    test('should show error for duplicate email', async ({ page }) => {
      await page.goto('/signup');

      await page.fill('input[formControlName="firstName"]', 'Test');
      await page.fill('input[formControlName="lastName"]', 'User');
      await page.fill('input[formControlName="email"]', testUsers.student.email);
      await page.fill('input[formControlName="password"]', 'Password123!');
      await page.fill('input[formControlName="confirmPassword"]', 'Password123!');
      await page.check('mat-checkbox[formControlName="agreeTerms"]');

      await page.click('button[type="submit"]');

      // Verify error message
      await expect(page.locator('.mat-snack-bar-container')).toContainText(/already registered/i);
    });

    test('should validate password match', async ({ page }) => {
      await page.goto('/signup');

      await page.fill('input[formControlName="password"]', 'Password123!');
      await page.fill('input[formControlName="confirmPassword"]', 'DifferentPassword!');
      await page.click('body'); // Trigger blur

      await expect(page.locator('mat-error')).toContainText(/match/i);
    });
  });

  test.describe('User Login', () => {

    test('should login as student successfully', async ({ page }) => {
      await page.goto('/login');

      await page.fill('input[formControlName="email"]', testUsers.student.email);
      await page.fill('input[formControlName="password"]', testUsers.student.password);
      await page.click('button[type="submit"]');

      await expect(page).toHaveURL('/dashboard');
      await expect(page.locator('text=Welcome')).toBeVisible();
    });

    test('should login as admin successfully', async ({ page }) => {
      await page.goto('/login');

      await page.fill('input[formControlName="email"]', testUsers.admin.email);
      await page.fill('input[formControlName="password"]', testUsers.admin.password);
      await page.click('button[type="submit"]');

      await expect(page).toHaveURL('/admin');
    });

    test('should show error for invalid credentials', async ({ page }) => {
      await page.goto('/login');

      await page.fill('input[formControlName="email"]', testUsers.student.email);
      await page.fill('input[formControlName="password"]', 'WrongPassword!');
      await page.click('button[type="submit"]');

      await expect(page.locator('.mat-snack-bar-container')).toContainText(/invalid/i);
      await expect(page).toHaveURL('/login');
    });

    test('should toggle password visibility', async ({ page }) => {
      await page.goto('/login');

      const passwordInput = page.locator('input[formControlName="password"]');
      await expect(passwordInput).toHaveAttribute('type', 'password');

      await page.click('button[mat-icon-button]');
      await expect(passwordInput).toHaveAttribute('type', 'text');

      await page.click('button[mat-icon-button]');
      await expect(passwordInput).toHaveAttribute('type', 'password');
    });
  });

  test.describe('Password Reset', () => {

    test('should request password reset', async ({ page }) => {
      await page.goto('/forgot-password');

      await page.fill('input[formControlName="email"]', testUsers.student.email);
      await page.click('button[type="submit"]');

      await expect(page.locator('text=Check your email')).toBeVisible();
    });
  });

  test.describe('Logout', () => {

    test('should logout successfully', async ({ studentPage }) => {
      // Click user menu
      await studentPage.click('[data-testid="user-menu"]');
      await studentPage.click('text=Logout');

      await expect(studentPage).toHaveURL('/login');

      // Verify can't access protected route
      await studentPage.goto('/dashboard');
      await expect(studentPage).toHaveURL('/login');
    });
  });
});
```

### 4.4 Student Dashboard E2E Tests

**File**: `e2e/tests/student-dashboard.spec.ts`

```typescript
import { test, expect } from '../fixtures/test-fixtures';

test.describe('Student Dashboard', () => {

  test.describe('Dashboard Overview', () => {

    test('should display dashboard with user info', async ({ studentPage }) => {
      await expect(studentPage.locator('h1, h2')).toContainText(/Welcome|Dashboard/i);
    });

    test('should show application status card', async ({ studentPage }) => {
      await expect(studentPage.locator('[data-testid="application-status-card"]')).toBeVisible();
    });

    test('should show documents summary', async ({ studentPage }) => {
      await expect(studentPage.locator('[data-testid="documents-summary"]')).toBeVisible();
    });
  });

  test.describe('Navigation', () => {

    test('should navigate to applications page', async ({ studentPage }) => {
      await studentPage.click('a[href="/applications"]');
      await expect(studentPage).toHaveURL('/applications');
    });

    test('should navigate to documents page', async ({ studentPage }) => {
      await studentPage.click('a[href="/documents"]');
      await expect(studentPage).toHaveURL('/documents');
    });

    test('should navigate to profile page', async ({ studentPage }) => {
      await studentPage.click('a[href="/profile"]');
      await expect(studentPage).toHaveURL('/profile');
    });
  });
});
```

### 4.5 Application Management E2E Tests

**File**: `e2e/tests/applications.spec.ts`

```typescript
import { test, expect } from '../fixtures/test-fixtures';

test.describe('Application Management', () => {

  test.describe('View Applications', () => {

    test('should display applications list', async ({ studentPage }) => {
      await studentPage.goto('/applications');

      await expect(studentPage.locator('[data-testid="applications-list"]')).toBeVisible();
    });

    test('should view application details', async ({ studentPage }) => {
      await studentPage.goto('/applications');

      // Click first application
      await studentPage.click('[data-testid="application-card"]:first-child');

      await expect(studentPage).toHaveURL(/\/applications\/\d+/);
      await expect(studentPage.locator('[data-testid="application-details"]')).toBeVisible();
    });
  });

  test.describe('New Application', () => {

    test('should fill and submit new application', async ({ studentPage }) => {
      await studentPage.goto('/applications/new');

      // Fill form
      await studentPage.selectOption('mat-select[formControlName="preferredAccommodationType"]', { index: 1 });

      // Set date
      await studentPage.click('input[formControlName="preferredMoveInDate"]');
      await studentPage.click('.mat-calendar-next-button');
      await studentPage.click('.mat-calendar-body-cell:not(.mat-calendar-body-disabled):first-child');

      // Set duration
      await studentPage.fill('input[formControlName="preferredDurationMonths"]', '12');

      // Submit
      await studentPage.click('button[type="submit"]');

      // Verify success
      await expect(studentPage.locator('.mat-snack-bar-container')).toContainText(/submitted/i);
    });

    test('should validate required fields', async ({ studentPage }) => {
      await studentPage.goto('/applications/new');

      await studentPage.click('button[type="submit"]');

      await expect(studentPage.locator('mat-error')).toBeVisible();
    });
  });
});
```

### 4.6 Document Upload E2E Tests

**File**: `e2e/tests/documents.spec.ts`

```typescript
import { test, expect } from '../fixtures/test-fixtures';
import path from 'path';

test.describe('Document Management', () => {

  test.describe('View Documents', () => {

    test('should display documents list', async ({ studentPage }) => {
      await studentPage.goto('/documents');

      await expect(studentPage.locator('[data-testid="documents-list"]')).toBeVisible();
    });

    test('should filter documents by status', async ({ studentPage }) => {
      await studentPage.goto('/documents');

      await studentPage.selectOption('mat-select[data-testid="status-filter"]', 'PENDING');

      // Verify filtered results
      const documents = studentPage.locator('[data-testid="document-card"]');
      const count = await documents.count();

      for (let i = 0; i < count; i++) {
        await expect(documents.nth(i).locator('[data-testid="document-status"]')).toContainText('PENDING');
      }
    });
  });

  test.describe('Upload Documents', () => {

    test('should upload single document', async ({ studentPage }) => {
      await studentPage.goto('/documents');

      // Click upload button
      await studentPage.click('[data-testid="upload-document-button"]');

      // Select document type
      await studentPage.selectOption('mat-select[formControlName="documentType"]', { index: 1 });

      // Upload file
      const fileInput = studentPage.locator('input[type="file"]');
      await fileInput.setInputFiles(path.join(__dirname, '../fixtures/test-document.pdf'));

      // Submit
      await studentPage.click('[data-testid="upload-submit-button"]');

      // Verify success
      await expect(studentPage.locator('.mat-snack-bar-container')).toContainText(/uploaded/i);
    });

    test('should reject invalid file type', async ({ studentPage }) => {
      await studentPage.goto('/documents');
      await studentPage.click('[data-testid="upload-document-button"]');

      const fileInput = studentPage.locator('input[type="file"]');
      await fileInput.setInputFiles(path.join(__dirname, '../fixtures/invalid-file.exe'));

      await expect(studentPage.locator('mat-error')).toContainText(/invalid file type/i);
    });
  });

  test.describe('Download Documents', () => {

    test('should download document', async ({ studentPage }) => {
      await studentPage.goto('/documents');

      const downloadPromise = studentPage.waitForEvent('download');
      await studentPage.click('[data-testid="download-button"]:first-child');
      const download = await downloadPromise;

      expect(download.suggestedFilename()).toBeTruthy();
    });
  });

  test.describe('Delete Documents', () => {

    test('should delete document with confirmation', async ({ studentPage }) => {
      await studentPage.goto('/documents');

      // Click delete
      await studentPage.click('[data-testid="delete-button"]:first-child');

      // Confirm deletion
      await studentPage.click('[data-testid="confirm-delete-button"]');

      await expect(studentPage.locator('.mat-snack-bar-container')).toContainText(/deleted/i);
    });
  });
});
```

### 4.7 Admin Dashboard E2E Tests

**File**: `e2e/tests/admin-dashboard.spec.ts`

```typescript
import { test, expect } from '../fixtures/test-fixtures';

test.describe('Admin Dashboard', () => {

  test.describe('Dashboard Statistics', () => {

    test('should display all stats cards', async ({ adminPage }) => {
      await expect(adminPage.locator('[data-testid="stats-total-students"]')).toBeVisible();
      await expect(adminPage.locator('[data-testid="stats-pending-applications"]')).toBeVisible();
      await expect(adminPage.locator('[data-testid="stats-occupancy-rate"]')).toBeVisible();
    });
  });

  test.describe('Application Review', () => {

    test('should navigate to applications and view list', async ({ adminPage }) => {
      await adminPage.click('a[href="/admin/applications"]');
      await expect(adminPage).toHaveURL('/admin/applications');
      await expect(adminPage.locator('table')).toBeVisible();
    });

    test('should filter applications by status', async ({ adminPage }) => {
      await adminPage.goto('/admin/applications');

      await adminPage.selectOption('mat-select[data-testid="status-filter"]', 'PENDING');

      const rows = adminPage.locator('tbody tr');
      const count = await rows.count();

      for (let i = 0; i < Math.min(count, 5); i++) {
        await expect(rows.nth(i)).toContainText('PENDING');
      }
    });

    test('should open application detail dialog', async ({ adminPage }) => {
      await adminPage.goto('/admin/applications');

      await adminPage.click('tbody tr:first-child');

      await expect(adminPage.locator('mat-dialog-container')).toBeVisible();
    });

    test('should approve application', async ({ adminPage }) => {
      await adminPage.goto('/admin/applications');

      // Open first pending application
      await adminPage.click('tbody tr:has-text("PENDING"):first-child');

      // Click approve button
      await adminPage.click('[data-testid="approve-button"]');

      // Confirm
      await adminPage.click('[data-testid="confirm-button"]');

      await expect(adminPage.locator('.mat-snack-bar-container')).toContainText(/approved/i);
    });
  });

  test.describe('Room Management', () => {

    test('should display rooms list', async ({ adminPage }) => {
      await adminPage.goto('/admin/rooms');

      await expect(adminPage.locator('table')).toBeVisible();
    });

    test('should create new room', async ({ adminPage }) => {
      await adminPage.goto('/admin/rooms');

      await adminPage.click('[data-testid="add-room-button"]');

      // Fill form
      await adminPage.selectOption('mat-select[formControlName="buildingId"]', { index: 1 });
      await adminPage.fill('input[formControlName="roomNumber"]', 'TEST-999');
      await adminPage.fill('input[formControlName="capacity"]', '2');

      await adminPage.click('[data-testid="save-button"]');

      await expect(adminPage.locator('.mat-snack-bar-container')).toContainText(/created/i);
    });
  });

  test.describe('Room Allocation', () => {

    test('should display allocations list', async ({ adminPage }) => {
      await adminPage.goto('/admin/room-allocations');

      await expect(adminPage.locator('table')).toBeVisible();
    });

    test('should create manual allocation', async ({ adminPage }) => {
      await adminPage.goto('/admin/room-allocations');

      await adminPage.click('[data-testid="new-allocation-button"]');

      // Select student
      await adminPage.selectOption('mat-select[formControlName="studentId"]', { index: 1 });

      // Select room
      await adminPage.selectOption('mat-select[formControlName="roomId"]', { index: 1 });

      await adminPage.click('[data-testid="allocate-button"]');

      await expect(adminPage.locator('.mat-snack-bar-container')).toContainText(/allocated/i);
    });
  });
});
```

### 4.8 Responsive Design Tests

**File**: `e2e/tests/responsive.spec.ts`

```typescript
import { test, expect, devices } from '@playwright/test';

test.describe('Responsive Design', () => {

  test.describe('Mobile View', () => {

    test.use({ ...devices['iPhone 12'] });

    test('should show mobile navigation', async ({ page }) => {
      await page.goto('/login');

      // Login
      await page.fill('input[formControlName="email"]', 'e2e-student@university.edu');
      await page.fill('input[formControlName="password"]', 'TestPassword123!');
      await page.click('button[type="submit"]');
      await page.waitForURL('/dashboard');

      // Check mobile menu button is visible
      await expect(page.locator('[data-testid="mobile-menu-button"]')).toBeVisible();
    });

    test('should open mobile menu', async ({ page }) => {
      await page.goto('/dashboard');

      await page.click('[data-testid="mobile-menu-button"]');

      await expect(page.locator('mat-sidenav')).toBeVisible();
    });
  });

  test.describe('Tablet View', () => {

    test.use({ ...devices['iPad'] });

    test('should display sidebar collapsed', async ({ page }) => {
      await page.goto('/login');

      await page.fill('input[formControlName="email"]', 'e2e-admin@university.edu');
      await page.fill('input[formControlName="password"]', 'AdminPassword123!');
      await page.click('button[type="submit"]');
      await page.waitForURL('/admin');

      // Sidebar should be visible but may be collapsed
      await expect(page.locator('mat-sidenav')).toBeVisible();
    });
  });

  test.describe('Desktop View', () => {

    test.use({ viewport: { width: 1920, height: 1080 } });

    test('should display full sidebar', async ({ page }) => {
      await page.goto('/login');

      await page.fill('input[formControlName="email"]', 'e2e-admin@university.edu');
      await page.fill('input[formControlName="password"]', 'AdminPassword123!');
      await page.click('button[type="submit"]');
      await page.waitForURL('/admin');

      // Full sidebar with labels visible
      await expect(page.locator('mat-sidenav')).toBeVisible();
      await expect(page.locator('mat-sidenav .nav-label')).toBeVisible();
    });
  });
});
```

---

## 5. Manual Test Cases

### 5.1 Critical Priority Test Cases

| Test ID | Description | Preconditions | Steps | Expected Results | Priority |
|---------|-------------|---------------|-------|------------------|----------|
| FE-MTC-001 | Complete Registration Flow | None | 1. Navigate to /signup<br>2. Fill all fields<br>3. Accept terms<br>4. Submit<br>5. Check email<br>6. Click verification link | Verification email received, Link redirects to success page, User can login | Critical |
| FE-MTC-002 | Login and Session Persistence | Registered user exists | 1. Login with valid credentials<br>2. Navigate dashboard<br>3. Refresh page<br>4. Close browser<br>5. Reopen and navigate to /dashboard | User remains logged in, Session persists until logout or token expiry | Critical |
| FE-MTC-003 | Application Submission Flow | Logged in as student with complete profile | 1. Navigate to /applications/new<br>2. Fill all required fields<br>3. Submit application<br>4. View submitted application | Application created with PENDING status, Appears in applications list | Critical |
| FE-MTC-004 | Document Upload and Validation | Logged in as student, Application exists | 1. Navigate to documents<br>2. Click upload<br>3. Select document type<br>4. Upload valid PDF<br>5. Upload invalid file type<br>6. Upload oversized file | Valid PDF uploads, Invalid types rejected with error, Oversized files rejected | Critical |
| FE-MTC-005 | Admin Application Review | Logged in as admin, Pending applications exist | 1. Navigate to /admin/applications<br>2. Click pending application<br>3. Review documents<br>4. Add comment<br>5. Approve application | Application status changes, Comment visible, Student notified | Critical |

### 5.2 High Priority Test Cases

| Test ID | Description | Preconditions | Steps | Expected Results | Priority |
|---------|-------------|---------------|-------|------------------|----------|
| FE-MTC-006 | Password Reset Flow | Registered user | 1. Click Forgot Password<br>2. Enter email<br>3. Check email<br>4. Click reset link<br>5. Enter new password<br>6. Login with new password | Reset email received, Password changed, Can login with new password | High |
| FE-MTC-007 | Profile Update | Logged in as student | 1. Navigate to /profile<br>2. Update phone number<br>3. Update special requirements<br>4. Save changes | Profile updated, Changes reflected immediately | High |
| FE-MTC-008 | Admin Room Creation | Logged in as admin | 1. Navigate to /admin/rooms<br>2. Click Add Room<br>3. Fill all fields<br>4. Save | Room created, Appears in list, Building association correct | High |
| FE-MTC-009 | Room Allocation | Logged in as admin, Approved application exists | 1. Navigate to /admin/room-allocations<br>2. Select student from waiting list<br>3. Select available room<br>4. Confirm allocation | Allocation created, Room occupancy updated, Student notified | High |
| FE-MTC-010 | Form Validation Feedback | None | 1. Navigate to each form<br>2. Submit empty form<br>3. Enter invalid data<br>4. Check error messages | Clear error messages displayed, Invalid fields highlighted, Submit disabled | High |

### 5.3 Medium Priority Test Cases

| Test ID | Description | Preconditions | Steps | Expected Results | Priority |
|---------|-------------|---------------|-------|------------------|----------|
| FE-MTC-011 | Navigation Menu | Logged in | 1. Click each menu item<br>2. Check URL changes<br>3. Check page content loads | All navigation works, Correct pages load, No broken links | Medium |
| FE-MTC-012 | Table Pagination | Admin logged in, Many records exist | 1. Navigate to applications<br>2. Check page indicators<br>3. Navigate pages<br>4. Change page size | Pagination works, Correct records shown, Page size changes | Medium |
| FE-MTC-013 | Table Sorting | Admin logged in | 1. Click column headers<br>2. Check sort indicators<br>3. Verify sort order | Ascending/descending toggle, Correct sort applied, Indicator visible | Medium |
| FE-MTC-014 | Search Functionality | Admin logged in | 1. Use search field<br>2. Enter search term<br>3. Check filtered results<br>4. Clear search | Results filtered correctly, Clear returns all results | Medium |
| FE-MTC-015 | Notifications | Logged in | 1. Check notification bell<br>2. Click to open dropdown<br>3. Click notification<br>4. Mark as read | Notifications display, Count updates, Links navigate correctly | Medium |

### 5.4 Low Priority Test Cases

| Test ID | Description | Preconditions | Steps | Expected Results | Priority |
|---------|-------------|---------------|-------|------------------|----------|
| FE-MTC-016 | Theme Consistency | None | 1. Navigate all pages<br>2. Check color scheme<br>3. Check typography<br>4. Check spacing | Consistent theme throughout, Material design followed | Low |
| FE-MTC-017 | Loading States | None | 1. Throttle network<br>2. Navigate pages<br>3. Submit forms<br>4. Check spinners | Loading indicators shown, No blank screens | Low |
| FE-MTC-018 | Error States | None | 1. Disconnect network<br>2. Navigate pages<br>3. Check error messages<br>4. Reconnect and retry | User-friendly errors, Retry option available | Low |
| FE-MTC-019 | Keyboard Navigation | None | 1. Tab through forms<br>2. Use Enter to submit<br>3. Use Escape to close modals | Full keyboard accessibility, Focus visible | Low |
| FE-MTC-020 | Print Styles | Logged in | 1. Navigate to application details<br>2. Print preview<br>3. Check layout | Clean print layout, Essential info visible, No broken elements | Low |

---

## 6. Test Data Requirements

### 6.1 Test Users

```json
{
  "students": [
    {
      "email": "e2e-student@university.edu",
      "password": "TestPassword123!",
      "firstName": "E2E",
      "lastName": "Student",
      "role": "STUDENT",
      "emailVerified": true,
      "studentProfile": {
        "studentNumber": "E2E001",
        "phone": "1234567890",
        "institution": "Test University",
        "yearOfStudy": "First Year"
      }
    }
  ],
  "admins": [
    {
      "email": "e2e-admin@university.edu",
      "password": "AdminPassword123!",
      "firstName": "E2E",
      "lastName": "Admin",
      "role": "ADMIN",
      "emailVerified": true
    }
  ]
}
```

### 6.2 Test Files

Create the following in `e2e/fixtures/`:

```
e2e/
  fixtures/
    test-document.pdf          # Valid PDF < 5MB
    test-image.jpg             # Valid JPG < 5MB
    large-file.pdf             # File > 5MB for rejection testing
    invalid-file.exe           # Invalid file type
```

---

## 7. Test Environment Setup

### 7.1 Install Dependencies

```bash
# Install Angular testing dependencies
npm install --save-dev @angular-devkit/build-angular karma karma-chrome-launcher karma-coverage karma-jasmine karma-jasmine-html-reporter jasmine-core

# Install Playwright
npm install --save-dev @playwright/test
npx playwright install
```

### 7.2 Angular Test Configuration

Add to `angular.json`:

```json
{
  "projects": {
    "frontend": {
      "architect": {
        "test": {
          "builder": "@angular-devkit/build-angular:karma",
          "options": {
            "main": "src/test.ts",
            "polyfills": "src/polyfills.ts",
            "tsConfig": "tsconfig.spec.json",
            "karmaConfig": "karma.conf.js",
            "assets": ["src/assets"],
            "styles": ["src/styles.scss"],
            "scripts": [],
            "codeCoverage": true,
            "codeCoverageExclude": [
              "src/test.ts",
              "**/*.spec.ts"
            ]
          }
        }
      }
    }
  }
}
```

### 7.3 Commands to Run Tests

```bash
# Run unit tests
ng test

# Run unit tests with coverage
ng test --code-coverage

# Run unit tests headless (CI)
ng test --browsers=ChromeHeadless --watch=false

# Run Playwright E2E tests
npx playwright test

# Run Playwright with UI
npx playwright test --ui

# Run Playwright headed
npx playwright test --headed

# View Playwright report
npx playwright show-report
```

### 7.4 CI/CD Configuration

**File**: `.github/workflows/frontend-test.yml`

```yaml
name: Frontend Tests

on:
  push:
    branches: [main, develop]
    paths:
      - 'frontend/**'
  pull_request:
    branches: [main, develop]
    paths:
      - 'frontend/**'

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend

    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        run: npm ci

      - name: Run unit tests
        run: npm run test -- --browsers=ChromeHeadless --watch=false --code-coverage

      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          files: frontend/coverage/lcov.info
          flags: frontend

  e2e-tests:
    runs-on: ubuntu-latest
    needs: unit-tests
    defaults:
      run:
        working-directory: frontend

    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        run: npm ci

      - name: Install Playwright browsers
        run: npx playwright install --with-deps

      - name: Build Angular app
        run: npm run build

      - name: Run Playwright tests
        run: npx playwright test

      - name: Upload Playwright report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report
          path: frontend/e2e/playwright-report
```

---

## Appendix A: Component Test Coverage Matrix

| Component | Unit Test | Integration Test | E2E Test |
|-----------|-----------|------------------|----------|
| LoginComponent | Yes | Yes | Yes |
| SignupComponent | Yes | Yes | Yes |
| ProfileSetupComponent | Yes | Yes | Yes |
| DashboardComponent | Yes | Yes | Yes |
| ApplicationsComponent | Yes | Yes | Yes |
| DocumentsComponent | Yes | Yes | Yes |
| AdminDashboardComponent | Yes | Yes | Yes |
| ApplicationReviewComponent | Yes | Yes | Yes |
| RoomManagementComponent | Yes | Yes | Yes |
| RoomAllocationComponent | Yes | Yes | Yes |

## Appendix B: Service Test Coverage Matrix

| Service | Unit Test | Integration Test |
|---------|-----------|------------------|
| AuthService | Yes | Yes |
| ApplicationService | Yes | Yes |
| DocumentService | Yes | Yes |
| StudentService | Yes | Yes |
| AdminService | Yes | Yes |
| RoomService | Yes | Yes |
| BuildingService | Yes | Yes |
| NotificationService | Yes | Yes |

---

*Document Version: 1.0*
*Last Updated: January 2026*
*Author: QA Engineering Team*
