import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AcademicYearService } from '../../../../core/services/academic-year.service';
import { AcademicYear, AcademicYearRequest } from '../../../../core/models/academic-year.model';

export interface AcademicYearDialogData {
  mode: 'create' | 'edit';
  academicYear?: AcademicYear;
}

@Component({
  selector: 'app-academic-year-form-dialog',
  templateUrl: './academic-year-form-dialog.component.html',
  styleUrls: ['./academic-year-form-dialog.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatCheckboxModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
  ],
})
export class AcademicYearFormDialogComponent implements OnInit {
  private fb = inject(FormBuilder);
  private academicYearService = inject(AcademicYearService);
  private dialogRef = inject<MatDialogRef<AcademicYearFormDialogComponent>>(MatDialogRef);
  private snackBar = inject(MatSnackBar);
  data = inject<AcademicYearDialogData>(MAT_DIALOG_DATA);

  form: FormGroup;
  isSubmitting = false;
  isEditMode = false;

  constructor() {
    const data = this.data;

    this.isEditMode = data.mode === 'edit';
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(50)]],
      startDate: [null, Validators.required],
      endDate: [null, Validators.required],
      applicationOpenDate: [null, Validators.required],
      applicationCloseDate: [null, Validators.required],
      isCurrent: [false],
    });
  }

  ngOnInit(): void {
    if (this.isEditMode && this.data.academicYear) {
      const year = this.data.academicYear;
      this.form.patchValue({
        name: year.name,
        startDate: year.startDate ? new Date(year.startDate) : null,
        endDate: year.endDate ? new Date(year.endDate) : null,
        applicationOpenDate: year.applicationOpenDate ? new Date(year.applicationOpenDate) : null,
        applicationCloseDate: year.applicationCloseDate
          ? new Date(year.applicationCloseDate)
          : null,
        isCurrent: year.isCurrent,
      });
    }
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    const formValue = this.form.value;

    const request: AcademicYearRequest = {
      name: formValue.name,
      startDate: this.formatDateForApi(formValue.startDate),
      endDate: this.formatDateForApi(formValue.endDate),
      applicationOpenDate: this.formatDateForApi(formValue.applicationOpenDate),
      applicationCloseDate: this.formatDateForApi(formValue.applicationCloseDate),
      isCurrent: formValue.isCurrent,
    };

    const operation = this.isEditMode
      ? this.academicYearService.updateAcademicYear(this.data.academicYear!.id, request)
      : this.academicYearService.createAcademicYear(request);

    operation.subscribe({
      next: () => {
        const message = this.isEditMode
          ? 'Academic year updated successfully'
          : 'Academic year created successfully';
        this.snackBar.open(message, 'Close', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: (error) => {
        console.error('Failed to save academic year:', error);
        const message = error?.error?.message || 'Failed to save academic year';
        this.snackBar.open(message, 'Close', { duration: 5000 });
        this.isSubmitting = false;
      },
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  private formatDateForApi(date: Date): string {
    if (!date) return '';
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
