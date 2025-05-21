import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-new-application',
  templateUrl: './new-application.component.html',
  styleUrls: ['./new-application.component.css'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule]
})
export class NewApplicationComponent {
  applicationForm: FormGroup;
  isSubmitting = false;
  currentUser: any;

  constructor(
      private fb: FormBuilder,
      private router: Router,
      private authService: AuthService
  ) {
    this.applicationForm = this.fb.group({
      accommodationType: ['', Validators.required],
      moveInDate: ['', Validators.required],
      duration: ['', Validators.required],
      mealPlan: ['', Validators.required],
      specialRequirements: [''],
      roomPreference: ['', Validators.required],
      budget: ['', [Validators.required, Validators.min(0)]],
      agreeToTerms: [false, Validators.requiredTrue]
    });
  }

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/login']);
    }
  }

  onSubmit() {
    if (this.applicationForm.valid) {
      this.isSubmitting = true;
      const applicationData = {
        ...this.applicationForm.value,
        userId: this.currentUser.id
      };

      this.authService.submitApplication(applicationData).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.router.navigate(['/application-confirmation']);
        },
        error: (error) => {
          this.isSubmitting = false;
          console.error('Error submitting application:', error);
        }
      });
    } else {
      this.applicationForm.markAllAsTouched();
    }
  }
}