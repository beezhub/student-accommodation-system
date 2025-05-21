import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule]
})
export class ProfileComponent implements OnInit {
  currentUser: any;
  profileForm: FormGroup;
  isEditing = false;
  isSaving = false;

  constructor(
      private authService: AuthService,
      private fb: FormBuilder
  ) {
    this.profileForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],
      studentNumber: ['', Validators.required],
      institution: ['', Validators.required],
      yearOfStudy: ['', Validators.required],
      dateOfBirth: ['', Validators.required]
    });
  }

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (this.currentUser) {
      this.profileForm.patchValue({
        firstName: this.currentUser.firstName,
        lastName: this.currentUser.lastName,
        email: this.currentUser.email,
        phone: this.currentUser.phone || '',
        studentNumber: this.currentUser.studentNumber || '',
        institution: this.currentUser.institution || '',
        yearOfStudy: this.currentUser.yearOfStudy || '',
        dateOfBirth: this.currentUser.dateOfBirth || ''
      });
    }
  }

  toggleEdit() {
    this.isEditing = !this.isEditing;
    if (!this.isEditing) {
      this.profileForm.patchValue(this.currentUser);
    }
  }

  onSubmit() {
    if (this.profileForm.valid) {
      this.isSaving = true;
      // Here you would typically update the profile through your service
      setTimeout(() => {
        this.isSaving = false;
        this.isEditing = false;
        // Update the current user with the new values
        this.currentUser = { ...this.currentUser, ...this.profileForm.value };
      }, 1000);
    }
  }
}