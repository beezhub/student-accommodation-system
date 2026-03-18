import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-owner-set-password',
  templateUrl: './owner-set-password.component.html',
  styleUrls: ['./owner-set-password.component.css'],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OwnerSetPasswordComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private snackBar = inject(MatSnackBar);
  private cdr = inject(ChangeDetectorRef);

  passwordForm: FormGroup;
  token = '';
  isSubmitting = false;
  hidePassword = true;
  hideConfirm = true;
  tokenExpired = false;

  constructor() {
    this.passwordForm = this.fb.group(
      {
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required]],
      },
      { validators: this.passwordMatchValidator }
    );
  }

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParams['token'] || '';
    if (!this.token) {
      this.tokenExpired = true;
      this.cdr.markForCheck();
    }
  }

  passwordMatchValidator(g: FormGroup) {
    const pw = g.get('password')?.value;
    const cpw = g.get('confirmPassword')?.value;
    if (pw !== cpw) {
      g.get('confirmPassword')?.setErrors({ mismatch: true });
      return { mismatch: true };
    }
    return null;
  }

  onSubmit(): void {
    if (this.passwordForm.valid) {
      this.isSubmitting = true;
      this.http
        .post(`${environment.apiUrl}/public/owner/set-password`, {
          token: this.token,
          password: this.passwordForm.get('password')?.value,
        })
        .subscribe({
          next: () => {
            this.isSubmitting = false;
            this.snackBar.open('Password set! You can now log in.', 'Close', {
              duration: 5000,
              panelClass: ['success-snackbar'],
            });
            this.router.navigate(['/login']);
          },
          error: (error) => {
            this.isSubmitting = false;
            this.cdr.markForCheck();
            if (error?.status === 400 || error?.status === 404) {
              this.tokenExpired = true;
              this.cdr.markForCheck();
            } else {
              this.snackBar.open('Failed to set password. Please try again.', 'Close', {
                duration: 4000,
                panelClass: ['error-snackbar'],
              });
            }
          },
        });
    } else {
      this.passwordForm.markAllAsTouched();
    }
  }
}
