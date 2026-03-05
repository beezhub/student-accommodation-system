import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import {
  AccommodationTypeService,
  AccommodationType,
  PageResponse,
} from '../../../core/services/accommodation-type.service';

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
import { MatChipsModule } from '@angular/material/chips';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';

@Component({
  selector: 'app-accommodation-type-management',
  templateUrl: './accommodation-type-management.component.html',
  styleUrls: ['./accommodation-type-management.component.scss'],
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
    MatDialogModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatMenuModule,
    MatChipsModule,
    EmptyStateComponent,
  ],
})
export class AccommodationTypeManagementComponent implements OnInit {
  private accommodationTypeService = inject(AccommodationTypeService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  accommodationTypes: AccommodationType[] = [];
  displayedColumns: string[] = ['name', 'description', 'isActive', 'actions'];

  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;

  filterForm: FormGroup;

  constructor() {
    this.filterForm = this.fb.group({
      name: [''],
    });
  }

  ngOnInit(): void {
    this.loadAccommodationTypes();
  }

  loadAccommodationTypes(): void {
    this.isLoading = true;

    this.accommodationTypeService.getAll(this.currentPage, this.pageSize).subscribe({
      next: (response: PageResponse<AccommodationType>) => {
        this.accommodationTypes = response.content;
        this.totalElements = response.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load accommodation types:', error);
        this.snackBar.open('Failed to load accommodation types', 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  onFilter(): void {
    this.currentPage = 0;
    this.loadAccommodationTypes();
  }

  onClearFilters(): void {
    this.filterForm.reset();
    this.currentPage = 0;
    this.loadAccommodationTypes();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadAccommodationTypes();
  }

  openCreateDialog(): void {
    import('./accommodation-type-form-dialog/accommodation-type-form-dialog.component').then(
      (m) => {
        const dialogRef = this.dialog.open(m.AccommodationTypeFormDialogComponent, {
          width: '500px',
          data: { mode: 'create' },
        });

        dialogRef.afterClosed().subscribe((result) => {
          if (result) {
            this.loadAccommodationTypes();
          }
        });
      }
    );
  }

  openEditDialog(accommodationType: AccommodationType): void {
    import('./accommodation-type-form-dialog/accommodation-type-form-dialog.component').then(
      (m) => {
        const dialogRef = this.dialog.open(m.AccommodationTypeFormDialogComponent, {
          width: '500px',
          data: { mode: 'edit', accommodationType },
        });

        dialogRef.afterClosed().subscribe((result) => {
          if (result) {
            this.loadAccommodationTypes();
          }
        });
      }
    );
  }

  deleteAccommodationType(accommodationType: AccommodationType): void {
    if (
      confirm(
        `Are you sure you want to delete accommodation type "${accommodationType.name}"? This action cannot be undone.`
      )
    ) {
      this.accommodationTypeService.delete(accommodationType.id).subscribe({
        next: () => {
          this.snackBar.open('Accommodation type deleted successfully', 'Close', {
            duration: 3000,
          });
          this.loadAccommodationTypes();
        },
        error: (error) => {
          console.error('Failed to delete accommodation type:', error);
          this.snackBar.open(
            'Failed to delete accommodation type. It may be in use by existing applications.',
            'Close',
            { duration: 5000 }
          );
        },
      });
    }
  }
}
