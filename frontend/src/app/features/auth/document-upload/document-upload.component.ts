import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-document-upload',
  templateUrl: './document-upload.component.html',
  styleUrls: ['./document-upload.component.css'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule]
})
export class DocumentUploadComponent implements OnInit {
  uploadForm: FormGroup;
  isSubmitting = false;
  currentUser: any;
  uploadedFiles: { [key: string]: File } = {};

  requiredDocuments = [
    { id: 'studentId', name: 'Student ID', description: 'A clear photo or scan of your student ID card' },
    { id: 'proofOfEnrollment', name: 'Proof of Enrollment', description: 'Current enrollment letter or registration document' },
    { id: 'passport', name: 'Passport/ID', description: 'Valid government-issued ID or passport' }
  ];

  constructor(
      private fb: FormBuilder,
      private router: Router,
      private authService: AuthService
  ) {
    this.uploadForm = this.fb.group({
      studentId: [null, Validators.required],
      proofOfEnrollment: [null, Validators.required],
      passport: [null, Validators.required]
    });
  }

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/login']);
    }
  }

  onFileSelected(event: any, documentId: string) {
    if (event.target.files && event.target.files[0]) {
      const file = event.target.files[0];
      this.uploadedFiles[documentId] = file;
      this.uploadForm.patchValue({ [documentId]: file });
    }
  }

  onSubmit() {
    if (this.uploadForm.valid) {
      this.isSubmitting = true;
      const formData = new FormData();

      Object.keys(this.uploadedFiles).forEach(key => {
        formData.append(key, this.uploadedFiles[key]);
      });
      this.isSubmitting = false;
      this.router.navigate(['/application-review']);
      // this.authService.uploadDocuments(formData).subscribe({
      //   next: () => {
      //     this.isSubmitting = false;
      //     this.router.navigate(['/application-review']);
      //   },
      //   error: (error) => {
      //     this.isSubmitting = false;
      //     console.error('Error uploading documents:', error);
      //   }
      // });
    } else {
      this.uploadForm.markAllAsTouched();
    }
  }
}