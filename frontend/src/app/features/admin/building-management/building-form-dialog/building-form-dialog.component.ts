import { Component, Inject, OnInit } from '@angular/core';

import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import {
  BuildingService,
  BuildingRequest,
  BuildingResponse,
} from '../../../../core/services/building.service';

@Component({
  selector: 'app-building-form-dialog',
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <div class="header-icon">
          <mat-icon>{{ data.mode === 'create' ? 'add_business' : 'edit' }}</mat-icon>
        </div>
        <div class="header-text">
          <h2 mat-dialog-title>{{ data.mode === 'create' ? 'Add New Building' : 'Edit Building' }}</h2>
          <p class="subtitle">{{ data.mode === 'create' ? 'Create a new accommodation building' : 'Update building information' }}</p>
        </div>
      </div>

      <mat-dialog-content>
        <form [formGroup]="buildingForm" class="building-form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Building Name</mat-label>
            <mat-icon matPrefix>apartment</mat-icon>
            <input matInput formControlName="name" placeholder="e.g., North Hall" required />
            @if (buildingForm.get('name')?.hasError('required')) {
              <mat-error>Building name is required</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Address</mat-label>
            <mat-icon matPrefix>location_on</mat-icon>
            <input matInput formControlName="address" placeholder="e.g., 123 Campus Drive" />
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Total Floors</mat-label>
            <mat-icon matPrefix>layers</mat-icon>
            <input
              matInput
              type="number"
              formControlName="totalFloors"
              placeholder="e.g., 5"
              min="1"
            />
            <mat-hint>Number of floors in the building</mat-hint>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Description</mat-label>
            <mat-icon matPrefix>description</mat-icon>
            <textarea
              matInput
              formControlName="description"
              rows="4"
              placeholder="Additional information about the building"
            ></textarea>
          </mat-form-field>
        </form>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-stroked-button (click)="onCancel()" [disabled]="isSubmitting" class="cancel-btn">
          Cancel
        </button>
        <button
          mat-raised-button
          color="primary"
          (click)="onSubmit()"
          [disabled]="!buildingForm.valid || isSubmitting"
          class="submit-btn"
        >
          @if (isSubmitting) {
            <mat-spinner diameter="20" class="button-spinner"></mat-spinner>
            {{ data.mode === 'create' ? 'Creating...' : 'Updating...' }}
          }
          @if (!isSubmitting) {
            <mat-icon>{{ data.mode === 'create' ? 'add' : 'check' }}</mat-icon>
            {{ data.mode === 'create' ? 'Create Building' : 'Update Building' }}
          }
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .dialog-container {
        min-width: 500px;
      }

      /* Dialog Header */
      .dialog-header {
        display: flex;
        align-items: flex-start;
        gap: 16px;
        padding: 24px 24px 0;
      }

      .header-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 56px;
        height: 56px;
        background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%);
        border-radius: 16px;
        border: 1px solid #fed7aa;
      }

      .header-icon mat-icon {
        font-size: 28px;
        width: 28px;
        height: 28px;
        color: #ea580c;
      }

      .header-text h2 {
        margin: 0 0 4px 0;
        font-size: 22px;
        font-weight: 700;
        color: #1e293b;
      }

      .header-text .subtitle {
        margin: 0;
        font-size: 14px;
        color: #64748b;
      }

      /* Dialog Content */
      mat-dialog-content {
        padding: 24px !important;
        max-height: 60vh;
        overflow-y: auto;
      }

      .building-form {
        display: flex;
        flex-direction: column;
        gap: 4px;
      }

      .full-width {
        width: 100%;
      }

      mat-icon[matPrefix] {
        color: #94a3b8;
        margin-right: 8px;
      }

      /* Dialog Actions */
      mat-dialog-actions {
        padding: 16px 24px 24px !important;
        gap: 12px;
        border-top: 1px solid #f1f5f9;
        margin-top: 8px;
      }

      .cancel-btn {
        border-radius: 10px !important;
        padding: 0 20px !important;
        height: 44px;
        color: #64748b;
        border-color: #e2e8f0 !important;
      }

      .cancel-btn:hover {
        background-color: #f8fafc;
      }

      .submit-btn {
        border-radius: 10px !important;
        padding: 0 24px !important;
        height: 44px;
        font-weight: 600;
        display: flex;
        align-items: center;
        gap: 6px;
      }

      .submit-btn mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
      }

      .button-spinner {
        display: inline-block;
        margin-right: 8px;
      }

      /* Responsive */
      @media (max-width: 600px) {
        .dialog-container {
          min-width: unset;
          width: 100%;
        }

        mat-dialog-content {
          padding: 16px !important;
        }

        .dialog-header {
          padding: 16px 16px 0;
        }

        .header-icon {
          width: 48px;
          height: 48px;
          border-radius: 12px;
        }

        .header-icon mat-icon {
          font-size: 24px;
          width: 24px;
          height: 24px;
        }

        .header-text h2 {
          font-size: 18px;
        }

        mat-dialog-actions {
          flex-direction: column-reverse;
          padding: 16px !important;
        }

        .cancel-btn,
        .submit-btn {
          width: 100%;
        }
      }
    `,
  ],
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

      const operation =
        this.data.mode === 'create'
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
          this.snackBar.open(error.error?.message || 'Failed to save building', 'Close', {
            duration: 5000,
          });
          this.isSubmitting = false;
        },
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
