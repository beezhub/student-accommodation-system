import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { BuildingOwnerGuard } from './building-owner.guard';
import { AuthService } from '../services/auth.service';
import { RoleService } from '../services/role.service';

describe('BuildingOwnerGuard', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let roleServiceSpy: jasmine.SpyObj<RoleService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockRoute = {} as ActivatedRouteSnapshot;
  const mockState = { url: '/owner/dashboard' } as RouterStateSnapshot;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['isTokenExpired']);
    roleServiceSpy = jasmine.createSpyObj<RoleService>('RoleService', [
      'isBuildingOwner',
      'isPlatformAdmin',
    ]);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: RoleService, useValue: roleServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });
  });

  function runGuard(): boolean | ReturnType<typeof BuildingOwnerGuard> {
    return TestBed.runInInjectionContext(() => BuildingOwnerGuard(mockRoute, mockState));
  }

  it('returns true for authenticated building owner', () => {
    authServiceSpy.isTokenExpired.and.returnValue(false);
    roleServiceSpy.isBuildingOwner.and.returnValue(true);

    const result = runGuard();

    expect(result).toBeTrue();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('redirects to /login and returns false when token is expired', () => {
    authServiceSpy.isTokenExpired.and.returnValue(true);

    const result = runGuard();

    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('redirects to /admin and returns false for platform admin', () => {
    authServiceSpy.isTokenExpired.and.returnValue(false);
    roleServiceSpy.isBuildingOwner.and.returnValue(false);
    roleServiceSpy.isPlatformAdmin.and.returnValue(true);

    const result = runGuard();

    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin']);
  });

  it('redirects to /dashboard and returns false for student', () => {
    authServiceSpy.isTokenExpired.and.returnValue(false);
    roleServiceSpy.isBuildingOwner.and.returnValue(false);
    roleServiceSpy.isPlatformAdmin.and.returnValue(false);

    const result = runGuard();

    expect(result).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  });
});
