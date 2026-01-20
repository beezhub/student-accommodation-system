import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { DocumentViewerComponent } from './document-viewer/document-viewer.component';
import { DocumentService } from '../../../core/services/document.service';
import { ModernStatusBadgeComponent } from '../../../shared/modern-status-badge/modern-status-badge.component';
import { DocumentTypeService } from '../../../core/services/document-type.service';
import { DocumentType } from '../../../core/models/document-type.model';
import { ErrorHandlingService, ParsedError } from '../../../core/services/error-handling.service';
import { ErrorDisplayComponent } from '../../../shared/error-display/error-display.component';

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

@Component({
  selector: 'app-documents',
  templateUrl: './documents.component.html',
  styleUrls: ['./documents.component.css'],
  standalone: true,
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
    ModernStatusBadgeComponent,
    ErrorDisplayComponent,
  ],
})
export class DocumentsComponent implements OnInit {
  currentUser: any;
  selectedDocument: any = null;
  isUploading = false;
  showUploadModal = false;
  selectedFile: File | null = null;
  uploadForm: FormGroup;

  // Error state
  loadError: ParsedError | null = null;
  uploadError: ParsedError | null = null;
  fileError: string | null = null; // Simple string for file validation errors
  isRetrying = false;

  documents = [
    {
      id: '1',
      name: 'Student ID',
      type: 'Identity',
      uploadDate: '2024-01-15',
      status: 'Verified',
      size: '1.2 MB',
    },
    {
      id: '2',
      name: 'Proof of Enrollment',
      type: 'Academic',
      uploadDate: '2024-01-15',
      status: 'Pending',
      size: '2.1 MB',
    },
    {
      id: '3',
      name: 'Passport',
      type: 'Identity',
      uploadDate: '2024-01-15',
      status: 'Verified',
      size: '3.5 MB',
    },
  ];

  // Material table configuration
  displayedColumns: string[] = ['name', 'type', 'uploadDate', 'status', 'size', 'actions'];
  dataSource = new MatTableDataSource<any>(this.documents);

  constructor(
    private authService: AuthService,
    private fb: FormBuilder,
    private documentService: DocumentService,
    private documentTypeService: DocumentTypeService,
    private errorService: ErrorHandlingService
  ) {
    this.uploadForm = this.fb.group({
      documentType: ['', Validators.required],
    });
  }

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    this.loadDocuments();
    this.loadDocumentTypes();
  }

  loadDocuments(): void {
    this.loadError = null;
    this.isRetrying = true;

    this.documentService.getUploadedDocuments().subscribe({
      next: (docs) => {
        this.isRetrying = false;
        this.documents = docs.map((doc) => ({
          id: String(doc.id),
          name: doc.documentName,
          type: doc.documentTypeName,
          uploadDate: doc.UploadedDate,
          status: doc.documentStatus,
          size: '', // Size not provided by API, can be updated if available
        }));
        this.dataSource.data = this.documents;
      },
      error: (err) => {
        this.isRetrying = false;
        console.error('Failed to fetch documents:', err);
        this.loadError = this.errorService.parseError(err);
      },
    });
  }

  loadDocumentTypes(): void {
    this.documentTypeService.getRequiredDocumentTypes().subscribe({
      next: (types: DocumentType[]) => {
        this.requiredDocumentTypes = types;
      },
      error: (err) => {
        console.error('Failed to load document types', err);
        // Show a warning but don't block the page
        this.errorService.showWarning('Failed to load document types. Please try refreshing.');
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
    this.fileError = null;
  }

  closeUploadModal() {
    this.showUploadModal = false;
    this.uploadForm.reset();
    this.selectedFile = null;
    this.uploadError = null;
    this.fileError = null;
  }

  dismissUploadError(): void {
    this.uploadError = null;
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        // 5MB limit
        this.fileError = 'File size must be less than 5MB';
        this.selectedFile = null;
        return;
      }

      const allowedTypes = ['application/pdf', 'image/jpeg', 'image/png'];
      if (!allowedTypes.includes(file.type)) {
        this.fileError = 'Only PDF, JPG, and PNG files are allowed';
        this.selectedFile = null;
        return;
      }

      this.fileError = null;
      this.uploadError = null;
      this.selectedFile = file;
    }
  }

  uploadDocument() {
    if (this.uploadForm.valid && this.selectedFile) {
      this.isUploading = true;
      this.uploadError = null;
      this.fileError = null;

      const selectedTypeId: number = this.uploadForm.get('documentType')?.value;

      this.documentService.uploadSingleDocument(this.selectedFile, selectedTypeId).subscribe({
        next: () => {
          this.isUploading = false;
          this.errorService.showSuccess('Document uploaded successfully!');
          // Optimistically add new doc to the list with basic info
          const typeName =
            this.requiredDocumentTypes.find((t) => t.id === selectedTypeId)?.name ||
            String(selectedTypeId);
          const newDoc = {
            id: String(Date.now()),
            name: this.selectedFile!.name,
            type: typeName,
            uploadDate: new Date().toISOString(),
            status: 'Pending',
            size: this.formatFileSize(this.selectedFile!.size),
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
                uploadDate: doc.UploadedDate,
                status: doc.documentStatus,
                size: '',
              }));
              this.dataSource.data = this.documents;
            },
            error: (err) => console.error('Failed to refresh documents:', err),
          });
          this.closeUploadModal();
        },
        error: (error) => {
          this.isUploading = false;
          console.error('Upload error:', error);
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
    this.selectedDocument = document;
  }

  closeViewer() {
    this.selectedDocument = null;
  }

  deleteDocument(documentId: string) {
    if (confirm('Are you sure you want to delete this document?')) {
      // Here you would typically call your service to delete the document
      this.documents = this.documents.filter((doc) => doc.id !== documentId);
      // keep dataSource in sync
      this.dataSource.data = this.documents;
    }
  }

  requiredDocumentTypes: DocumentType[] = [];
}
