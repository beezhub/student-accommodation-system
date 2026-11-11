import { Component, OnInit, inject } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ErrorStateMatcher } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Observable } from 'rxjs';

import { AcademicYearService } from '../../../../core/services/academic-year.service';
import { InstitutionService } from '../../../../core/services/institution.service';
import { AcademicYear, IntakeType } from '../../../../core/models/academic-year.model';
import { Institution } from '../../../../core/models/institution.model';
import {
  ALL_INSTITUTIONS,
  IntakePreview,
  describeAffected,
  formatDay,
  parseApiDate,
  previewIntakes,
  toApiDate,
} from '../academic-year-wording';

/**
 * 'pattern' creates a year with its intakes filled in (GH-494); 'edit' changes a year's name,
 * institution and dates. `institutionId` preselects the institution for a new year.
 */
export interface AcademicYearDialogData {
  mode: 'pattern' | 'edit';
  academicYear?: AcademicYear;
  institutionId?: number | null;
}

/** The last move-out day has to come after the first move-in day, as the server checks. */
function moveOutAfterMoveIn(group: AbstractControl): ValidationErrors | null {
  const start = group.get('startDate')?.value as Date | null;
  const end = group.get('endDate')?.value as Date | null;
  return start && end && end.getTime() <= start.getTime() ? { dateOrder: true } : null;
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
    MatDatepickerModule,
    MatSelectModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatTooltipModule,
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

  /** The intake choices, each with what it creates (GH-524). */
  readonly intakeOptions = [
    { control: 'annual', label: 'Annual', help: '1 intake for the whole year' },
    { control: 'semesters', label: 'Semesters', help: '2 intakes, each half of the year' },
    { control: 'trimesters', label: 'Trimesters', help: '3 intakes, each a third of the year' },
  ] as const;

  form = this.fb.group(
    {
      institutionId: [null as number | null],
      name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(20)]),
      startDate: [null as Date | null, Validators.required],
      endDate: [null as Date | null, Validators.required],
      annual: this.fb.nonNullable.control(true),
      semesters: this.fb.nonNullable.control(false),
      trimesters: this.fb.nonNullable.control(false),
    },
    { validators: moveOutAfterMoveIn }
  );

  /** Marks the move-out field red when the dates are the wrong way round, not only when empty. */
  readonly endDateErrorState: ErrorStateMatcher = {
    isErrorState: (control) =>
      !!control &&
      (control.invalid || this.form.hasError('dateOrder')) &&
      (control.touched || control.dirty),
  };

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
        startDate: parseApiDate(year.startDate),
        endDate: parseApiDate(year.endDate),
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

  /** The full name of the picked institution, which the closed select may cut off. */
  get institutionName(): string {
    const id = this.form.controls.institutionId.value;
    return this.institutions.find((institution) => institution.id === id)?.name ?? ALL_INSTITUTIONS;
  }

  /** The intakes that will be created, worked out the way the server does. */
  get preview(): IntakePreview[] {
    const { startDate, endDate } = this.form.getRawValue();
    return previewIntakes(toApiDate(startDate), toApiDate(endDate), this.intakeTypes);
  }

  /** "1 February to 1 July", with the year only when it differs from the first move-in. */
  describeStay(intake: IntakePreview): string {
    const year = this.form.controls.startDate.value?.getFullYear();
    return `${formatDay(intake.stayStartDate, year)} to ${formatDay(intake.stayEndDate, year)}`;
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
      startDate: toApiDate(value.startDate),
      endDate: toApiDate(value.endDate),
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
