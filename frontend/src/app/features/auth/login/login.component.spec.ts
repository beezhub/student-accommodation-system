import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideLocationMocks } from '@angular/common/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';

import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';
import { RoleService } from '../../../core/services/role.service';

/**
 * GH-150: guards wrote a `returnUrl` query parameter that nothing ever read. LoginComponent never
 * injected ActivatedRoute and hard-coded `/admin` or `/dashboard`, so a user interrupted mid-task
 * was always dropped at a dashboard rather than back where they were going.
 *
 * <p>These use a real Router through RouterTestingHarness rather than a Router spy. The template
 * has routerLink anchors, and RouterLink subscribes to `router.events` in its constructor and calls
 * `router.createUrlTree` during change detection, so a spy object cannot construct the component at
 * all. A hand-rolled ActivatedRoute stub fails a second way, because it is passed to createUrlTree
 * as `relativeTo` and the real Router reads snapshot fields the stub does not have.
 */
describe('LoginComponent returnUrl handling', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let roleService: jasmine.SpyObj<RoleService>;
  let router: Router;

  const student = { id: 1, email: 'a@b.com', mustChangePassword: false };

  beforeEach(() => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['login']);
    roleService = jasmine.createSpyObj<RoleService>('RoleService', [
      'isPlatformAdmin',
      'isPlatformStaff',
      'isTenantAdmin',
    ]);
    roleService.isPlatformAdmin.and.returnValue(false);
    roleService.isTenantAdmin.and.returnValue(false);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'login', component: LoginComponent }]),
        provideLocationMocks(),
        provideNoopAnimations(),
        { provide: AuthService, useValue: authService },
        { provide: RoleService, useValue: roleService },
      ],
    });
    router = TestBed.inject(Router);
  });

  /** Signs in through the component mounted at the given login URL and returns where it went. */
  async function signInFrom(loginUrl: string, user: unknown = student): Promise<void> {
    authService.login.and.returnValue(of(user) as never);
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl(loginUrl, LoginComponent);
    component.loginForm.setValue({ email: 'a@b.com', password: 'password123' });
    component.onSubmit();
  }

  it('validReturnUrl_navigatesThere', async () => {
    const navigateByUrl = spyOn(router, 'navigateByUrl').and.callThrough();

    await signInFrom('/login?returnUrl=%2Fadmin%2Frooms');

    expect(navigateByUrl).toHaveBeenCalledWith('/admin/rooms');
  });

  it('returnUrlWithQueryString_survivesIntact', async () => {
    const navigateByUrl = spyOn(router, 'navigateByUrl').and.callThrough();

    await signInFrom('/login?returnUrl=%2Fadmin%2Frooms%3Fbuilding%3D3');

    expect(navigateByUrl).toHaveBeenCalledWith('/admin/rooms?building=3');
  });

  it('noReturnUrl_fallsBackToTheRoleLanding', async () => {
    const navigate = spyOn(router, 'navigate').and.callThrough();

    await signInFrom('/login');

    expect(navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('platformStaffWithNoReturnUrl_landsOnAdmin', async () => {
    roleService.isPlatformStaff.and.returnValue(true);
    const navigate = spyOn(router, 'navigate').and.callThrough();

    await signInFrom('/login');

    expect(navigate).toHaveBeenCalledWith(['/admin']);
  });

  it('adminWithNoReturnUrl_landsOnAdmin', async () => {
    roleService.isPlatformAdmin.and.returnValue(true);
    const navigate = spyOn(router, 'navigate').and.callThrough();

    await signInFrom('/login');

    expect(navigate).toHaveBeenCalledWith(['/admin']);
  });

  // Angular's Router cannot navigate off-origin, so these are defence in depth rather than the
  // thing standing between a user and an open redirect. The loop case is the one a tester hits.
  const rejected = [
    ['absolute url', 'https%3A%2F%2Fevil.com'],
    ['protocol-relative', '%2F%2Fevil.com'],
    ['backslash-escaped', '%2F%5Cevil.com'],
    ['javascript scheme', 'javascript%3Aalert(1)'],
    ['the login page itself', '%2Flogin'],
    ['the site root', '%2F'],
  ];

  rejected.forEach(([label, encoded]) => {
    it(`rejectsReturnUrl_${label.replace(/\s/g, '')}_andUsesTheRoleLanding`, async () => {
      const navigate = spyOn(router, 'navigate').and.callThrough();

      await signInFrom(`/login?returnUrl=${encoded}`);

      // Reaching the role branch at all is the proof: an accepted returnUrl returns before it.
      expect(navigate).toHaveBeenCalledWith(['/dashboard']);
    });
  });

  /** A forced password change must win over any returnUrl. */
  it('mustChangePassword_winsOverReturnUrl', async () => {
    const navigate = spyOn(router, 'navigate').and.callThrough();

    await signInFrom('/login?returnUrl=%2Fadmin%2Frooms', { ...student, mustChangePassword: true });

    expect(navigate).toHaveBeenCalledWith(['/change-password'], {
      queryParams: { forced: 'true' },
    });
  });
});

/**
 * GH-291: after a password change the app sends the user back here with `passwordChanged=true`
 * rather than letting a stray 401 announce a successful change as a dropped session.
 */
describe('LoginComponent passwordChanged message', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let roleService: jasmine.SpyObj<RoleService>;

  beforeEach(() => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['login']);
    roleService = jasmine.createSpyObj<RoleService>('RoleService', [
      'isPlatformAdmin',
      'isPlatformStaff',
      'isTenantAdmin',
    ]);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'login', component: LoginComponent }]),
        provideLocationMocks(),
        provideNoopAnimations(),
        { provide: AuthService, useValue: authService },
        { provide: RoleService, useValue: roleService },
      ],
    });
  });

  it('passwordChanged_true_showsAClearMessage', async () => {
    // The routed component ends up with its own MatSnackBar instance rather than the one
    // TestBed.inject would hand back beforehand, so spy on the prototype instead of an instance.
    const open = spyOn(MatSnackBar.prototype, 'open').and.callThrough();

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/login?passwordChanged=true', LoginComponent);

    expect(open).toHaveBeenCalledWith(
      'Your password was changed. Please sign in again.',
      'Close',
      jasmine.any(Object)
    );
  });

  it('noPasswordChangedParam_saysNothing', async () => {
    const open = spyOn(MatSnackBar.prototype, 'open').and.callThrough();

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/login', LoginComponent);

    expect(open).not.toHaveBeenCalled();
  });
});
