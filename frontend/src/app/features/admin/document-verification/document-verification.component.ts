import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { AdminService, DocumentResponse, PageResponse } from '../../../core/services/admin.service';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { SelectionModel } from '@angular/cdk/collections';

@Component({
  selector: 'app-document-verification',
  templateUrl: './document-verification.component.html',
  styleUrls: ['./document-verification.component.css'],
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
    MatChipsModule,
    MatCheckboxModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatDividerModule,
  ],
})
export class DocumentVerificationComponent implements OnInit {
  documents: DocumentResponse[] = [];
  displayedColumns: string[] = [
    'select',
    'documentName',
    'documentType',
    'studentName',
    'studentEmail',
    'uploadDate',
    'status',
    'actions',
  ];

  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;

  filterForm: FormGroup;
  selection = new SelectionModel<DocumentResponse>(true, []);

  statuses = ['PENDING', 'APPROVED', 'REJECTED'];

  constructor(
    private adminService: AdminService,
    private fb: FormBuilder,
    private snackBar: MatSnackBar
  ) {
    this.filterForm = this.fb.group({
      studentName: [''],
      documentType: [''],
      status: [''],
    });
  }

  ngOnInit(): void {
    this.loadDocuments();
  }

  loadDocuments(): void {
    this.isLoading = true;
    const filters = {
      ...this.filterForm.value,
      page: this.currentPage,
      size: this.pageSize,
    };

    this.adminService.searchDocuments(filters).subscribe({
      next: (response: PageResponse<DocumentResponse>) => {
        this.documents = response.content;
        this.totalElements = response.totalElements;
        this.isLoading = false;
        this.selection.clear();
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
    this.filterForm.reset();
    this.currentPage = 0;
    this.loadDocuments();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadDocuments();
  }

  updateStatus(document: DocumentResponse, status: string): void {
    this.adminService.updateDocumentStatus(document.id, status).subscribe({
      next: () => {
        this.snackBar.open(`Document ${status.toLowerCase()} successfully`, 'Close', {
          duration: 3000,
        });
        this.loadDocuments();
      },
      error: (error) => {
        console.error('Failed to update status:', error);
        this.snackBar.open('Failed to update document status', 'Close', {
          duration: 3000,
        });
      },
    });
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

  bulkApprove(): void {
    if (this.selection.isEmpty()) {
      this.snackBar.open('Please select documents to approve', 'Close', { duration: 3000 });
      return;
    }

    const selectedIds = this.selection.selected.map(doc => doc.id);
    let completed = 0;
    let failed = 0;

    selectedIds.forEach(id => {
      this.adminService.updateDocumentStatus(id, 'APPROVED').subscribe({
        next: () => {
          completed++;
          if (completed + failed === selectedIds.length) {
            this.finalizeBulkOperation(completed, failed, 'approved');
          }
        },
        error: () => {
          failed++;
          if (completed + failed === selectedIds.length) {
            this.finalizeBulkOperation(completed, failed, 'approved');
          }
        },
      });
    });
  }

  bulkReject(): void {
    if (this.selection.isEmpty()) {
      this.snackBar.open('Please select documents to reject', 'Close', { duration: 3000 });
      return;
    }

    const selectedIds = this.selection.selected.map(doc => doc.id);
    let completed = 0;
    let failed = 0;

    selectedIds.forEach(id => {
      this.adminService.updateDocumentStatus(id, 'REJECTED').subscribe({
        next: () => {
          completed++;
          if (completed + failed === selectedIds.length) {
            this.finalizeBulkOperation(completed, failed, 'rejected');
          }
        },
        error: () => {
          failed++;
          if (completed + failed === selectedIds.length) {
            this.finalizeBulkOperation(completed, failed, 'rejected');
          }
        },
      });
    });
  }

  private finalizeBulkOperation(completed: number, failed: number, action: string): void {
    if (failed === 0) {
      this.snackBar.open(
        `Successfully ${action} ${completed} document(s)`,
        'Close',
        { duration: 3000 }
      );
    } else {
      this.snackBar.open(
        `${completed} document(s) ${action}, ${failed} failed`,
        'Close',
        { duration: 5000 }
      );
    }
    this.loadDocuments();
  }

  isAllSelected(): boolean {
    return this.selection.selected.length === this.documents.length && this.documents.length > 0;
  }

  toggleAllRows(): void {
    if (this.isAllSelected()) {
      this.selection.clear();
    } else {
      this.documents.forEach(row => this.selection.select(row));
    }
  }

  getStatusClass(status: string): string {
    return status.toLowerCase();
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'hourglass_empty';
      case 'APPROVED':
        return 'verified';
      case 'REJECTED':
        return 'error';
      default:
        return 'help';
    }
  }
}
