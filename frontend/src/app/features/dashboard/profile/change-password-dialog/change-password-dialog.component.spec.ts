import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { ChangePasswordDialogComponent } from './change-password-dialog.component';
import { AuthService } from '../../../../core/services/auth.service';
import { PASSWORD_RULES_HINT } from '../../../../shared/validators/strong-password.validator';

/** #430: the dialog asked for 8 characters while the server's @StrongPassword refuses them. */
describe('ChangePasswordDialogComponent', () => {
  const STRONG = 'Correct-Horse9';

  let authService: jasmine.SpyObj<AuthService>;
  let fixture: ComponentFixture<ChangePasswordDialogComponent>;
  let component: ChangePasswordDialogComponent;

  beforeEach(() => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['changePassword']);
    authService.changePassword.and.returnValue(of({ message: 'Password changed' }));

    TestBed.configureTestingModule({
      imports: [ChangePasswordDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: MatDialogRef, useValue: { close: jasmine.createSpy('close') } },
        { provide: AuthService, useValue: authService },
      ],
    });
    fixture = TestBed.createComponent(ChangePasswordDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function enter(password: string): void {
    component.passwordForm.setValue({
      currentPassword: 'old-password',
      newPassword: password,
      confirmPassword: password,
    });
  }

  function dialogHtml(): string {
    return (fixture.nativeElement as HTMLElement).innerHTML;
  }

  it('passwordTheServerWouldRefuse_isNotSent', () => {
    enter('simplepassword');

    component.onSubmit();

    expect(component.passwordForm.valid).toBeFalse();
    expect(authService.changePassword).not.toHaveBeenCalled();
  });

  it('passwordTheServerWouldRefuse_saysWhichRulesAreMissing', () => {
    enter('simplepassword');
    component.passwordForm.markAllAsTouched();

    fixture.detectChanges();

    expect(dialogHtml()).toContain('Password needs an uppercase letter, a number, a symbol');
  });

  it('passwordMeetingTheRules_isSent', () => {
    enter(STRONG);

    component.onSubmit();

    expect(authService.changePassword).toHaveBeenCalledOnceWith('old-password', STRONG);
  });

  it('dialog_describesTheRealRulesAndNotEightCharacters', () => {
    expect(dialogHtml()).toContain(PASSWORD_RULES_HINT);
    expect(dialogHtml()).not.toContain('8 characters');
  });

  it('validationErrorFromTheServer_showsTheServersReason', () => {
    authService.changePassword.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { status: 400, errors: ['Password must be at least 12 characters'] },
          })
      )
    );
    enter(STRONG);

    component.onSubmit();

    expect(component.errorMessage).toBe('Password must be at least 12 characters');
  });
});
