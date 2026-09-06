import {
  Component,
  OnInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  inject,
} from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../../core/services/auth.service';
import { DocumentTypeService } from '../../../core/services/document-type.service';
import { DocumentType } from '../../../core/models/document-type.model';
import { DocumentService } from '../../../core/services/document.service';
import { ErrorHandlingService, ParsedError } from '../../../core/services/error-handling.service';
import { ErrorDisplayComponent } from '../../../shared/error-display/error-display.component';
import { User } from '../../../core/models/user.model';

@Component({
  selector: 'app-document-upload',
  templateUrl: './document-upload.component.html',
  styleUrls: ['./document-upload.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatProgressBarModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatDividerModule,
    ErrorDisplayComponent,
  ],
})
export class DocumentUploadComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly authService = inject(AuthService);
  private readonly documentTypeService = inject(DocumentTypeService);
  private readonly documentService = inject(DocumentService);
  private readonly errorService = inject(ErrorHandlingService);

  uploadForm: FormGroup;
  isSubmitting = false;
  currentUser: User | null = null;
  uploadedFiles: { [key: string]: File } = {};

  requiredDocuments: DocumentType[] = [];

  // Error state
  loadError: ParsedError | null = null;
  submitError: ParsedError | null = null;
  fileValidationErrors: { [key: string]: string[] } = {};
  isLoadingTypes = false;

  // Upload progress
  uploadProgress = 0;
  uploadStatus: 'idle' | 'uploading' | 'complete' | 'error' = 'idle';

  constructor() {
    this.uploadForm = this.fb.group({});
  }

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/login']);
      return;
    }
    this.loadDocumentTypes();
  }

  loadDocumentTypes(): void {
    this.isLoadingTypes = true;
    this.loadError = null;
    this.cdr.markForCheck();

    this.documentTypeService.getRequiredDocumentTypes().subscribe({
      next: (documentTypes: DocumentType[]) => {
        this.isLoadingTypes = false;
        this.requiredDocuments = documentTypes;
        documentTypes.forEach((doc) => {
          // Control names are strings; doc.id is a number. `any[]` let the number through, and
          // `contains` then never matched, so a control was added on every load (GH-158).
          const controlName = String(doc.id);
          if (!this.uploadForm.contains(controlName)) {
            this.uploadForm.addControl(controlName, this.fb.control(null, Validators.required));
          }
        });
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.isLoadingTypes = false;
        console.error('Failed to load required document types', err);
        this.loadError = this.errorService.parseError(err);
        this.cdr.markForCheck();
      },
    });
  }

  retryLoadTypes(): void {
    this.loadDocumentTypes();
  }

  onFileSelected(event: Event, documentId: string) {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0];
    if (file) {
      // Validate file
      const validation = this.documentService.validateFile(file);

      if (!validation.valid) {
        this.fileValidationErrors[documentId] = validation.errors;
        // Clear the file from form if validation fails
        this.uploadForm.patchValue({ [documentId]: null });
        delete this.uploadedFiles[documentId];
        this.cdr.markForCheck();
        return;
      }

      // Clear any previous validation errors for this document
      delete this.fileValidationErrors[documentId];
      this.uploadedFiles[documentId] = file;
      this.uploadForm.patchValue({ [documentId]: file });
      this.cdr.markForCheck();
    }
  }

  getFileValidationErrors(documentId: string): string[] {
    return this.fileValidationErrors[documentId] || [];
  }

  hasFileValidationError(documentId: string): boolean {
    return this.fileValidationErrors[documentId]?.length > 0;
  }

  formatFileSize(bytes: number): string {
    return this.documentService.formatFileSize(bytes);
  }

  onSubmit() {
    // Clear previous errors
    this.submitError = null;

    // Check if there are any file validation errors
    if (Object.keys(this.fileValidationErrors).length > 0) {
      this.errorService.showWarning('Please fix file validation errors before uploading');
      return;
    }

    if (this.uploadForm.valid) {
      this.isSubmitting = true;
      this.uploadProgress = 0;
      this.uploadStatus = 'uploading';

      const files: File[] = [];
      const documentTypes: number[] = [];

      Object.keys(this.uploadedFiles).forEach((key) => {
        files.push(this.uploadedFiles[key]);
        documentTypes.push(Number(key));
      });

      this.documentService.uploadMultipleDocumentsWithProgress(files, documentTypes).subscribe({
        next: (progress) => {
          this.uploadProgress = progress.progress;

          if (progress.status === 'complete') {
            this.isSubmitting = false;
            this.uploadStatus = 'complete';
            this.errorService.showSuccess('All documents uploaded successfully!');
            this.cdr.markForCheck();

            // Navigate after a brief delay to show completion
            setTimeout(() => {
              this.router.navigate(['/application-submission']);
            }, 500);
          }
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Upload error', err);
          this.isSubmitting = false;
          this.uploadStatus = 'error';
          this.uploadProgress = 0;

          this.submitError = this.errorService.parseError(err);
          this.cdr.markForCheck();
        },
      });
    } else {
      this.uploadForm.markAllAsTouched();
      this.errorService.showWarning('Please upload all required documents');
    }
  }

  dismissError(): void {
    this.submitError = null;
    this.cdr.markForCheck();
  }

  retrySubmit(): void {
    this.onSubmit();
  }

  skipUpload(): void {
    this.errorService.showWarning(
      'You can upload documents later from your dashboard before submitting your application.'
    );
    this.router.navigate(['/dashboard']);
  }

  get uploadedCount(): number {
    return Object.keys(this.uploadedFiles).filter((key) => !this.hasFileValidationError(key))
      .length;
  }
}
