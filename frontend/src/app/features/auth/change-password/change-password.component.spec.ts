import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideLocationMocks } from '@angular/common/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { ChangePasswordComponent } from './change-password.component';
import { AuthService } from '../../../core/services/auth.service';
import { RoleService } from '../../../core/services/role.service';
import { PASSWORD_RULES_HINT } from '../../../shared/validators/strong-password.validator';

/**
 * GH-291: a completed password change used to leave the blacklisted old token in place and send
 * the user into `/admin` or `/dashboard`, where the first API call failed with 401 and the error
 * interceptor bounced them straight back to a confusing "session expired" login page. The fix
 * ends the local session itself and sends the user to a fresh login with a clear reason.
 */
describe('ChangePasswordComponent', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let roleService: jasmine.SpyObj<RoleService>;
  let router: Router;

  beforeEach(() => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', [
      'changePassword',
      'endSessionAfterPasswordChange',
    ]);
    roleService = jasmine.createSpyObj<RoleService>('RoleService', [
      'isPlatformAdmin',
      'isTenantAdmin',
    ]);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'change-password', component: ChangePasswordComponent }]),
        provideLocationMocks(),
        provideNoopAnimations(),
        { provide: AuthService, useValue: authService },
        { provide: RoleService, useValue: roleService },
      ],
    });

    router = TestBed.inject(Router);
  });

  async function submitFrom(url: string): Promise<ChangePasswordComponent> {
    authService.changePassword.and.returnValue(of({ message: 'Password changed' }));
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl(url, ChangePasswordComponent);
    component.changePasswordForm.setValue({
      currentPassword: 'oldPassword1',
      newPassword: 'Correct-Horse9',
      confirmPassword: 'Correct-Horse9',
    });
    component.onSubmit();
    return component;
  }

  /**
   * #430: this page is forced on everyone at first login, and it asked for 8 characters while the
   * server's @StrongPassword refuses them, so following the page's advice left them stuck.
   */
  describe('password rules', () => {
    let harness: RouterTestingHarness;
    let component: ChangePasswordComponent;

    beforeEach(async () => {
      harness = await RouterTestingHarness.create();
      component = await harness.navigateByUrl('/change-password', ChangePasswordComponent);
      harness.detectChanges();
    });

    function enter(password: string): void {
      component.changePasswordForm.setValue({
        currentPassword: 'oldPassword1',
        newPassword: password,
        confirmPassword: password,
      });
    }

    function pageText(): string {
      return (harness.routeNativeElement as HTMLElement).textContent ?? '';
    }

    it('passwordTheServerWouldRefuse_isNotSent', () => {
      enter('simplepassword');

      component.onSubmit();

      expect(component.changePasswordForm.valid).toBeFalse();
      expect(authService.changePassword).not.toHaveBeenCalled();
    });

    it('passwordTheServerWouldRefuse_saysWhichRulesAreMissing', () => {
      enter('simplepassword');
      component.changePasswordForm.markAllAsTouched();

      harness.detectChanges();

      expect(pageText()).toContain('Password needs an uppercase letter, a number, a symbol');
    });

    it('page_describesTheRealRulesAndNotEightCharacters', () => {
      expect(pageText()).toContain(PASSWORD_RULES_HINT);
      expect(pageText()).not.toContain('8 characters');
    });
  });

  it('success_endsTheLocalSessionInsteadOfNavigatingInWithTheOldToken', async () => {
    await submitFrom('/change-password?forced=true');

    expect(authService.endSessionAfterPasswordChange).toHaveBeenCalled();
  });

  it('success_navigatesToLoginWithAClearReason', async () => {
    const navigate = spyOn(router, 'navigate').and.callThrough();

    await submitFrom('/change-password?forced=true');

    expect(navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { passwordChanged: 'true' },
    });
  });
});
