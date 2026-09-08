import { Component, inject } from '@angular/core';

import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { RoleService } from '../../../core/services/role.service';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
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
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);
  private roleService = inject(RoleService);
  private snackBar = inject(MatSnackBar);

  loginForm: FormGroup;
  isSubmitting = false;
  errorMessage: string = '';
  hidePassword = true;
  constructor() {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
    });

    // Landing here straight after a password change is the expected outcome, not a dropped
    // session, so say so plainly instead of letting an unrelated 401 announce it as an expiry
    // (GH-291).
    if (this.route.snapshot.queryParamMap.get('passwordChanged') === 'true') {
      this.snackBar.open('Your password was changed. Please sign in again.', 'Close', {
        duration: 5000,
        panelClass: ['success-snackbar'],
      });
    }
  }
  onSubmit(): void {
    if (this.loginForm.valid) {
      this.isSubmitting = true;
      this.errorMessage = '';

      const loginData = {
        email: this.loginForm.get('email')?.value,
        password: this.loginForm.get('password')?.value,
      };

      this.authService.login(loginData).subscribe({
        next: (user) => {
          this.isSubmitting = false;

          // Check if user must change password
          if (user.mustChangePassword) {
            this.snackBar.open('You must change your password before continuing.', 'Close', {
              duration: 5000,
              panelClass: ['warning-snackbar'],
            });
            this.router.navigate(['/change-password'], { queryParams: { forced: 'true' } });
          } else {
            this.snackBar.open('Login successful!', 'Close', {
              duration: 3000,
              panelClass: ['success-snackbar'],
            });
            // Back to whatever the guard interrupted, if anything.
            const target = this.safeReturnUrl(this.route.snapshot.queryParamMap.get('returnUrl'));
            if (target) {
              // navigateByUrl, so a returnUrl carrying its own query string or fragment survives.
              this.router.navigateByUrl(target);
              return;
            }

            // Redirect based on user role
            if (this.roleService.isPlatformAdmin() || this.roleService.isTenantAdmin()) {
              this.router.navigate(['/admin']);
            } else {
              this.router.navigate(['/dashboard']);
            }
          }
        },
        error: (error: HttpErrorResponse) => {
          this.isSubmitting = false;
          this.errorMessage =
            error?.error?.message || error?.message || 'Login failed. Please try again.';
          this.snackBar.open(this.errorMessage, 'Close', {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            duration: 5000,
            panelClass: ['error-snackbar'],
          });
        },
      });
    } else {
      this.loginForm.markAllAsTouched();
      this.snackBar.open('Please fill in all required fields correctly.', 'Close', {
        duration: 3000,
        panelClass: ['warning-snackbar'],
      });
    }
  }

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }

  /**
   * Deny-by-default check on the returnUrl query parameter.
   *
   * <p>Angular routing cannot navigate off-origin, so this is defence in depth rather than the
   * thing standing between a user and an open redirect. It also stops a redirect loop back to the
   * login page, which is the failure a tester would actually hit.
   */
  private safeReturnUrl(raw: string | null): string | null {
    if (!raw || !raw.startsWith('/')) return null;
    if (raw.startsWith('//') || raw.startsWith('/\\')) return null;
    if (raw.includes('://')) return null;
    const path = raw.split(/[?#]/)[0];
    if (path === '/login' || path === '/') return null;
    return raw;
  }
}
