import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
  DocumentTypeService,
  DocumentTypeResponse,
  PageResponse,
} from '../../../core/services/document-type.service';
import { RoleService } from '../../../core/services/role.service';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatChipsModule } from '@angular/material/chips';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

// Modern UI Components
import { ModernStatusBadgeComponent } from '../../../shared/modern-status-badge/modern-status-badge.component';
import { DecimalPipe } from '@angular/common';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { confirmAction } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-document-type-management',
  templateUrl: './document-type-management.component.html',
  styleUrls: ['./document-type-management.component.scss'],
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
    MatDialogModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatMenuModule,
    MatChipsModule,
    MatSlideToggleModule,
    ModernStatusBadgeComponent,
    EmptyStateComponent,
    SkeletonLoaderComponent,
    DecimalPipe,
  ],
})
export class DocumentTypeManagementComponent implements OnInit {
  private documentTypeService = inject(DocumentTypeService);
  private roleService = inject(RoleService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  get isTenantAdmin(): boolean {
    return this.roleService.isTenantAdmin();
  }

  /** Returns false for platform-default types when the user is TENANT_ADMIN. */
  canMutate(docType: DocumentTypeResponse): boolean {
    if (this.isTenantAdmin && docType.isPlatformDefault) return false;
    return true;
  }

  documentTypes: DocumentTypeResponse[] = [];
  displayedColumns: string[] = ['typeName', 'description', 'isRequired', 'createdAt', 'actions'];

  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;
  filtersVisible = false;

  filterForm: FormGroup;

  get activeFilterCount(): number {
    const v = this.filterForm.value;
    return [v.name, v.isRequired !== '' ? v.isRequired : null].filter(
      (x: unknown) => x !== null && x !== '' && x !== undefined
    ).length;
  }

  toggleFilters(): void {
    this.filtersVisible = !this.filtersVisible;
  }

  constructor() {
    this.filterForm = this.fb.group({
      name: [''],
      isRequired: [''],
    });
  }

  ngOnInit(): void {
    this.loadDocumentTypes();
  }

  loadDocumentTypes(): void {
    this.isLoading = true;
    const filters = {
      ...this.filterForm.value,
      page: this.currentPage,
      size: this.pageSize,
    };

    this.documentTypeService.searchDocumentTypes(filters, true).subscribe({
      next: (response: PageResponse<DocumentTypeResponse>) => {
        this.documentTypes = response.content;
        this.totalElements = response.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load document types:', error);
        this.snackBar.open('Failed to load document types', 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  onFilter(): void {
    this.currentPage = 0;
    this.loadDocumentTypes();
  }

  onClearFilters(): void {
    this.filterForm.reset();
    this.currentPage = 0;
    this.loadDocumentTypes();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadDocumentTypes();
  }

  openCreateDialog(): void {
    import('./document-type-form-dialog/document-type-form-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.DocumentTypeFormDialogComponent, {
        width: '600px',
        data: { mode: 'create' },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.loadDocumentTypes();
        }
      });
    });
  }

  openEditDialog(documentType: DocumentTypeResponse): void {
    import('./document-type-form-dialog/document-type-form-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.DocumentTypeFormDialogComponent, {
        width: '600px',
        data: { mode: 'edit', documentType },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.loadDocumentTypes();
        }
      });
    });
  }

  /** Opens the create dialog pre-filled with the platform type's name so TENANT_ADMIN can create a shadow. */
  openShadowDialog(docType: DocumentTypeResponse): void {
    import('./document-type-form-dialog/document-type-form-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.DocumentTypeFormDialogComponent, {
        width: '600px',
        data: { mode: 'create', prefillName: docType.typeName, shadow: true },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.loadDocumentTypes();
        }
      });
    });
  }

  toggleRequiredFlag(documentType: DocumentTypeResponse): void {
    const newValue = !documentType.isRequired;
    this.documentTypeService.updateRequiredFlag(documentType.id, newValue).subscribe({
      next: () => {
        const status = newValue ? 'required' : 'optional';
        this.snackBar.open(`Document type marked as ${status}`, 'Close', { duration: 3000 });
        this.loadDocumentTypes();
      },
      error: (error) => {
        console.error('Failed to update required flag:', error);
        this.snackBar.open('Failed to update required flag', 'Close', { duration: 3000 });
      },
    });
  }

  deleteDocumentType(documentType: DocumentTypeResponse): void {
    confirmAction(this.dialog, {
      title: 'Delete this document type?',
      message: `"${documentType.typeName}" is removed permanently. This cannot be undone.`,
      confirmLabel: 'Delete',
      destructive: true,
    }).subscribe((confirmed) => {
      if (!confirmed) return;
      this.documentTypeService.deleteDocumentType(documentType.id).subscribe({
        next: () => {
          this.snackBar.open('Document type deleted successfully', 'Close', { duration: 3000 });
          this.loadDocumentTypes();
        },
        error: (error) => {
          console.error('Failed to delete document type:', error);
          this.snackBar.open(
            'Failed to delete document type. It may be associated with existing documents.',
            'Close',
            { duration: 5000 }
          );
        },
      });
    });
  }

  getRequiredBadgeStatus(isRequired: boolean): string {
    return isRequired ? 'REQUIRED' : 'OPTIONAL';
  }
}
