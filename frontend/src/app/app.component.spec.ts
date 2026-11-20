import { hidesToolbar } from './app.component';

describe('hidesToolbar', () => {
  it('hidesToolbar_emailVerificationRequired_hidesTheNavbar', () => {
    expect(hidesToolbar('/email-verification-required')).toBeTrue();
  });

  it('hidesToolbar_signUpPagesWithQuery_hidesTheNavbar', () => {
    expect(hidesToolbar('/verify-email?token=abc')).toBeTrue();
    expect(hidesToolbar('/login')).toBeTrue();
    expect(hidesToolbar('/buildings/4')).toBeTrue();
  });

  it('hidesToolbar_privacyPolicy_hidesTheNavbar', () => {
    // Public page with its own header: the navbar's account menu assumes a signed-in user.
    expect(hidesToolbar('/privacy')).toBeTrue();
  });

  it('hidesToolbar_studentPages_keepsTheNavbar', () => {
    expect(hidesToolbar('/dashboard')).toBeFalse();
    expect(hidesToolbar('/documents')).toBeFalse();
    expect(hidesToolbar('/login-help')).toBeFalse();
  });
});
