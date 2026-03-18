import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { StudentGuard } from './student.guard';
import { AuthService } from '../services/auth.service';
import { RoleService } from '../services/role.service';
import { ROLE } from '../models/role.model';

describe('StudentGuard', () => {
  let guard: StudentGuard;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let roleServiceSpy: jasmine.SpyObj<RoleService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockRoute = {} as ActivatedRouteSnapshot;
  const mockState = { url: '/dashboard' } as RouterStateSnapshot;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', [], {
      currentUserValue: null,
    });
    roleServiceSpy = jasmine.createSpyObj<RoleService>('RoleService', [
      'isStudent',
      'isPlatformAdmin',
    ]);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        StudentGuard,
        { provide: AuthService, useValue: authServiceSpy },
        { provide: RoleService, useValue: roleServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });
    guard = TestBed.inject(StudentGuard);
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });

  it('canActivate_studentUser_returnsTrue', () => {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue({ userRole: ROLE.STUDENT });
    roleServiceSpy.isStudent.and.returnValue(true);
    roleServiceSpy.isPlatformAdmin.and.returnValue(false);

    const result = guard.canActivate(mockRoute, mockState);

    expect(result).toBeTrue();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('canActivate_platformAdminUser_redirectsToAdmin_returnsFalse', () => {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue({ userRole: ROLE.PLATFORM_ADMIN });
    roleServiceSpy.isStudent.and.returnValue(false);
    roleServiceSpy.isPlatformAdmin.and.returnValue(true);

    const result = guard.canActivate(mockRoute, mockState);

    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin']);
  });

  it('canActivate_buildingStaffUser_redirectsToHome_returnsFalse', () => {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue({ userRole: ROLE.BUILDING_STAFF });
    roleServiceSpy.isStudent.and.returnValue(false);
    roleServiceSpy.isPlatformAdmin.and.returnValue(false);

    const result = guard.canActivate(mockRoute, mockState);

    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/home']);
  });

  it('canActivate_unauthenticatedUser_redirectsToLogin_returnsFalse', () => {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue(null);
    roleServiceSpy.isStudent.and.returnValue(false);
    roleServiceSpy.isPlatformAdmin.and.returnValue(false);

    const result = guard.canActivate(mockRoute, mockState);

    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/dashboard' },
    });
  });
});
