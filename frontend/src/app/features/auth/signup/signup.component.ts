import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { UserService } from '../../../service/user.service';
import { Injectable } from '@angular/core';  
import { User } from '../../../model/user.model';

@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  providers: [UserService] 
})

export class SignupComponent {
  signupForm: FormGroup;
  isSubmitting = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private userService: UserService
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
    
    g.get('confirmPassword')?.setErrors({ 'mismatch': true });
    return { 'mismatch': true };
  }

  onSubmit(): void {
    if (this.signupForm.invalid) {
      return;
    }

    const { confirmPassword, ...user } = this.signupForm.value; // Exclude confirmPassword
    this.isSubmitting = true;

    this.userService.register(user).subscribe({
      next: (response) => {
        console.log('User registered:', response);
        this.isSubmitting = false;
        this.router.navigate(['/login']); // Navigate to login page
      },
      error: (error) => {
        this.isSubmitting = false;
        console.error('Error during registration:', error);
        // Handle error (e.g., show a message to the user)
      },
    })
  }
}