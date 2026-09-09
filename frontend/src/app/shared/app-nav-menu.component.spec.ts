import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { AppNavMenuComponent } from './app-nav-menu.component';
import { AuthService } from '../core/services/auth.service';
import { RoleService } from '../core/services/role.service';
import { ROLE } from '../core/models/role.model';

/**
 * GH-304: isStudent() used to compare userRole against the literal string 'STUDENT' instead of
 * going through RoleService, the same pattern GH-158 fixed in admin-sidebar and user-management.
 */
describe('AppNavMenuComponent', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let roleService: jasmine.SpyObj<RoleService>;

  beforeEach(() => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', [], {
      currentUserValue: {
        id: '1',
        firstName: 'Ada',
        lastName: 'Lovelace',
        email: 'ada@example.com',
        userRole: 'STUDENT',
        profileCompleted: true,
      },
    });
    roleService = jasmine.createSpyObj<RoleService>('RoleService', ['isStudent']);

    TestBed.configureTestingModule({
      imports: [AppNavMenuComponent, HttpClientTestingModule],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AuthService, useValue: authService },
        { provide: RoleService, useValue: roleService },
      ],
    });
  });

  it('isStudent defers to RoleService rather than comparing userRole directly', () => {
    roleService.isStudent.and.returnValue(true);
    const fixture = TestBed.createComponent(AppNavMenuComponent);

    expect(fixture.componentInstance.isStudent()).toBe(true);
    expect(roleService.isStudent).toHaveBeenCalled();
  });

  it('isStudent reflects RoleService.isStudent() returning false', () => {
    roleService.isStudent.and.returnValue(false);
    const fixture = TestBed.createComponent(AppNavMenuComponent);

    expect(fixture.componentInstance.isStudent()).toBe(false);
  });
});

/**
 * These use the real RoleService rather than a spy, so the admin roles below only pass if
 * RoleService itself keeps treating them as non-student. role.service.spec.ts covers each of
 * these roles for RoleService's own methods; this checks the nav menu behaves accordingly.
 */
describe('AppNavMenuComponent with the real RoleService (admin paths)', () => {
  let authService: jasmine.SpyObj<AuthService>;

  function setUpWithRole(userRole: string) {
    authService = jasmine.createSpyObj<AuthService>('AuthService', [], {
      currentUserValue: {
        id: '1',
        firstName: 'Ada',
        lastName: 'Lovelace',
        email: 'ada@example.com',
        userRole,
        profileCompleted: true,
      },
    });

    TestBed.configureTestingModule({
      imports: [AppNavMenuComponent, HttpClientTestingModule],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AuthService, useValue: authService },
      ],
    });

    return TestBed.createComponent(AppNavMenuComponent);
  }

  for (const role of [
    ROLE.PLATFORM_ADMIN,
    ROLE.PLATFORM_STAFF,
    ROLE.TENANT_ADMIN,
    ROLE.BUILDING_STAFF,
  ]) {
    it(`isStudent() is false for ${role}, so the student-only nav links do not render`, () => {
      const fixture = setUpWithRole(role);

      expect(fixture.componentInstance.isStudent()).toBe(false);

      fixture.detectChanges();
      const links = fixture.nativeElement.querySelectorAll('.nav-links .nav-link');
      expect(links.length).toBe(0);
    });
  }

  it('isStudent() is true for STUDENT, so the student-only nav links render', () => {
    const fixture = setUpWithRole(ROLE.STUDENT);

    expect(fixture.componentInstance.isStudent()).toBe(true);

    fixture.detectChanges();
    const links = fixture.nativeElement.querySelectorAll('.nav-links .nav-link');
    expect(links.length).toBe(3);
  });
});
