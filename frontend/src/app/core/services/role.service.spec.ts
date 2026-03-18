import { TestBed } from '@angular/core/testing';
import { RoleService } from './role.service';
import { AuthService } from './auth.service';
import { ROLE } from '../models/role.model';

describe('RoleService', () => {
  let service: RoleService;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', [], {
      currentUserValue: null,
    });

    TestBed.configureTestingModule({
      providers: [RoleService, { provide: AuthService, useValue: authServiceSpy }],
    });
    service = TestBed.inject(RoleService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('isPlatformAdmin_whenUserIsPlatformAdmin_returnsTrue', () => {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue({ userRole: ROLE.PLATFORM_ADMIN });
    expect(service.isPlatformAdmin()).toBeTrue();
  });

  it('isPlatformAdmin_whenUserIsStudent_returnsFalse', () => {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue({ userRole: ROLE.STUDENT });
    expect(service.isPlatformAdmin()).toBeFalse();
  });

  it('isPlatformAdmin_whenUserIsNull_returnsFalse', () => {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue(null);
    expect(service.isPlatformAdmin()).toBeFalse();
  });

  it('isStudent_whenUserIsStudent_returnsTrue', () => {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue({ userRole: ROLE.STUDENT });
    expect(service.isStudent()).toBeTrue();
  });

  it('isStudent_whenUserIsPlatformAdmin_returnsFalse', () => {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue({ userRole: ROLE.PLATFORM_ADMIN });
    expect(service.isStudent()).toBeFalse();
  });

  it('isBuildingStaff_whenUserIsBuildingStaff_returnsTrue', () => {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue({ userRole: ROLE.BUILDING_STAFF });
    expect(service.isBuildingStaff()).toBeTrue();
  });

  it('isAdmin_whenUserIsPlatformAdmin_returnsTrue', () => {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue({ userRole: ROLE.PLATFORM_ADMIN });
    expect(service.isAdmin()).toBeTrue();
  });

  it('isAdmin_whenUserIsBuildingStaff_returnsTrue', () => {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue({ userRole: ROLE.BUILDING_STAFF });
    expect(service.isAdmin()).toBeTrue();
  });

  it('isAdmin_whenUserIsStudent_returnsFalse', () => {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue({ userRole: ROLE.STUDENT });
    expect(service.isAdmin()).toBeFalse();
  });

  it('hasRole_withUnknownRole_returnsFalse', () => {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue({ userRole: 'UNKNOWN_ROLE' });
    expect(service.isPlatformAdmin()).toBeFalse();
    expect(service.isStudent()).toBeFalse();
    expect(service.isBuildingStaff()).toBeFalse();
  });

  it('hasRole_whenCurrentUserIsNull_returnsFalse', () => {
    (
      Object.getOwnPropertyDescriptor(authServiceSpy, 'currentUserValue')!.get as jasmine.Spy
    ).and.returnValue(null);
    expect(service.isPlatformAdmin()).toBeFalse();
    expect(service.isStudent()).toBeFalse();
    expect(service.isBuildingStaff()).toBeFalse();
    expect(service.isAdmin()).toBeFalse();
  });
});
