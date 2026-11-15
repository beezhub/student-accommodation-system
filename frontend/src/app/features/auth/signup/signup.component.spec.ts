import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { SignupComponent } from './signup.component';
import { AuthService } from '../../../core/services/auth.service';
import { PASSWORD_RULES_HINT } from '../../../shared/validators/strong-password.validator';

describe('SignupComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [SignupComponent],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AuthService, useValue: jasmine.createSpyObj('AuthService', ['signup']) },
      ],
    });
  });

  function privacyLink(fixture: ComponentFixture<SignupComponent>): HTMLAnchorElement {
    const link = (fixture.nativeElement as HTMLElement).querySelector<HTMLAnchorElement>(
      'a.privacy-link'
    );
    expect(link).withContext('privacy policy link').not.toBeNull();
    return link as HTMLAnchorElement;
  }

  it('privacyLink_always_opensThePolicyInANewTab', () => {
    const fixture = TestBed.createComponent(SignupComponent);
    fixture.detectChanges();

    const link = privacyLink(fixture);

    expect(link.getAttribute('href')).toBe('/privacy');
    expect(link.target).toBe('_blank');
    expect(link.rel).toContain('noopener');
  });

  it('privacyLink_clicked_leavesTheAgreementUnticked', () => {
    const fixture = TestBed.createComponent(SignupComponent);
    fixture.detectChanges();
    const link = privacyLink(fixture);
    // A real click would open a tab, so point the link at a URL that does nothing. It still has an
    // href, and a link with an href is what the browser activates instead of the label around it.
    link.removeAttribute('target');
    link.setAttribute('href', 'javascript:void(0)');

    link.click();
    fixture.detectChanges();

    expect(fixture.componentInstance.signupForm.get('agreeTerms')?.value).toBeFalse();
    expect(
      (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>(
        '.terms-checkbox input[type="checkbox"]'
      )?.checked
    ).toBeFalse();
  });

  it('passwordHint_wrapsOntoTwoLines_growsInsteadOfCoveringConfirmPassword', () => {
    const fixture = TestBed.createComponent(SignupComponent);
    fixture.detectChanges();

    const hint = [...(fixture.nativeElement as HTMLElement).querySelectorAll('mat-hint')].find(
      (element) => element.textContent?.trim() === PASSWORD_RULES_HINT
    );
    const subscript = hint?.closest('.mat-mdc-form-field-subscript-wrapper');

    expect(subscript?.classList).toContain('mat-mdc-form-field-subscript-dynamic-size');
  });
});
