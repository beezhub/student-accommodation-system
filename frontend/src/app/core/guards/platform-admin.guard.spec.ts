import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { PlatformAdminGuard } from './platform-admin.guard';
import { AuthService } from '../services/auth.service';
import { RoleService } from '../services/role.service';
import { ROLE } from '../models/role.model';

describe('PlatformAdminGuard', () => {
  let guard: PlatformAdminGuard;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let roleServiceSpy: jasmine.SpyObj<RoleService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockRoute = {} as ActivatedRouteSnapshot;
  const mockState = { url: '/admin/tenants' } as RouterStateSnapshot;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', [], {
      currentUserValue: null,
    });
    roleServiceSpy = jasmine.createSpyObj<RoleService>('RoleService', [
      'isPlatformAdmin',
      'isPlatformStaff',
      'isTenantAdmin',
    ]);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        PlatformAdminGuard,
        { provide: AuthService, useValue: authServiceSpy },
        { provide: RoleService, useValue: roleServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });
    guard = TestBed.inject(PlatformAdminGuard);
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });

  it('canActivate_platformAdminUser_returnsTrue', () => {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue({ userRole: ROLE.PLATFORM_ADMIN });
    roleServiceSpy.isPlatformAdmin.and.returnValue(true);
    roleServiceSpy.isPlatformStaff.and.returnValue(false);
    roleServiceSpy.isTenantAdmin.and.returnValue(false);

    const result = guard.canActivate(mockRoute, mockState);

    expect(result).toBeTrue();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('canActivate_tenantAdminUser_redirectsToAdmin_returnsFalse', () => {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue({ userRole: ROLE.TENANT_ADMIN });
    roleServiceSpy.isPlatformAdmin.and.returnValue(false);
    roleServiceSpy.isPlatformStaff.and.returnValue(false);
    roleServiceSpy.isTenantAdmin.and.returnValue(true);

    const result = guard.canActivate(mockRoute, mockState);

    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin']);
  });

  it('canActivate_platformStaffUser_redirectsToAdmin_returnsFalse', () => {
    // Staff hold /admin but none of its governance children. Letting them through here would
    // render a screen the backend then answers with a 403 (GH-238).
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue({ userRole: ROLE.PLATFORM_STAFF });
    roleServiceSpy.isPlatformAdmin.and.returnValue(false);
    roleServiceSpy.isPlatformStaff.and.returnValue(true);
    roleServiceSpy.isTenantAdmin.and.returnValue(false);

    const result = guard.canActivate(mockRoute, mockState);

    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin']);
  });

  it('canActivate_studentUser_redirectsToDashboard_returnsFalse', () => {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue({ userRole: ROLE.STUDENT });
    roleServiceSpy.isPlatformAdmin.and.returnValue(false);
    roleServiceSpy.isPlatformStaff.and.returnValue(false);
    roleServiceSpy.isTenantAdmin.and.returnValue(false);

    const result = guard.canActivate(mockRoute, mockState);

    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('canActivate_unauthenticatedUser_redirectsToLogin_returnsFalse', () => {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue(null);
    roleServiceSpy.isPlatformAdmin.and.returnValue(false);
    roleServiceSpy.isPlatformStaff.and.returnValue(false);
    roleServiceSpy.isTenantAdmin.and.returnValue(false);

    const result = guard.canActivate(mockRoute, mockState);

    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/admin/tenants' },
    });
  });
});
