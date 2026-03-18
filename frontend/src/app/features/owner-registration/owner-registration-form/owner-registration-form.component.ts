import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-owner-registration-form',
  templateUrl: './owner-registration-form.component.html',
  styleUrls: ['./owner-registration-form.component.css'],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OwnerRegistrationFormComponent {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private http = inject(HttpClient);
  private snackBar = inject(MatSnackBar);

  registrationForm: FormGroup;
  isSubmitting = false;

  constructor() {
    this.registrationForm = this.fb.group({
      fullName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      organisationName: ['', [Validators.required]],
      contactPhone: [''],
      message: [''],
    });
  }

  onSubmit(): void {
    if (this.registrationForm.valid) {
      this.isSubmitting = true;
      this.http
        .post(`${environment.apiUrl}/public/owner/register`, this.registrationForm.value)
        .subscribe({
          next: () => {
            this.isSubmitting = false;
            this.router.navigate(['/owner/verification-pending'], {
              queryParams: { email: this.registrationForm.get('email')?.value },
            });
          },
          error: (error) => {
            this.isSubmitting = false;
            const status = error?.status;
            const message =
              status === 409
                ? 'An account with this email already exists.'
                : error?.error?.message || 'Registration failed. Please try again.';
            this.snackBar.open(message, 'Close', {
              duration: 5000,
              panelClass: ['error-snackbar'],
            });
          },
        });
    } else {
      this.registrationForm.markAllAsTouched();
      this.snackBar.open('Please fill in all required fields correctly.', 'Close', {
        duration: 3000,
        panelClass: ['warning-snackbar'],
      });
    }
  }
}
