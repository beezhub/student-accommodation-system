import {
  Component,
  OnInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';

import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { DocumentViewerComponent } from './document-viewer/document-viewer.component';
import {
  DocumentService,
  isNoActiveApplicationError,
} from '../../../core/services/document.service';
import { ModernStatusBadgeComponent } from '../../../shared/modern-status-badge/modern-status-badge.component';
import { DocumentTypeService } from '../../../core/services/document-type.service';
import { DocumentType } from '../../../core/models/document-type.model';
import { ErrorHandlingService, ParsedError } from '../../../core/services/error-handling.service';
import { ErrorDisplayComponent } from '../../../shared/error-display/error-display.component';
import { ApplicationService } from '../../../core/services/application.service';

// Material imports
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressBarModule } from '@angular/material/progress-bar';

@Component({
  selector: 'app-documents',
  templateUrl: './documents.component.html',
  styleUrls: ['./documents.component.scss'],
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    DocumentViewerComponent,
    // Material modules
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatChipsModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatDividerModule,
    MatProgressBarModule,
    ModernStatusBadgeComponent,
    ErrorDisplayComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentsComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly documentService = inject(DocumentService);
  private readonly documentTypeService = inject(DocumentTypeService);
  private readonly errorService = inject(ErrorHandlingService);
  private readonly applicationService = inject(ApplicationService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  currentUser: any;
  selectedDocument: any = null;
  isUploading = false;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  showUploadModal = false;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  selectedFile: File | null = null;
  uploadForm: FormGroup;

  // Error state
  loadError: ParsedError | null = null;
  uploadError: ParsedError | null = null;
  fileValidationErrors: string[] = []; // Array for detailed file validation errors
  isRetrying = false;

  // Upload progress
  uploadProgress = 0;
  uploadStatus: 'idle' | 'validating' | 'uploading' | 'complete' | 'error' = 'idle';

  documents: any[] = [];

  // Material table configuration
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  readonly displayedColumns: readonly string[] = [
    'name',
    'type',
    'uploadDate',
    'status',
    'size',
    'actions',
  ] as const;
  dataSource = new MatTableDataSource<any>([]);

  // Application check state
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  hasActiveApplication = false;
  isCheckingApplication = true;

  allDocumentTypes: DocumentType[] = [];

  constructor() {
    this.uploadForm = this.fb.group({
      documentType: ['', Validators.required],
    });
  }

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    this.checkActiveApplication();
  }

  /**
   * Checks if user has an active application before allowing document operations.
   * Redirects to application creation if no active application exists.
   */
  private checkActiveApplication(): void {
    this.isCheckingApplication = true;

    this.applicationService.hasActiveApplication().subscribe({
      next: (hasActive) => {
        this.isCheckingApplication = false;
        this.hasActiveApplication = hasActive;

        if (!hasActive) {
          this.errorService.showWarning('Please create an application before uploading documents');
          this.router.navigate(['/applications/new']);
        } else {
          this.loadDocuments();
          this.loadDocumentTypes();
        }
        this.cdr.markForCheck(); // Trigger change detection
      },
      error: () => {
        this.isCheckingApplication = false;
        this.hasActiveApplication = false;
        this.errorService.showWarning(
          'Unable to verify application status. Please create an application first.'
        );
        this.router.navigate(['/applications/new']);
        this.cdr.markForCheck(); // Trigger change detection
      },
    });
  }

  loadDocuments(): void {
    this.loadError = null;
    this.isRetrying = true;

    this.documentService.getUploadedDocuments().subscribe({
      next: (docs) => {
        this.isRetrying = false;
        console.log('Documents received from API:', docs);
        this.documents = docs.map((doc) => ({
          id: String(doc.id),
          name: doc.documentName,
          type: doc.documentTypeName,
          uploadDate: doc.uploadDate,
          status: doc.documentStatus,
          size: doc.fileSize ? this.formatFileSize(doc.fileSize) : 'Unknown',
          fileUrl: doc.fileUrl,
        }));
        console.log('Mapped documents:', this.documents);
        this.dataSource.data = this.documents;
        this.cdr.markForCheck(); // Trigger change detection
      },
      error: (err) => {
        this.isRetrying = false;
        console.error('Failed to fetch documents:', err);
        this.loadError = this.errorService.parseError(err);
        this.cdr.markForCheck(); // Trigger change detection
        this.loadError = this.errorService.parseError(err);
      },
    });
  }

  loadDocumentTypes(): void {
    this.documentTypeService.getAllDocumentTypes().subscribe({
      next: (types: any) => {
        // Handle both array and paginated response formats
        if (Array.isArray(types)) {
          this.allDocumentTypes = types;
        } else if (types && Array.isArray(types.content)) {
          this.allDocumentTypes = types.content;
        } else {
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          this.allDocumentTypes = [];
        }
        console.log('Document types loaded:', this.allDocumentTypes);
        this.cdr.markForCheck(); // Trigger change detection
      },
      error: (err) => {
        console.error('Failed to load document types', err);
        this.allDocumentTypes = [];
        // Show a warning but don't block the page
        this.errorService.showWarning('Failed to load document types. Please try refreshing.');
        this.cdr.markForCheck();
      },
    });
  }

  retryLoadDocuments(): void {
    this.loadDocuments();
    this.loadDocumentTypes();
  }

  getStatusClass(status: string): string {
    switch (status.toLowerCase()) {
      case 'verified':
        return 'status-verified';
      case 'pending':
        return 'status-pending';
      case 'rejected':
        return 'status-rejected';
      default:
        return 'status-default';
    }
  }

  openUploadModal() {
    this.showUploadModal = true;
    this.uploadForm.reset();
    this.selectedFile = null;
    this.uploadError = null;
    this.fileValidationErrors = [];
    this.uploadProgress = 0;
    this.uploadStatus = 'idle';
  }

  closeUploadModal() {
    this.showUploadModal = false;
    this.uploadForm.reset();
    this.selectedFile = null;
    this.uploadError = null;
    this.fileValidationErrors = [];
    this.uploadProgress = 0;
    this.uploadStatus = 'idle';
  }

  dismissUploadError(): void {
    this.uploadError = null;
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.uploadStatus = 'validating';
      this.fileValidationErrors = [];
      this.uploadError = null;

      // Use document service's validation
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const validation = this.documentService.validateFile(file);

      if (!validation.valid) {
        this.fileValidationErrors = validation.errors;
        this.selectedFile = null;
        this.uploadStatus = 'error';
        return;
      }

      this.fileValidationErrors = [];
      this.selectedFile = file;
      this.uploadStatus = 'idle';
    }
  }

  uploadDocument() {
    if (this.uploadForm.valid && this.selectedFile) {
      this.isUploading = true;
      this.uploadError = null;
      this.fileValidationErrors = [];
      this.uploadProgress = 0;
      this.uploadStatus = 'uploading';

      const selectedTypeId: number = this.uploadForm.get('documentType')?.value;
      const fileName = this.selectedFile.name;
      const fileSize = this.selectedFile.size;

      this.documentService
        .uploadSingleDocumentWithProgress(this.selectedFile, selectedTypeId)
        .subscribe({
          next: (progress) => {
            this.uploadProgress = progress.progress;

            if (progress.status === 'complete') {
              this.isUploading = false;
              this.uploadStatus = 'complete';
              this.errorService.showSuccess('Document uploaded successfully!');

              // Optimistically add new doc to the list with basic info
              const typeName =
                this.allDocumentTypes.find((t) => t.id === selectedTypeId)?.name ||
                String(selectedTypeId);
              const newDoc = {
                id: String(Date.now()),
                name: fileName,
                type: typeName,
                uploadDate: new Date().toISOString(),
                status: 'Pending',
                size: this.formatFileSize(fileSize),
              };
              this.documents.unshift(newDoc);
              this.dataSource.data = this.documents;

              // Refresh from server to show accurate data
              this.documentService.getUploadedDocuments().subscribe({
                next: (docs) => {
                  this.documents = docs.map((doc) => ({
                    id: String(doc.id),
                    name: doc.documentName,
                    type: doc.documentTypeName,
                    uploadDate: doc.uploadDate,
                    status: doc.documentStatus,
                    size: doc.fileSize ? this.formatFileSize(doc.fileSize) : 'Unknown',
                    fileUrl: doc.fileUrl,
                  }));
                  this.dataSource.data = this.documents;
                  this.cdr.markForCheck();
                },
                error: (err) => console.error('Failed to refresh documents:', err),
              });

              // Close modal after a brief delay to show completion
              setTimeout(() => this.closeUploadModal(), 500);
            }
          },
          error: (error) => {
            this.isUploading = false;
            this.uploadStatus = 'error';
            this.uploadProgress = 0;
            console.error('Upload error:', error);

            // Check if error is due to no active application
            if (isNoActiveApplicationError(error)) {
              this.closeUploadModal();
              this.errorService.showWarning(
                'Please create an application before uploading documents'
              );
              this.router.navigate(['/applications/new']);
              return;
            }

            this.uploadError = this.errorService.parseError(error);
          },
        });
    }
  }

  retryUpload(): void {
    this.uploadDocument();
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  viewDocument(document: any) {
    // Convert id back to number for the viewer component
    this.selectedDocument = {
      ...document,
      id: Number(document.id),
    };
    console.log('Viewing document:', this.selectedDocument);
  }

  closeViewer() {
    this.selectedDocument = null;
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  deleteDocument(documentId: string) {
    if (confirm('Are you sure you want to delete this document?')) {
      // Here you would typically call your service to delete the document
      this.documents = this.documents.filter((doc) => doc.id !== documentId);
      // keep dataSource in sync
      this.dataSource.data = this.documents;
    }
  }
}
