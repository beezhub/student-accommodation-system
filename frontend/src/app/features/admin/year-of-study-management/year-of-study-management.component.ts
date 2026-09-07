import { Component, OnInit, inject } from '@angular/core';

import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
  YearOfStudyService,
  YearOfStudyResponse,
  PageResponse,
} from '../../../core/services/year-of-study.service';

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
import { DecimalPipe } from '@angular/common';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { confirmAction } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-year-of-study-management',
  templateUrl: './year-of-study-management.component.html',
  styleUrls: ['./year-of-study-management.component.scss'],
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
    EmptyStateComponent,
    SkeletonLoaderComponent,
    DecimalPipe,
  ],
})
export class YearOfStudyManagementComponent implements OnInit {
  private yearOfStudyService = inject(YearOfStudyService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  yearsOfStudy: YearOfStudyResponse[] = [];
  displayedColumns: string[] = ['yearOfStudy', 'actions'];

  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;
  filtersVisible = false;

  filterForm: FormGroup;

  get activeFilterCount(): number {
    const v = this.filterForm.value;
    return [v.year].filter((x: string) => x && x.trim() !== '').length;
  }

  toggleFilters(): void {
    this.filtersVisible = !this.filtersVisible;
  }

  constructor() {
    this.filterForm = this.fb.group({
      year: [''],
    });
  }

  ngOnInit(): void {
    this.loadYearsOfStudy();
  }

  loadYearsOfStudy(): void {
    this.isLoading = true;
    const filters = {
      ...this.filterForm.value,
      page: this.currentPage,
      size: this.pageSize,
    };

    this.yearOfStudyService.searchYearsOfStudy(filters).subscribe({
      next: (response: PageResponse<YearOfStudyResponse>) => {
        this.yearsOfStudy = response.content;
        this.totalElements = response.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load years of study:', error);
        this.snackBar.open('Failed to load years of study', 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  onFilter(): void {
    this.currentPage = 0;
    this.loadYearsOfStudy();
  }

  onClearFilters(): void {
    this.filterForm.reset();
    this.currentPage = 0;
    this.loadYearsOfStudy();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadYearsOfStudy();
  }

  openCreateDialog(): void {
    import('./year-of-study-form-dialog/year-of-study-form-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.YearOfStudyFormDialogComponent, {
        width: '500px',
        data: { mode: 'create' },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.loadYearsOfStudy();
          this.yearOfStudyService.clearCache();
        }
      });
    });
  }

  openEditDialog(yearOfStudy: YearOfStudyResponse): void {
    import('./year-of-study-form-dialog/year-of-study-form-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.YearOfStudyFormDialogComponent, {
        width: '500px',
        data: { mode: 'edit', yearOfStudy },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.loadYearsOfStudy();
          this.yearOfStudyService.clearCache();
        }
      });
    });
  }

  deleteYearOfStudy(yearOfStudy: YearOfStudyResponse): void {
    confirmAction(this.dialog, {
      title: 'Delete this year of study?',
      message: `"${yearOfStudy.yearOfStudy}" is removed permanently. This cannot be undone.`,
      confirmLabel: 'Delete',
      destructive: true,
    }).subscribe((confirmed) => {
      if (!confirmed) return;
      this.yearOfStudyService.deleteYearOfStudy(yearOfStudy.id).subscribe({
        next: () => {
          this.snackBar.open('Year of study deleted successfully', 'Close', { duration: 3000 });
          this.loadYearsOfStudy();
          this.yearOfStudyService.clearCache();
        },
        error: (error) => {
          console.error('Failed to delete year of study:', error);
          this.snackBar.open(
            'Failed to delete year of study. It may be associated with existing students.',
            'Close',
            { duration: 5000 }
          );
        },
      });
    });
  }

  getYearNumber(index: number): string {
    const num = this.currentPage * this.pageSize + index + 1;
    return num.toString().padStart(2, '0');
  }
}
