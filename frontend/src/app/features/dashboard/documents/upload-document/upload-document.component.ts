import { Component, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-upload-document',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatIconModule,
    MatDividerModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './upload-document.component.html',
  styleUrls: ['./upload-document.component.css'],
})
export class UploadDocumentComponent {
  @Output() close = new EventEmitter<void>();
  uploadForm: FormGroup;
  selectedFile: File | null = null;
  uploadError: string | null = null;
  isUploading = false;
  documents: any[] = [];
  documentTypes = [
    { value: 'student-id', viewValue: 'Student ID' },
    { value: 'passport', viewValue: 'Passport' },
    { value: 'drivers-license', viewValue: "Driver's License" },
    { value: 'enrollment-proof', viewValue: 'Proof of Enrollment' },
    { value: 'academic-transcript', viewValue: 'Academic Transcript' },
    { value: 'financial-statement', viewValue: 'Financial Statement' },
    { value: 'medical-record', viewValue: 'Medical Record' },
    { value: 'other', viewValue: 'Other' },
  ];

  constructor(private fb: FormBuilder, private authService: AuthService) {
    this.uploadForm = this.fb.group({
      documentType: ['', Validators.required],
    });
  }

  closeUploadModal() {
    this.uploadForm.reset();
    this.selectedFile = null;
    this.uploadError = null;
    this.close.emit();
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      if (file.size > 5 * 1024 * 1024) {
        this.uploadError = 'File size exceeds 5MB limit';
        this.selectedFile = null;
        return;
      }
      const allowedTypes = ['application/pdf', 'image/jpeg', 'image/png'];
      if (!allowedTypes.includes(file.type)) {
        this.uploadError = 'Only PDF, JPG, and PNG files are allowed';
        this.selectedFile = null;
        return;
      }
      this.uploadError = null;
      this.selectedFile = file;
    }
  }

  uploadDocument() {
    if (this.uploadForm.valid && this.selectedFile) {
      this.isUploading = true;
      this.uploadError = null;
      const formData = new FormData();
      formData.append('file', this.selectedFile);
      formData.append('type', this.uploadForm.get('documentType')?.value);

      this.authService.uploadDocuments(formData).subscribe({
        next: (response) => {
          this.isUploading = false;
          this.documents.unshift({
            id: response.id,
            name: this.selectedFile!.name,
            type: this.uploadForm.get('documentType')?.value,
            uploadDate: new Date().toISOString(),
            status: 'Pending',
            size: this.formatFileSize(this.selectedFile!.size),
          });
          this.closeUploadModal();
        },
        error: (error) => {
          this.isUploading = false;
          this.uploadError = 'Failed to upload document. Please try again.';
          console.error('Upload error:', error);
        },
      });
    }
  }

  private formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }
}
