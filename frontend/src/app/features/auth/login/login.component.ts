import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service'; // Adjust path as needed

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule]
})
export class LoginComponent {
  loginForm: FormGroup;
  isSubmitting = false;
  errorMessage: string = '';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  passwordMatchValidator(g: FormGroup) {
    const password = g.get('password')?.value;
    const confirmPassword = g.get('confirmPassword')?.value;

    if (password === confirmPassword) {
      return null;
    }

    g.get('confirmPassword')?.setErrors({ 'mismatch': true });
    return { 'mismatch': true };
  }

  onSubmit(): void {
    
    if (this.loginForm.valid) {
      this.isSubmitting = true;
      this.errorMessage = '';
      console.log('Login form submitted:', this.loginForm.value);
      const loginData = {
        email: this.loginForm.get('email')?.value,
        password: this.loginForm.get('password')?.value
      };
      console.log('Login data:', loginData);

      this.authService.login(loginData).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.router.navigate(['/student-registration']);
        },
        error: (error: { error: { message: string; }; }) => {
          this.isSubmitting = false;
          this.errorMessage = error.error.message || 'Login failed. Please try again.';
        }
      });
    } else {
      this.loginForm.markAllAsTouched();
      console.log('Login form is invalid:', this.loginForm.errors);
    }
  }
}