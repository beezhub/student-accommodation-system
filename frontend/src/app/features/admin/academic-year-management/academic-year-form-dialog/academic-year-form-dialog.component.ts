import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { Observable } from 'rxjs';

import { AcademicYearService } from '../../../../core/services/academic-year.service';
import { InstitutionService } from '../../../../core/services/institution.service';
import { AcademicYear, IntakeType } from '../../../../core/models/academic-year.model';
import { Institution } from '../../../../core/models/institution.model';
import { ALL_INSTITUTIONS, describeAffected } from '../academic-year-wording';

/**
 * 'pattern' creates a year with its intakes filled in (GH-494); 'edit' changes a year's name,
 * institution and dates. `institutionId` preselects the institution for a new year.
 */
export interface AcademicYearDialogData {
  mode: 'pattern' | 'edit';
  academicYear?: AcademicYear;
  institutionId?: number | null;
}

@Component({
  selector: 'app-academic-year-form-dialog',
  templateUrl: './academic-year-form-dialog.component.html',
  styleUrls: ['./academic-year-form-dialog.component.css'],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCheckboxModule,
    MatSelectModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatIconModule,
  ],
})
export class AcademicYearFormDialogComponent implements OnInit {
  private fb = inject(FormBuilder);
  private academicYearService = inject(AcademicYearService);
  private institutionService = inject(InstitutionService);
  private dialogRef = inject<MatDialogRef<AcademicYearFormDialogComponent>>(MatDialogRef);
  private snackBar = inject(MatSnackBar);
  data = inject<AcademicYearDialogData>(MAT_DIALOG_DATA);

  readonly allInstitutions = ALL_INSTITUTIONS;
  readonly isEditMode = this.data.mode === 'edit';
  institutions: Institution[] = [];
  isSubmitting = false;

  form = this.fb.nonNullable.group({
    institutionId: [null as number | null],
    name: ['', [Validators.required, Validators.maxLength(20)]],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    annual: [true],
    semesters: [false],
    trimesters: [false],
  });

  ngOnInit(): void {
    this.institutionService.getInstitutions().subscribe({
      next: (institutions) => (this.institutions = institutions),
      error: () => (this.institutions = []),
    });
    const year = this.data.academicYear;
    if (this.isEditMode && year) {
      this.form.patchValue({
        institutionId: year.institutionId ?? null,
        name: year.name,
        startDate: year.startDate,
        endDate: year.endDate,
      });
    } else {
      this.form.patchValue({ institutionId: this.data.institutionId ?? null });
    }
  }

  /** The intake types ticked for a pattern. */
  get intakeTypes(): IntakeType[] {
    const value = this.form.getRawValue();
    return [
      ...(value.annual ? (['ANNUAL'] as IntakeType[]) : []),
      ...(value.semesters ? (['SEMESTER'] as IntakeType[]) : []),
      ...(value.trimesters ? (['TRIMESTER'] as IntakeType[]) : []),
    ];
  }

  get canSubmit(): boolean {
    return this.form.valid && (this.isEditMode || this.intakeTypes.length > 0);
  }

  onSubmit(): void {
    if (!this.canSubmit) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSubmitting = true;
    const value = this.form.getRawValue();
    const base = {
      institutionId: value.institutionId,
      name: value.name.trim(),
      startDate: value.startDate,
      endDate: value.endDate,
    };
    const operation: Observable<AcademicYear> =
      this.isEditMode && this.data.academicYear
        ? this.academicYearService.updateAcademicYear(this.data.academicYear.id, base)
        : this.academicYearService.createFromPattern({ ...base, intakeTypes: this.intakeTypes });

    operation.subscribe({
      next: (saved) => {
        const affected = describeAffected(saved.applicationCount, saved.allocationCount);
        const message = this.isEditMode
          ? `${saved.name} saved.` + (affected ? ` ${affected}` : '')
          : `${saved.name} created with ${saved.intakes?.length ?? 0} intake(s).`;
        this.snackBar.open(message, 'Close', { duration: 6000 });
        this.dialogRef.close(true);
      },
      error: (error: HttpErrorResponse) => {
        this.snackBar.open(error?.error?.message || 'The year could not be saved', 'Close', {
          duration: 6000,
        });
        this.isSubmitting = false;
      },
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
