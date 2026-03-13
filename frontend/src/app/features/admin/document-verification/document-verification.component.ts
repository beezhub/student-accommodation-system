import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { AdminService, DocumentResponse, PageResponse } from '../../../core/services/admin.service';
import { DocumentTypeService } from '../../../core/services/document-type.service';
import { DocumentViewerComponent } from '../../dashboard/documents/document-viewer/document-viewer.component';
import { ModernStatusBadgeComponent } from '../../../shared/modern-status-badge/modern-status-badge.component';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-document-verification',
  templateUrl: './document-verification.component.html',
  styleUrls: ['./document-verification.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    DocumentViewerComponent,
    ModernStatusBadgeComponent,
    SkeletonLoaderComponent,
    EmptyStateComponent,
  ],
})
export class DocumentVerificationComponent implements OnInit {
  private adminService = inject(AdminService);
  private documentTypeService = inject(DocumentTypeService);
  private fb = inject(FormBuilder);
  private snackBar = inject(MatSnackBar);

  documents: DocumentResponse[] = [];
  displayedColumns: string[] = ['documentName', 'studentName', 'uploadDate', 'status', 'actions'];

  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;
  filtersVisible = false;

  filterForm: FormGroup;

  get activeFilterCount(): number {
    const v = this.filterForm?.value ?? {};
    return [v.studentName, v.documentTypeId, v.status].filter(Boolean).length;
  }

  toggleFilters(): void {
    this.filtersVisible = !this.filtersVisible;
  }

  statuses = ['PENDING', 'APPROVED', 'REJECTED'];
  documentTypes: any[] = [];

  selectedDocument: any = null;

  constructor() {
    this.filterForm = this.fb.group({
      studentName: [''],
      documentTypeId: [''],
      status: [''],
    });
  }

  ngOnInit(): void {
    this.loadDocumentTypes();
    this.loadDocuments();
  }

  loadDocumentTypes(): void {
    this.documentTypeService.searchDocumentTypes({ page: 0, size: 100 }).subscribe({
      next: (response) => {
        this.documentTypes = response.content;
      },
      error: (err) => {
        console.error('Failed to load document types:', err);
      },
    });
  }

  loadDocuments(): void {
    this.isLoading = true;
    const formValues = this.filterForm.value;
    const filters = {
      studentName: formValues.studentName || undefined,
      documentTypeId: formValues.documentTypeId || undefined,
      status: formValues.status || undefined,
      page: this.currentPage,
      size: this.pageSize,
    };

    this.adminService.searchDocuments(filters).subscribe({
      next: (response: PageResponse<DocumentResponse>) => {
        this.documents = response.content;
        this.totalElements = response.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load documents:', error);
        this.snackBar.open('Failed to load documents', 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  onFilter(): void {
    this.currentPage = 0;
    this.loadDocuments();
  }

  onClearFilters(): void {
    this.filterForm.reset({
      studentName: '',
      documentTypeId: '',
      status: '',
    });
    this.currentPage = 0;
    this.loadDocuments();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadDocuments();
  }

  downloadDocument(doc: DocumentResponse): void {
    this.adminService.downloadDocument(doc.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = window.document.createElement('a');
        link.href = url;
        link.download = doc.documentName;
        link.click();
        window.URL.revokeObjectURL(url);
        this.snackBar.open('Document downloaded', 'Close', { duration: 2000 });
      },
      error: (error) => {
        console.error('Failed to download document:', error);
        this.snackBar.open('Failed to download document', 'Close', { duration: 3000 });
      },
    });
  }

  viewDocument(document: DocumentResponse): void {
    this.selectedDocument = {
      id: document.id,
      name: document.documentName,
      type: document.documentTypeName,
      uploadDate: document.createdAt,
      status: document.documentStatus,
      size: '',
    };
  }

  closeViewer(): void {
    this.selectedDocument = null;
  }
}
