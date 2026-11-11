import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
  inject,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { AcademicYear } from '../../../core/models/academic-year.model';
import { AcademicYearService } from '../../../core/services/academic-year.service';
import { RoleService } from '../../../core/services/role.service';
import { confirmAction } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { InstitutionGroup, describeYearDates, groupByInstitution } from './academic-year-wording';
import { IntakeListComponent } from './intake-list/intake-list.component';
import { AcademicYearDialogData } from './academic-year-form-dialog/academic-year-form-dialog.component';

/**
 * A company's academic years, grouped by institution, with the intakes inside each (GH-494).
 * The current year follows the dates, so there is nothing to set (GH-439).
 */
@Component({
  selector: 'app-academic-year-management',
  templateUrl: './academic-year-management.component.html',
  styleUrls: ['./academic-year-management.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatIconModule,
    MatSnackBarModule,
    MatTooltipModule,
    EmptyStateComponent,
    SkeletonLoaderComponent,
    IntakeListComponent,
  ],
})
export class AcademicYearManagementComponent implements OnInit {
  private academicYearService = inject(AcademicYearService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private roleService = inject(RoleService);
  private cdr = inject(ChangeDetectorRef);

  /** Each company manages its own years; platform roles manage any company's (GH-439). */
  readonly canManage =
    this.roleService.isPlatformAdmin() ||
    this.roleService.isPlatformStaff() ||
    this.roleService.isTenantAdmin();
  readonly describeDates = describeYearDates;

  academicYears: AcademicYear[] = [];
  groups: InstitutionGroup[] = [];
  isLoading = false;

  ngOnInit(): void {
    this.loadAcademicYears();
  }

  loadAcademicYears(): void {
    this.isLoading = true;
    this.academicYearService.getAcademicYears(true).subscribe({
      next: (years) => {
        this.academicYears = years;
        this.groups = groupByInstitution(years);
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.snackBar.open('Failed to load academic years', 'Close', { duration: 3000 });
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  openCreateDialog(institutionId: number | null = null): void {
    this.openDialog({ mode: 'pattern', institutionId });
  }

  openEditDialog(academicYear: AcademicYear): void {
    this.openDialog({ mode: 'edit', academicYear });
  }

  /** "Copy last year": the group's newest year, with every date a year later. */
  copyLastYear(group: InstitutionGroup): void {
    const latest = group.years[0];
    if (!latest) {
      return;
    }
    this.academicYearService.copyAcademicYear(latest.id).subscribe({
      next: (copy) => {
        this.snackBar.open(`${copy.name} created from ${latest.name}. Check the dates.`, 'Close', {
          duration: 6000,
        });
        this.loadAcademicYears();
      },
      error: (error: HttpErrorResponse) => this.showError(error, 'The year could not be copied'),
    });
  }

  deleteAcademicYear(academicYear: AcademicYear): void {
    confirmAction(this.dialog, {
      title: `Delete ${academicYear.name}?`,
      message:
        'The year and its intakes are removed. A year with applications cannot be deleted; ' +
        'close its intakes instead.',
      confirmLabel: 'Delete',
      destructive: true,
    }).subscribe((confirmed) => {
      if (!confirmed) return;
      this.academicYearService.deleteAcademicYear(academicYear.id).subscribe({
        next: () => {
          this.snackBar.open(`${academicYear.name} deleted`, 'Close', { duration: 3000 });
          this.loadAcademicYears();
        },
        error: (error: HttpErrorResponse) => this.showError(error, 'The year could not be deleted'),
      });
    });
  }

  private openDialog(data: AcademicYearDialogData): void {
    import('./academic-year-form-dialog/academic-year-form-dialog.component').then((m) => {
      this.dialog
        .open(m.AcademicYearFormDialogComponent, { width: '600px', data })
        .afterClosed()
        .subscribe((saved) => {
          if (saved) {
            this.loadAcademicYears();
          }
        });
    });
  }

  private showError(error: HttpErrorResponse, fallback: string): void {
    this.snackBar.open(error?.error?.message || fallback, 'Close', { duration: 6000 });
  }
}
