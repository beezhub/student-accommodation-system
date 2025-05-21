import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-profile-setup',
  templateUrl: './profile-setup.component.html',
  styleUrls: ['./profile-setup.component.css'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule]
})
export class ProfileSetupComponent implements OnInit {
  profileForm: FormGroup;
  isSubmitting = false;
  currentUser: any;

  constructor(
      private fb: FormBuilder,
      private router: Router,
      private authService: AuthService
  ) {
    this.profileForm = this.fb.group({
      studentNumber: ['', [Validators.required]],
      phoneNumber: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],
      dateOfBirth: ['', [Validators.required]],
      institution: ['', [Validators.required]],
      yearOfStudy: ['', [Validators.required]],
      lengthOfStay: ['', [Validators.required]],
      gender: ['', [Validators.required]],
      specialRequirements: ['']
    });
  }

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/login']);
    }
  }

  onSubmit() {
    if (this.profileForm.valid) {
      this.isSubmitting = true;
      this.isSubmitting = false;
      this.router.navigate(['/document-upload']);
      // this.authService.updateProfile(this.profileForm.value).subscribe({
      //   next: () => {
      //     this.isSubmitting = false;
      //     this.router.navigate(['/document-upload']);
      //   },
      //   error: (error) => {
      //     this.isSubmitting = false;
      //     console.error('Error updating profile:', error);
      //   }
      // });
    } else {
      this.profileForm.markAllAsTouched();
    }
  }
}