import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { SignupComponent } from './signup.component';
import { AuthService } from '../../../core/services/auth.service';
import { PASSWORD_RULES_HINT } from '../../../shared/validators/strong-password.validator';

describe('SignupComponent', () => {
  it('passwordHint_wrapsOntoTwoLines_growsInsteadOfCoveringConfirmPassword', () => {
    TestBed.configureTestingModule({
      imports: [SignupComponent],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AuthService, useValue: jasmine.createSpyObj('AuthService', ['signup']) },
      ],
    });
    const fixture = TestBed.createComponent(SignupComponent);
    fixture.detectChanges();

    const hint = [...(fixture.nativeElement as HTMLElement).querySelectorAll('mat-hint')].find(
      (element) => element.textContent?.trim() === PASSWORD_RULES_HINT
    );
    const subscript = hint?.closest('.mat-mdc-form-field-subscript-wrapper');

    expect(subscript?.classList).toContain('mat-mdc-form-field-subscript-dynamic-size');
  });
});
