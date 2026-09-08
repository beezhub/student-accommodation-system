import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideLocationMocks } from '@angular/common/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { ChangePasswordComponent } from './change-password.component';
import { AuthService } from '../../../core/services/auth.service';
import { RoleService } from '../../../core/services/role.service';

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
      newPassword: 'newPassword1',
      confirmPassword: 'newPassword1',
    });
    component.onSubmit();
    return component;
  }

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
