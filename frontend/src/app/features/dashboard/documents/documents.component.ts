import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { DocumentViewerComponent } from './document-viewer/document-viewer.component';
import { DocumentService } from '../../../core/services/document.service';
import { ModernStatusBadgeComponent } from '../../../shared/modern-status-badge/modern-status-badge.component';

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
    ModernStatusBadgeComponent
  ]
})
export class DocumentsComponent implements OnInit {
  currentUser: any;
  selectedDocument: any = null;
  isUploading = false;
  uploadError: string | null = null;
  showUploadModal = false;
  selectedFile: File | null = null;
  uploadForm: FormGroup;

  documents = [
    {
      id: '1',
      name: 'Student ID',
      type: 'Identity',
      uploadDate: '2024-01-15',
      status: 'Verified',
      size: '1.2 MB'
    },
    {
      id: '2',
      name: 'Proof of Enrollment',
      type: 'Academic',
      uploadDate: '2024-01-15',
      status: 'Pending',
      size: '2.1 MB'
    },
    {
      id: '3',
      name: 'Passport',
      type: 'Identity',
      uploadDate: '2024-01-15',
      status: 'Verified',
      size: '3.5 MB'
    }
  ];

  // Material table configuration
  displayedColumns: string[] = ['name', 'type', 'uploadDate', 'status', 'size', 'actions'];
  dataSource = new MatTableDataSource<any>(this.documents);

  constructor(
    private authService: AuthService,
    private fb: FormBuilder,
    private documentService: DocumentService
  ) {
    this.uploadForm = this.fb.group({
      documentType: ['', Validators.required]
    });
  }

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    this.documentService.getUploadedDocuments().subscribe({
      next: (docs) => {
        this.documents = docs.map(doc => ({
          id: String(doc.id),
          name: doc.documentName,
          type: doc.documentTypeName,
          uploadDate: doc.UploadedDate,
          status: doc.documentStatus,
          size: '' // Size not provided by API, can be updated if available
        }));
        this.dataSource.data = this.documents;
      },
      error: (err) => {
        console.error('Failed to fetch documents:', err);
      }
    });
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
  }

  closeUploadModal() {
    this.showUploadModal = false;
    this.uploadForm.reset();
    this.selectedFile = null;
    this.uploadError = null;
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      if (file.size > 5 * 1024 * 1024) { // 5MB limit
        this.uploadError = 'File size must be less than 5MB';
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
          // Add the new document to the list
          const newDoc = {
            id: response.id,
            name: this.selectedFile!.name,
            type: this.uploadForm.get('documentType')?.value,
            uploadDate: new Date().toISOString(),
            status: 'Pending',
            size: this.formatFileSize(this.selectedFile!.size)
          };
          this.documents.unshift(newDoc);
          // keep dataSource in sync
          this.dataSource.data = this.documents;
          this.closeUploadModal();
        },
        error: (error) => {
          this.isUploading = false;
          this.uploadError = 'Failed to upload document. Please try again.';
          console.error('Upload error:', error);
        }
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

  viewDocument(document: any) {
    this.selectedDocument = document;
  }

  closeViewer() {
    this.selectedDocument = null;
  }

  deleteDocument(documentId: string) {
    if (confirm('Are you sure you want to delete this document?')) {
      // Here you would typically call your service to delete the document
      this.documents = this.documents.filter(doc => doc.id !== documentId);
      // keep dataSource in sync
      this.dataSource.data = this.documents;
    }
  }
}