import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { AcademicYearService } from '../../../core/services/academic-year.service';
import { AcademicYear } from '../../../core/models/academic-year.model';
import { RoleService } from '../../../core/services/role.service';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { confirmAction } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-academic-year-management',
  templateUrl: './academic-year-management.component.html',
  styleUrls: ['./academic-year-management.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTooltipModule,
    EmptyStateComponent,
    SkeletonLoaderComponent,
  ],
})
export class AcademicYearManagementComponent implements OnInit {
  private academicYearService = inject(AcademicYearService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private roleService = inject(RoleService);

  /** The calendar is platform-wide: the backend refuses writes from a company admin (GH-439). */
  readonly canManage = this.roleService.isPlatformAdmin() || this.roleService.isPlatformStaff();

  academicYears: AcademicYear[] = [];
  displayedColumns: string[] = [
    'name',
    'dates',
    'applicationWindow',
    'status',
    ...(this.canManage ? ['actions'] : []),
  ];
  isLoading = false;
  filterForm: FormGroup;

  constructor() {
    this.filterForm = this.fb.group({
      name: [''],
    });
  }

  ngOnInit(): void {
    this.loadAcademicYears();
  }

  loadAcademicYears(): void {
    this.isLoading = true;
    this.academicYearService.getAcademicYears(true).subscribe({
      next: (years: AcademicYear[]) => {
        this.academicYears = years;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load academic years:', error);
        this.snackBar.open('Failed to load academic years', 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  openCreateDialog(): void {
    import('./academic-year-form-dialog/academic-year-form-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.AcademicYearFormDialogComponent, {
        width: '600px',
        data: { mode: 'create' },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.loadAcademicYears();
        }
      });
    });
  }

  openEditDialog(academicYear: AcademicYear): void {
    import('./academic-year-form-dialog/academic-year-form-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.AcademicYearFormDialogComponent, {
        width: '600px',
        data: { mode: 'edit', academicYear },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.loadAcademicYears();
        }
      });
    });
  }

  deleteAcademicYear(academicYear: AcademicYear): void {
    confirmAction(this.dialog, {
      title: 'Delete this academic year?',
      message: `"${academicYear.name}" is removed permanently. This cannot be undone.`,
      confirmLabel: 'Delete',
      destructive: true,
    }).subscribe((confirmed) => {
      if (!confirmed) return;
      this.academicYearService.deleteAcademicYear(academicYear.id).subscribe({
        next: () => {
          this.snackBar.open('Academic year deleted successfully', 'Close', { duration: 3000 });
          this.loadAcademicYears();
        },
        error: (error) => {
          console.error('Failed to delete academic year:', error);
          this.snackBar.open(
            'Failed to delete academic year. It may have associated applications.',
            'Close',
            { duration: 5000 }
          );
        },
      });
    });
  }

  setAsCurrent(academicYear: AcademicYear): void {
    if (academicYear.isCurrent) {
      return;
    }

    this.academicYearService.setCurrentAcademicYear(academicYear.id).subscribe({
      next: () => {
        this.snackBar.open(`${academicYear.name} is now the current academic year`, 'Close', {
          duration: 3000,
        });
        this.loadAcademicYears();
      },
      error: (error) => {
        console.error('Failed to set current academic year:', error);
        this.snackBar.open('Failed to set current academic year', 'Close', { duration: 3000 });
      },
    });
  }

  formatDate(dateString: string): string {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString('en-ZA', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });
  }
}
