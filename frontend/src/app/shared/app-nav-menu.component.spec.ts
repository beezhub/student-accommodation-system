import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { AppNavMenuComponent } from './app-nav-menu.component';
import { AuthService } from '../core/services/auth.service';
import { RoleService } from '../core/services/role.service';

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
