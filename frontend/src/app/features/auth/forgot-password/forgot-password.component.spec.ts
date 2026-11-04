import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { ForgotPasswordComponent } from './forgot-password.component';
import { AuthService } from '../../../core/services/auth.service';

describe('ForgotPasswordComponent', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let open: jasmine.Spy;
  let component: ForgotPasswordComponent;

  beforeEach(() => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['forgotPassword']);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AuthService, useValue: authService },
      ],
    });
    open = spyOn(MatSnackBar.prototype, 'open').and.callThrough();
    component = TestBed.createComponent(ForgotPasswordComponent).componentInstance;
  });

  /** #441: a refused request showed Angular's "Http failure response for ..." text. */
  it('onSubmit_requestRefused_showsTheBackendReason', () => {
    authService.forgotPassword.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 429,
            url: 'http://localhost:8080/api/auth/forgot-password',
            error: { status: 429, errors: ['Too many requests. Please try again later.'] },
          })
      )
    );

    component.forgotPasswordForm.setValue({ email: 'a@b.com' });
    component.onSubmit();

    expect(component.isSubmitting).toBeFalse();
    expect(component.emailSent).toBeFalse();
    expect(open).toHaveBeenCalledWith(
      'Too many requests. Please try again later.',
      'Close',
      jasmine.any(Object)
    );
  });

  it('onSubmit_accepted_saysTheEmailWasSent', () => {
    authService.forgotPassword.and.returnValue(of({ message: 'Check your email' }));

    component.forgotPasswordForm.setValue({ email: 'a@b.com' });
    component.onSubmit();

    expect(authService.forgotPassword).toHaveBeenCalledWith('a@b.com');
    expect(component.emailSent).toBeTrue();
    expect(open).toHaveBeenCalledWith('Check your email', 'Close', jasmine.any(Object));
  });

  it('onSubmit_invalidEmail_warnsAndSendsNothing', () => {
    component.forgotPasswordForm.setValue({ email: 'not-an-email' });
    component.onSubmit();

    expect(authService.forgotPassword).not.toHaveBeenCalled();
    expect(component.forgotPasswordForm.get('email')?.touched).toBeTrue();
  });

  it('backToLogin_goesToLogin', () => {
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

    component.backToLogin();

    expect(navigate).toHaveBeenCalledWith(['/login']);
  });
});
