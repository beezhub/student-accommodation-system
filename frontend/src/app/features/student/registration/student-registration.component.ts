import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

@Component({
  selector: 'app-student-registration',
  templateUrl: './student-registration.component.html',
  styleUrls: ['./student-registration.component.css'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule]
})
export class StudentRegistrationComponent {
  registrationForm: FormGroup;
  isSubmitting = false;
  currentStep = 1;
  totalSteps = 3;

  constructor(
    private fb: FormBuilder,
    private router: Router
  ) {
    this.registrationForm = this.fb.group({
      // Personal Information
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      gender: ['', [Validators.required]],
      dob: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],
      nationality: ['', [Validators.required]],
      
      // Academic Information
      university: ['', [Validators.required]],
      course: ['', [Validators.required]],
      studentId: ['', [Validators.required]],
      yearOfStudy: ['', [Validators.required]],
      
      // Accommodation Preferences
      accommodationType: ['', [Validators.required]],
      moveInDate: ['', [Validators.required]],
      duration: ['', [Validators.required]],
      specialRequirements: [''],
      emergencyContactName: ['', [Validators.required]],
      emergencyContactPhone: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]]
    });
  }

  get stepValid(): boolean {
    if (this.currentStep === 1) {
      return !!this.registrationForm.get('firstName')?.valid && 
         !!this.registrationForm.get('lastName')?.valid &&
         !!this.registrationForm.get('gender')?.valid &&
         !!this.registrationForm.get('dob')?.valid &&
         !!this.registrationForm.get('email')?.valid &&
         !!this.registrationForm.get('phone')?.valid &&
         !!this.registrationForm.get('nationality')?.valid;
    } else if (this.currentStep === 2) {
      return !!this.registrationForm.get('university')?.valid &&
             !!this.registrationForm.get('course')?.valid &&
             !!this.registrationForm.get('studentId')?.valid &&
             !!this.registrationForm.get('yearOfStudy')?.valid;
    } else {
      return !!this.registrationForm.get('accommodationType')?.valid &&
             !!this.registrationForm.get('moveInDate')?.valid &&
             !!this.registrationForm.get('duration')?.valid &&
             !!this.registrationForm.get('emergencyContactName')?.valid &&
             !!this.registrationForm.get('emergencyContactPhone')?.valid;
    }
  }

  nextStep(): void {
    if (this.stepValid && this.currentStep < this.totalSteps) {
      this.currentStep++;
      window.scrollTo(0, 0);
    } else {
      this.markStepAsTouched();
    }
  }

  prevStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
      window.scrollTo(0, 0);
    }
  }

  markStepAsTouched(): void {
    if (this.currentStep === 1) {
      this.registrationForm.get('firstName')?.markAsTouched();
      this.registrationForm.get('lastName')?.markAsTouched();
      this.registrationForm.get('gender')?.markAsTouched();
      this.registrationForm.get('dob')?.markAsTouched();
      this.registrationForm.get('email')?.markAsTouched();
      this.registrationForm.get('phone')?.markAsTouched();
      this.registrationForm.get('nationality')?.markAsTouched();
    } else if (this.currentStep === 2) {
      this.registrationForm.get('university')?.markAsTouched();
      this.registrationForm.get('course')?.markAsTouched();
      this.registrationForm.get('studentId')?.markAsTouched();
      this.registrationForm.get('yearOfStudy')?.markAsTouched();
    } else {
      this.registrationForm.get('accommodationType')?.markAsTouched();
      this.registrationForm.get('moveInDate')?.markAsTouched();
      this.registrationForm.get('duration')?.markAsTouched();
      this.registrationForm.get('emergencyContactName')?.markAsTouched();
      this.registrationForm.get('emergencyContactPhone')?.markAsTouched();
    }
  }

  onSubmit(): void {
    if (this.registrationForm.valid) {
      this.isSubmitting = true;
      // Here you would typically submit to a backend service
      setTimeout(() => {
        this.isSubmitting = false;
        // Show a success message or redirect
        alert('Student registered successfully!');
        this.router.navigate(['/login']);
      }, 1500);
    } else {
      this.registrationForm.markAllAsTouched();
    }
  }
}