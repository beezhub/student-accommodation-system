import { Component, OnInit } from '@angular/core';

import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
  InstitutionService,
  InstitutionResponse,
  PageResponse,
} from '../../../core/services/institution.service';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';

@Component({
  selector: 'app-institution-management',
  templateUrl: './institution-management.component.html',
  styleUrls: ['./institution-management.component.css'],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatMenuModule,
  ],
})
export class InstitutionManagementComponent implements OnInit {
  institutions: InstitutionResponse[] = [];
  displayedColumns: string[] = ['name', 'address', 'phoneNumber', 'emailAddress', 'actions'];

  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;

  filterForm: FormGroup;

  constructor(
    private institutionService: InstitutionService,
    private fb: FormBuilder,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {
    this.filterForm = this.fb.group({
      name: [''],
      address: [''],
      contactEmail: [''],
    });
  }

  ngOnInit(): void {
    this.loadInstitutions();
  }

  loadInstitutions(): void {
    this.isLoading = true;
    const filters = {
      ...this.filterForm.value,
      page: this.currentPage,
      size: this.pageSize,
    };

    this.institutionService.searchInstitutions(filters).subscribe({
      next: (response: PageResponse<InstitutionResponse>) => {
        this.institutions = response.content;
        this.totalElements = response.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load institutions:', error);
        this.snackBar.open('Failed to load institutions', 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  onFilter(): void {
    this.currentPage = 0;
    this.loadInstitutions();
  }

  onClearFilters(): void {
    this.filterForm.reset();
    this.currentPage = 0;
    this.loadInstitutions();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadInstitutions();
  }

  openCreateDialog(): void {
    import('./institution-form-dialog/institution-form-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.InstitutionFormDialogComponent, {
        width: '600px',
        data: { mode: 'create' },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.loadInstitutions();
        }
      });
    });
  }

  openEditDialog(institution: InstitutionResponse): void {
    import('./institution-form-dialog/institution-form-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.InstitutionFormDialogComponent, {
        width: '600px',
        data: { mode: 'edit', institution },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.loadInstitutions();
        }
      });
    });
  }

  deleteInstitution(institution: InstitutionResponse): void {
    if (
      confirm(
        `Are you sure you want to delete institution "${institution.name}"? This action cannot be undone.`
      )
    ) {
      this.institutionService.deleteInstitution(institution.id).subscribe({
        next: () => {
          this.snackBar.open('Institution deleted successfully', 'Close', { duration: 3000 });
          this.loadInstitutions();
        },
        error: (error) => {
          console.error('Failed to delete institution:', error);
          this.snackBar.open(
            'Failed to delete institution. It may have associated students.',
            'Close',
            { duration: 5000 }
          );
        },
      });
    }
  }
}
