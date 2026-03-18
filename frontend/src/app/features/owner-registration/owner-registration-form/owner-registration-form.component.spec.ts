import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { OwnerRegistrationFormComponent } from './owner-registration-form.component';

describe('OwnerRegistrationFormComponent', () => {
  let component: OwnerRegistrationFormComponent;
  let httpSpy: jasmine.SpyObj<HttpClient>;
  let routerSpy: jasmine.SpyObj<Router>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    httpSpy = jasmine.createSpyObj<HttpClient>('HttpClient', ['post']);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);
    snackBarSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [OwnerRegistrationFormComponent, ReactiveFormsModule, NoopAnimationsModule],
      providers: [
        { provide: HttpClient, useValue: httpSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(OwnerRegistrationFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('form is invalid when required fields are empty', () => {
    expect(component.registrationForm.valid).toBeFalse();
  });

  it('form is valid when required fields are filled', () => {
    component.registrationForm.patchValue({
      fullName: 'Jane Smith',
      email: 'jane@acme.com',
      organisationName: 'Acme Properties',
    });

    expect(component.registrationForm.valid).toBeTrue();
  });

  it('email field is invalid for non-email string', () => {
    component.registrationForm.patchValue({ email: 'not-an-email' });
    expect(component.registrationForm.get('email')?.valid).toBeFalse();
  });

  it('navigates to /owner/verification-pending on successful submit', fakeAsync(() => {
    httpSpy.post.and.returnValue(of({}));
    component.registrationForm.patchValue({
      fullName: 'Jane Smith',
      email: 'jane@acme.com',
      organisationName: 'Acme Properties',
    });

    component.onSubmit();
    tick();

    expect(routerSpy.navigate).toHaveBeenCalledWith(
      ['/owner/verification-pending'],
      { queryParams: { email: 'jane@acme.com' } }
    );
  }));

  it('shows duplicate error snackbar on 409 response', fakeAsync(() => {
    httpSpy.post.and.returnValue(throwError(() => ({ status: 409 })));
    component.registrationForm.patchValue({
      fullName: 'Jane Smith',
      email: 'jane@acme.com',
      organisationName: 'Acme Properties',
    });

    component.onSubmit();
    tick();

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'An account with this email already exists.',
      'Close',
      jasmine.objectContaining({ panelClass: ['error-snackbar'] })
    );
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  }));

  it('shows generic error snackbar on non-409 server error', fakeAsync(() => {
    httpSpy.post.and.returnValue(
      throwError(() => ({ status: 500, error: { message: 'Internal error' } }))
    );
    component.registrationForm.patchValue({
      fullName: 'Jane Smith',
      email: 'jane@acme.com',
      organisationName: 'Acme Properties',
    });

    component.onSubmit();
    tick();

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Internal error',
      'Close',
      jasmine.objectContaining({ panelClass: ['error-snackbar'] })
    );
  }));

  it('marks all fields touched and shows warning when submitting invalid form', () => {
    component.onSubmit();

    expect(component.registrationForm.touched).toBeTrue();
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Please fill in all required fields correctly.',
      'Close',
      jasmine.objectContaining({ panelClass: ['warning-snackbar'] })
    );
    expect(httpSpy.post).not.toHaveBeenCalled();
  });

  it('sets isSubmitting to false after successful response', fakeAsync(() => {
    httpSpy.post.and.returnValue(of({}));
    component.registrationForm.patchValue({
      fullName: 'Jane Smith',
      email: 'jane@acme.com',
      organisationName: 'Acme Properties',
    });

    component.onSubmit();
    tick();

    expect(component.isSubmitting).toBeFalse();
  }));

  it('sets isSubmitting to false after error response', fakeAsync(() => {
    httpSpy.post.and.returnValue(throwError(() => ({ status: 500 })));
    component.registrationForm.patchValue({
      fullName: 'Jane Smith',
      email: 'jane@acme.com',
      organisationName: 'Acme Properties',
    });

    component.onSubmit();
    tick();

    expect(component.isSubmitting).toBeFalse();
  }));
});
