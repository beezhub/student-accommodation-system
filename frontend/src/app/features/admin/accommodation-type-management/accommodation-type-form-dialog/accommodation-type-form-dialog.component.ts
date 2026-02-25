import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';

import {
  AccommodationTypeService,
  AccommodationType,
  AccommodationTypeRequest,
} from '../../../../core/services/accommodation-type.service';

export interface AccommodationTypeFormDialogData {
  mode: 'create' | 'edit';
  accommodationType?: AccommodationType;
}

@Component({
  selector: 'app-accommodation-type-form-dialog',
  templateUrl: './accommodation-type-form-dialog.component.html',
  styleUrls: ['./accommodation-type-form-dialog.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatSlideToggleModule,
    MatIconModule,
  ],
})
export class AccommodationTypeFormDialogComponent implements OnInit {
  form: FormGroup;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private accommodationTypeService: AccommodationTypeService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<AccommodationTypeFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AccommodationTypeFormDialogData
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(500)]],
      isActive: [true],
    });
  }

  ngOnInit(): void {
    if (this.data.mode === 'edit' && this.data.accommodationType) {
      this.form.patchValue({
        name: this.data.accommodationType.name,
        description: this.data.accommodationType.description || '',
        isActive: this.data.accommodationType.isActive,
      });
    }
  }

  get isEditMode(): boolean {
    return this.data.mode === 'edit';
  }

  get dialogTitle(): string {
    return this.isEditMode ? 'Edit Accommodation Type' : 'Create Accommodation Type';
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading = true;

    const request: AccommodationTypeRequest = {
      name: this.form.value.name.trim(),
      description: this.form.value.description?.trim() || null,
      isActive: this.form.value.isActive,
    };

    const operation = this.isEditMode
      ? this.accommodationTypeService.update(this.data.accommodationType!.id, request)
      : this.accommodationTypeService.create(request);

    operation.subscribe({
      next: () => {
        this.snackBar.open(
          this.isEditMode
            ? 'Accommodation type updated successfully'
            : 'Accommodation type created successfully',
          'Close',
          { duration: 3000 }
        );
        this.dialogRef.close(true);
      },
      error: (error) => {
        console.error('Failed to save accommodation type:', error);
        const message = error.error?.message || 'Failed to save accommodation type';
        this.snackBar.open(message, 'Close', { duration: 5000 });
        this.isLoading = false;
      },
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
