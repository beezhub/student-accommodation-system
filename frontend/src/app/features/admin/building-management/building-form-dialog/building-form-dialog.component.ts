import { Component, Inject, OnInit } from '@angular/core';

import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { BuildingService, BuildingRequest, BuildingResponse } from '../../../../core/services/building.service';

@Component({
  selector: 'app-building-form-dialog',
  template: `
    <h2 mat-dialog-title>{{ data.mode === 'create' ? 'Add Building' : 'Edit Building' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="buildingForm" class="building-form">
        <mat-form-field appearance="outline">
          <mat-label>Building Name *</mat-label>
          <input matInput formControlName="name" placeholder="e.g., North Hall" required>
          @if (buildingForm.get('name')?.hasError('required')) {
            <mat-error>
              Building name is required
            </mat-error>
          }
        </mat-form-field>
    
        <mat-form-field appearance="outline">
          <mat-label>Address</mat-label>
          <input matInput formControlName="address" placeholder="e.g., 123 Campus Drive">
        </mat-form-field>
    
        <mat-form-field appearance="outline">
          <mat-label>Total Floors</mat-label>
          <input matInput type="number" formControlName="totalFloors" placeholder="e.g., 5" min="1">
        </mat-form-field>
    
        <mat-form-field appearance="outline">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" rows="4"
          placeholder="Additional information about the building"></textarea>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()" [disabled]="isSubmitting">Cancel</button>
      <button mat-raised-button color="primary" (click)="onSubmit()"
        [disabled]="!buildingForm.valid || isSubmitting">
        @if (isSubmitting) {
          <mat-spinner diameter="20"></mat-spinner>
        }
        @if (!isSubmitting) {
          <span>{{ data.mode === 'create' ? 'Create' : 'Update' }}</span>
        }
      </button>
    </mat-dialog-actions>
    `,
  styles: [`
    .building-form {
      display: flex;
      flex-direction: column;
      gap: 16px;
      min-width: 400px;
      padding: 16px 0;
    }

    mat-dialog-content {
      max-height: 70vh;
      overflow-y: auto;
    }

    mat-dialog-actions button {
      margin-left: 8px;
    }

    mat-spinner {
      display: inline-block;
      margin-right: 8px;
    }
  `],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
],
})
export class BuildingFormDialogComponent implements OnInit {
  buildingForm: FormGroup;
  isSubmitting = false;

  constructor(
    private fb: FormBuilder,
    private buildingService: BuildingService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<BuildingFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { mode: 'create' | 'edit'; building?: BuildingResponse }
  ) {
    this.buildingForm = this.fb.group({
      name: ['', Validators.required],
      address: [''],
      totalFloors: [null],
      description: [''],
    });
  }

  ngOnInit(): void {
    if (this.data.mode === 'edit' && this.data.building) {
      this.buildingForm.patchValue({
        name: this.data.building.name,
        address: this.data.building.address,
        totalFloors: this.data.building.totalFloors,
        description: this.data.building.description,
      });
    }
  }

  onSubmit(): void {
    if (this.buildingForm.valid) {
      this.isSubmitting = true;
      const buildingData: BuildingRequest = this.buildingForm.value;

      const operation = this.data.mode === 'create'
        ? this.buildingService.createBuilding(buildingData)
        : this.buildingService.updateBuilding(this.data.building!.id, buildingData);

      operation.subscribe({
        next: () => {
          this.snackBar.open(
            `Building ${this.data.mode === 'create' ? 'created' : 'updated'} successfully`,
            'Close',
            { duration: 3000 }
          );
          this.dialogRef.close(true);
        },
        error: (error) => {
          console.error('Error saving building:', error);
          this.snackBar.open(
            error.error?.message || 'Failed to save building',
            'Close',
            { duration: 5000 }
          );
          this.isSubmitting = false;
        },
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
