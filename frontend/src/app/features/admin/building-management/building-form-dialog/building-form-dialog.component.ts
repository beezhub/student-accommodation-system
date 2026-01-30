import { Component, Inject, OnInit } from '@angular/core';

import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { CommonModule } from '@angular/common';
import {
  BuildingService,
  BuildingRequest,
  BuildingResponse,
} from '../../../../core/services/building.service';
import { InstitutionService } from '../../../../core/services/institution.service';
import { Institution } from '../../../../core/models/institution.model';

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
            <mat-label>Institutions Served</mat-label>
            <mat-icon matPrefix>school</mat-icon>
            <mat-select formControlName="institutionIds" multiple>
              @for (institution of institutions; track institution.id) {
                <mat-option [value]="institution.id">{{ institution.name }}</mat-option>
              }
            </mat-select>
            <mat-hint>Select which institutions this building serves (leave empty for all)</mat-hint>
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
          } @else {
            <ng-container>
              <mat-icon>{{ data.mode === 'create' ? 'add' : 'check' }}</mat-icon>
              {{ data.mode === 'create' ? 'Create Building' : 'Update Building' }}
            </ng-container>
          }
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .dialog-container {
        min-width: 520px;
        max-width: 600px;
      }

      /* Dialog Header */
      .dialog-header {
        display: flex;
        align-items: flex-start;
        gap: 18px;
        padding: 28px 28px 0;
      }

      .header-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 60px;
        height: 60px;
        background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%);
        border-radius: 18px;
        border: 1px solid #fed7aa;
        box-shadow: 0 4px 12px rgba(234, 88, 12, 0.1);
      }

      .header-icon mat-icon {
        font-size: 30px;
        width: 30px;
        height: 30px;
        color: #ea580c;
      }

      .header-text h2 {
        margin: 0 0 6px 0;
        font-size: 24px;
        font-weight: 700;
        color: #1e293b;
        background: linear-gradient(135deg, #1e293b 0%, #ea580c 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
      }

      .header-text .subtitle {
        margin: 0;
        font-size: 15px;
        color: #64748b;
        font-weight: 500;
      }

      /* Dialog Content */
      mat-dialog-content {
        padding: 28px !important;
        max-height: 65vh;
        overflow-y: auto;
      }

      .building-form {
        display: flex;
        flex-direction: column;
        gap: 20px;
      }

      .full-width {
        width: 100%;
      }

      mat-form-field {
        margin-bottom: 4px;
      }

      mat-form-field.mat-form-field-outline {
        border-radius: 12px;
      }

      mat-icon[matPrefix] {
        color: #94a3b8;
        margin-right: 12px;
        font-size: 20px;
        width: 20px;
        height: 20px;
      }

      mat-hint {
        color: #94a3b8;
        font-size: 12px;
        margin-top: 4px;
      }

      mat-error {
        color: #dc2626;
        font-size: 12px;
        font-weight: 500;
      }

      /* Multi-select styling */
      mat-select[multiple] {
        padding: 4px 0;
      }

      /* Dialog Actions */
      mat-dialog-actions {
        padding: 20px 28px 28px !important;
        gap: 16px;
        border-top: 1px solid #f1f5f9;
        margin-top: 12px;
      }

      .cancel-btn {
        border-radius: 12px !important;
        padding: 0 24px !important;
        height: 48px;
        color: #64748b;
        border-color: #e2e8f0 !important;
        font-weight: 600;
        transition: all 0.2s ease;
      }

      .cancel-btn:hover {
        background-color: #f8fafc;
        border-color: #cbd5e1 !important;
        color: #475569;
      }

      .submit-btn {
        border-radius: 12px !important;
        padding: 0 28px !important;
        height: 48px;
        font-weight: 700;
        display: flex;
        align-items: center;
        gap: 8px;
        box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
        transition: all 0.2s ease;
      }

      .submit-btn:hover {
        transform: translateY(-1px);
        box-shadow: 0 6px 16px rgba(59, 130, 246, 0.4);
      }

      .submit-btn:disabled {
        transform: none;
        box-shadow: none;
      }

      .submit-btn mat-icon {
        font-size: 22px;
        width: 22px;
        height: 22px;
      }

      .button-spinner {
        display: inline-block;
        margin-right: 10px;
      }

      /* Responsive */
      @media (max-width: 600px) {
        .dialog-container {
          min-width: unset;
          width: 100%;
        }

        mat-dialog-content {
          padding: 20px !important;
        }

        .dialog-header {
          padding: 20px 20px 0;
          gap: 14px;
        }

        .header-icon {
          width: 52px;
          height: 52px;
          border-radius: 14px;
        }

        .header-icon mat-icon {
          font-size: 26px;
          width: 26px;
          height: 26px;
        }

        .header-text h2 {
          font-size: 20px;
        }

        .header-text .subtitle {
          font-size: 14px;
        }

        .building-form {
          gap: 16px;
        }

        mat-dialog-actions {
          flex-direction: column-reverse;
          padding: 20px !important;
          gap: 12px;
        }

        .cancel-btn,
        .submit-btn {
          width: 100%;
          height: 44px;
        }

        .submit-btn {
          padding: 0 20px !important;
        }
      }

      @media (max-width: 480px) {
        .dialog-header {
          padding: 16px 16px 0;
        }

        .header-text h2 {
          font-size: 18px;
        }

        mat-dialog-content {
          padding: 16px !important;
        }

        mat-dialog-actions {
          padding: 16px !important;
        }
      }
    `,
  ],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatIconModule,
    MatSelectModule,
    MatChipsModule,
  ],
})
export class BuildingFormDialogComponent implements OnInit {
  buildingForm: FormGroup;
  isSubmitting = false;
  institutions: Institution[] = [];

  constructor(
    private fb: FormBuilder,
    private buildingService: BuildingService,
    private institutionService: InstitutionService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<BuildingFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { mode: 'create' | 'edit'; building?: BuildingResponse }
  ) {
    this.buildingForm = this.fb.group({
      name: ['', Validators.required],
      address: [''],
      totalFloors: [null],
      description: [''],
      institutionIds: [[]],
    });
  }

  ngOnInit(): void {
    this.loadInstitutions();
    if (this.data.mode === 'edit' && this.data.building) {
      this.buildingForm.patchValue({
        name: this.data.building.name,
        address: this.data.building.address,
        totalFloors: this.data.building.totalFloors,
        description: this.data.building.description,
        institutionIds: this.data.building.institutions?.map(i => i.id) || [],
      });
    }
  }

  loadInstitutions(): void {
    this.institutionService.getInstitutions().subscribe({
      next: (institutions) => {
        this.institutions = institutions;
      },
      error: (error) => {
        console.error('Error loading institutions:', error);
      }
    });
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
