import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  Output,
  inject,
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Observable } from 'rxjs';

import {
  AcademicYear,
  Intake,
  IntakeRequest,
  IntakeType,
} from '../../../../core/models/academic-year.model';
import { AcademicYearService } from '../../../../core/services/academic-year.service';
import { confirmAction } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';
import {
  describeAffected,
  describeIntake,
  parseApiDate,
  toApiDate,
} from '../academic-year-wording';

/**
 * The intakes inside one year, edited in place (GH-494). A new intake's window may be left empty:
 * the server fills it in from the move-in date.
 */
@Component({
  selector: 'app-intake-list',
  templateUrl: './intake-list.component.html',
  styleUrls: ['./intake-list.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    NgTemplateOutlet,
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
  ],
})
export class IntakeListComponent {
  private academicYearService = inject(AcademicYearService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);

  @Input({ required: true }) year!: AcademicYear;
  @Input() canManage = false;
  /** Emitted after any change, so the page reloads the year's window and intakes. */
  @Output() changed = new EventEmitter<void>();

  readonly types: { value: IntakeType; label: string }[] = [
    { value: 'ANNUAL', label: 'Annual' },
    { value: 'SEMESTER', label: 'Semester' },
    { value: 'TRIMESTER', label: 'Trimester' },
  ];
  readonly describe = describeIntake;

  /** The intake being edited, 'new' while adding one, or null. */
  editing: number | 'new' | null = null;
  isSaving = false;

  form = this.fb.nonNullable.group({
    intakeType: ['SEMESTER' as IntakeType, Validators.required],
    name: ['', [Validators.required, Validators.maxLength(50)]],
    stayStartDate: [null as Date | null, Validators.required],
    stayEndDate: [null as Date | null, Validators.required],
    applicationOpenDate: [null as Date | null],
    applicationCloseDate: [null as Date | null],
  });

  get intakes(): Intake[] {
    return this.year.intakes ?? [];
  }

  startAdd(): void {
    this.form.reset({
      intakeType: 'SEMESTER',
      name: '',
      stayStartDate: parseApiDate(this.year.startDate),
      stayEndDate: parseApiDate(this.year.endDate),
      applicationOpenDate: null,
      applicationCloseDate: null,
    });
    this.editing = 'new';
  }

  startEdit(intake: Intake): void {
    this.form.reset({
      intakeType: intake.intakeType,
      name: intake.name,
      stayStartDate: parseApiDate(intake.stayStartDate),
      stayEndDate: parseApiDate(intake.stayEndDate),
      applicationOpenDate: parseApiDate(intake.applicationOpenDate),
      applicationCloseDate: parseApiDate(intake.applicationCloseDate),
    });
    this.editing = intake.id;
  }

  cancel(): void {
    this.editing = null;
  }

  save(): void {
    if (this.form.invalid || this.editing === null) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const request: IntakeRequest = {
      intakeType: value.intakeType,
      name: value.name.trim(),
      stayStartDate: toApiDate(value.stayStartDate),
      stayEndDate: toApiDate(value.stayEndDate),
      applicationOpenDate: toApiDate(value.applicationOpenDate) || undefined,
      applicationCloseDate: toApiDate(value.applicationCloseDate) || undefined,
    };
    const call =
      this.editing === 'new'
        ? this.academicYearService.createIntake(this.year.id, request)
        : this.academicYearService.updateIntake(this.editing, request);
    this.run(call, (saved) => {
      const affected = describeAffected(saved.applicationCount, saved.allocationCount);
      return `${saved.name} saved.` + (affected ? ` ${affected}` : '');
    });
  }

  closeNow(intake: Intake): void {
    confirmAction(this.dialog, {
      title: `Close applications for ${intake.name}?`,
      message:
        'Applications close at the end of today. Students who already applied keep their place.',
      confirmLabel: 'Close applications',
    }).subscribe((confirmed) => {
      if (confirmed) {
        this.run(
          this.academicYearService.closeApplicationsNow(intake.id),
          () => `Applications for ${intake.name} close today.`
        );
      }
    });
  }

  remove(intake: Intake): void {
    confirmAction(this.dialog, {
      title: `Delete ${intake.name}?`,
      message:
        'The intake is removed. An intake with applications cannot be deleted; close its applications instead.',
      confirmLabel: 'Delete',
      destructive: true,
    }).subscribe((confirmed) => {
      if (confirmed) {
        this.run(this.academicYearService.deleteIntake(intake.id), () => `${intake.name} deleted.`);
      }
    });
  }

  private run<T>(call: Observable<T>, message: (result: T) => string): void {
    this.isSaving = true;
    call.subscribe({
      next: (result) => {
        this.isSaving = false;
        this.editing = null;
        this.snackBar.open(message(result), 'Close', { duration: 6000 });
        this.changed.emit();
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.isSaving = false;
        this.snackBar.open(error?.error?.message || 'The intake could not be saved', 'Close', {
          duration: 6000,
        });
        this.cdr.markForCheck();
      },
    });
  }
}
