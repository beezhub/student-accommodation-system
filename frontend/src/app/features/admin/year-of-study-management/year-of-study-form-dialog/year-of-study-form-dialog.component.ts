import { Component, Inject, OnInit } from '@angular/core';

import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import {
  YearOfStudyService,
  YearOfStudyResponse,
  YearOfStudyRequest,
} from '../../../../core/services/year-of-study.service';

// Material imports
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';

/**
 * Validates that a year of study value can be parsed to a numeric year.
 * Accepts formats like: "1", "First Year", "1st Year", "Year 1", etc.
 */
function numericYearValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;

  const value = control.value.toLowerCase().trim();

  // Direct numeric parse
  const directParse = parseInt(value, 10);
  if (!isNaN(directParse) && directParse > 0) return null;

  // Word to number mapping
  const validPatterns = [
    'first', 'one', '1st',
    'second', 'two', '2nd',
    'third', 'three', '3rd',
    'fourth', 'four', '4th',
    'fifth', 'five', '5th',
    'sixth', 'six', '6th',
    'seventh', 'seven', '7th',
    'eighth', 'eight', '8th',
    'ninth', 'nine', '9th',
    'tenth', 'ten', '10th'
  ];

  // Check if value contains any valid pattern
  for (const pattern of validPatterns) {
    if (value.includes(pattern)) return null;
  }

  // Check for embedded number (e.g., "Year 3")
  if (/\d+/.test(value)) return null;

  return { invalidYearFormat: true };
}

@Component({
  selector: 'app-year-of-study-form-dialog',
  templateUrl: './year-of-study-form-dialog.component.html',
  styleUrls: ['./year-of-study-form-dialog.component.css'],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatIconModule,
  ],
})
export class YearOfStudyFormDialogComponent implements OnInit {
  yearOfStudyForm: FormGroup;
  isLoading = false;
  mode: 'create' | 'edit';
  yearOfStudy?: YearOfStudyResponse;

  constructor(
    private fb: FormBuilder,
    private yearOfStudyService: YearOfStudyService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<YearOfStudyFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    this.mode = data.mode;
    this.yearOfStudy = data.yearOfStudy;

    this.yearOfStudyForm = this.fb.group({
      yearOfStudy: ['', [Validators.required, Validators.maxLength(50), numericYearValidator]],
    });
  }

  ngOnInit(): void {
    if (this.mode === 'edit' && this.yearOfStudy) {
      this.yearOfStudyForm.patchValue({
        yearOfStudy: this.yearOfStudy.yearOfStudy,
      });
    }
  }

  onSubmit(): void {
    if (this.yearOfStudyForm.invalid) {
      this.yearOfStudyForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    const request: YearOfStudyRequest = this.yearOfStudyForm.value;

    const operation =
      this.mode === 'create'
        ? this.yearOfStudyService.createYearOfStudy(request)
        : this.yearOfStudyService.updateYearOfStudy(this.yearOfStudy!.id, request);

    operation.subscribe({
      next: () => {
        const action = this.mode === 'create' ? 'created' : 'updated';
        this.snackBar.open(`Year of study ${action} successfully`, 'Close', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: (error) => {
        console.error(`Failed to ${this.mode} year of study:`, error);
        const action = this.mode === 'create' ? 'create' : 'update';
        this.snackBar.open(`Failed to ${action} year of study`, 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  getTitle(): string {
    return this.mode === 'create' ? 'Add New Year of Study' : 'Edit Year of Study';
  }

  getSubmitButtonText(): string {
    return this.mode === 'create' ? 'Create' : 'Update';
  }

  setYear(year: string): void {
    this.yearOfStudyForm.patchValue({ yearOfStudy: year });
  }
}
