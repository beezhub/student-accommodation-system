import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideLocationMocks } from '@angular/common/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { ResetPasswordComponent } from './reset-password.component';
import { AuthService } from '../../../core/services/auth.service';
import { PASSWORD_RULES_HINT } from '../../../shared/validators/strong-password.validator';

/**
 * #430: the form asked for 8 characters while the server's @StrongPassword wants 12 and four kinds
 * of character, so a password that followed the page's own advice came back as a 400. The token was
 * valid and the email had worked, which made it look like a broken reset.
 */
describe('ResetPasswordComponent', () => {
  const STRONG = 'Correct-Horse9';
  const POLICY =
    'Password must be at least 12 characters and include an uppercase letter, a lowercase letter, a digit and a special character';

  let authService: jasmine.SpyObj<AuthService>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let harness: RouterTestingHarness;
  let component: ResetPasswordComponent;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', [
      'verifyResetToken',
      'resetPassword',
    ]);
    authService.verifyResetToken.and.returnValue(of({ valid: true, message: 'Token is valid' }));
    authService.resetPassword.and.returnValue(of({ message: 'Password has been reset' }));

    TestBed.configureTestingModule({
      imports: [ResetPasswordComponent],
      providers: [
        provideRouter([{ path: 'reset-password', component: ResetPasswordComponent }]),
        provideLocationMocks(),
        provideNoopAnimations(),
        { provide: AuthService, useValue: authService },
      ],
    });
    // The component imports MatSnackBarModule itself, so the spy has to be an override.
    snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    TestBed.overrideProvider(MatSnackBar, { useValue: snackBar });

    harness = await RouterTestingHarness.create();
    component = await harness.navigateByUrl('/reset-password?token=abc', ResetPasswordComponent);
    harness.detectChanges();
  });

  function enter(password: string): void {
    component.resetPasswordForm.setValue({ newPassword: password, confirmPassword: password });
  }

  function pageText(): string {
    return (harness.routeNativeElement as HTMLElement).textContent ?? '';
  }

  it('passwordTheServerWouldRefuse_isNotSent', () => {
    enter('simplepassword');

    component.onSubmit();

    expect(component.resetPasswordForm.valid).toBeFalse();
    expect(authService.resetPassword).not.toHaveBeenCalled();
  });

  it('passwordTheServerWouldRefuse_saysWhichRulesAreMissing', () => {
    enter('simplepassword');
    component.resetPasswordForm.markAllAsTouched();

    harness.detectChanges();

    expect(pageText()).toContain('Password needs an uppercase letter, a number, a symbol');
  });

  it('passwordMeetingTheRules_isSent', () => {
    enter(STRONG);

    component.onSubmit();

    expect(authService.resetPassword).toHaveBeenCalledOnceWith('abc', STRONG);
  });

  it('page_describesTheRealRulesAndNotEightCharacters', () => {
    expect(pageText()).toContain(PASSWORD_RULES_HINT);
    expect(pageText()).not.toContain('8 characters');
  });

  /**
   * Validation failures arrive as errors[], with no message. The page fell back to Angular's own
   * "Http failure response for ..." text, so the user was never told what was wrong.
   */
  it('validationErrorFromTheServer_showsTheServersReason', () => {
    authService.resetPassword.and.returnValue(
      throwError(
        () => new HttpErrorResponse({ status: 400, error: { status: 400, errors: [POLICY] } })
      )
    );
    enter(STRONG);

    component.onSubmit();

    expect(snackBar.open).toHaveBeenCalledWith(POLICY, 'Close', jasmine.anything());
  });

  it('invalidTokenFromTheServer_showsTheServersReason', () => {
    authService.resetPassword.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { status: 400, errors: ['Invalid password reset token'] },
          })
      )
    );
    enter(STRONG);

    component.onSubmit();

    expect(snackBar.open).toHaveBeenCalledWith(
      'Invalid password reset token',
      'Close',
      jasmine.anything()
    );
  });

  it('passwordHint_wrapsOntoTwoLines_growsInsteadOfCoveringTheNextField', () => {
    const hint = [...(harness.routeNativeElement as HTMLElement).querySelectorAll('mat-hint')].find(
      (element) => element.textContent?.trim() === PASSWORD_RULES_HINT
    );
    const subscript = hint?.closest('.mat-mdc-form-field-subscript-wrapper');

    expect(subscript?.classList).toContain('mat-mdc-form-field-subscript-dynamic-size');
  });
});
