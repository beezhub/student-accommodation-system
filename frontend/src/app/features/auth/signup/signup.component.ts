import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router, RouterModule} from '@angular/router';
import {AuthService} from '../../../core/services/auth.service';

@Component({
    selector: 'app-signup',
    templateUrl: './signup.component.html',
    styleUrls: ['./signup.component.css'],
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterModule],
    providers: [AuthService]
})

export class SignupComponent {
    signupForm: FormGroup;
    isSubmitting = false;
    errorMessage: string = '';

    constructor(
        private fb: FormBuilder,
        private router: Router,
        private authService: AuthService
    ) {
        this.signupForm = this.fb.group({
            firstName: ['', [Validators.required]],
            lastName: ['', [Validators.required]],
            email: ['', [Validators.required, Validators.email]],
            password: ['', [Validators.required, Validators.minLength(8)]],
            confirmPassword: ['', [Validators.required]],
            agreeTerms: [false, [Validators.requiredTrue]]
        }, {
            validators: this.passwordMatchValidator
        });
    }

    passwordMatchValidator(g: FormGroup) {
        const password = g.get('password')?.value;
        const confirmPassword = g.get('confirmPassword')?.value;

        if (password === confirmPassword) {
            return null;
        }

        g.get('confirmPassword')?.setErrors({'mismatch': true});
        return {'mismatch': true};
    }

    onSubmit(): void {
        if (this.signupForm.valid) {
            this.isSubmitting = true;
            this.errorMessage = '';

            const signupData = {
                firstName: this.signupForm.get('firstName')?.value,
                lastName: this.signupForm.get('lastName')?.value,
                email: this.signupForm.get('email')?.value,
                password: this.signupForm.get('password')?.value,
                userRole: 'Student'
            };

            this.authService.signup(signupData).subscribe({
                next: () => {
                    this.isSubmitting = false;
                    this.router.navigate(['/login']);
                },
                error: (error: { error: { message: string; }; }) => {
                    this.isSubmitting = false;
                    this.errorMessage = error.error.message || 'An error occurred during signup';
                }
            });
        } else {
            this.signupForm.markAllAsTouched();
        }
    }
}