import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { RoomService, RoomRequest } from '../../../../core/services/room.service';
import { BuildingResponse } from '../../../../core/services/building.service';

@Component({
  selector: 'app-bulk-room-form-dialog',
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <div class="header-icon">
          <mat-icon>library_add</mat-icon>
        </div>
        <div class="header-text">
          <h2 mat-dialog-title>Bulk Add Rooms</h2>
          <p class="subtitle">Create multiple rooms at once</p>
        </div>
      </div>

      <mat-dialog-content>
        <form [formGroup]="bulkForm" class="bulk-form">
          <!-- Building -->
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Building</mat-label>
            <mat-icon matPrefix>apartment</mat-icon>
            <mat-select formControlName="buildingId" required>
              @for (building of data.buildings; track building.id) {
              <mat-option [value]="building.id">
                {{ building.name }}
              </mat-option>
              }
            </mat-select>
            @if (bulkForm.get('buildingId')?.hasError('required')) {
            <mat-error>Building is required</mat-error>
            }
          </mat-form-field>

          <div class="form-row">
            <mat-form-field appearance="outline" class="form-field">
              <mat-label>Room Number Prefix</mat-label>
              <mat-icon matPrefix>tag</mat-icon>
              <input matInput formControlName="prefix" placeholder="e.g., A-" />
              <mat-hint>Optional prefix before numbers</mat-hint>
            </mat-form-field>

            <mat-form-field appearance="outline" class="form-field">
              <mat-label>Starting Number</mat-label>
              <mat-icon matPrefix>looks_one</mat-icon>
              <input matInput type="number" formControlName="startNumber" min="1" required />
              @if (bulkForm.get('startNumber')?.hasError('required')) {
              <mat-error>Required</mat-error>
              } @if (bulkForm.get('startNumber')?.hasError('min')) {
              <mat-error>Min 1</mat-error>
              }
            </mat-form-field>
          </div>

          <div class="form-row">
            <mat-form-field appearance="outline" class="form-field">
              <mat-label>Number of Rooms</mat-label>
              <mat-icon matPrefix>layers</mat-icon>
              <input matInput type="number" formControlName="count" min="1" max="100" required />
              @if (bulkForm.get('count')?.hasError('required')) {
              <mat-error>Required</mat-error>
              } @if (bulkForm.get('count')?.hasError('min')) {
              <mat-error>At least 1</mat-error>
              } @if (bulkForm.get('count')?.hasError('max')) {
              <mat-error>Max 100</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="form-field">
              <mat-label>Capacity per Room</mat-label>
              <mat-icon matPrefix>group</mat-icon>
              <input matInput type="number" formControlName="capacity" min="1" required />
              @if (bulkForm.get('capacity')?.hasError('required')) {
              <mat-error>Required</mat-error>
              } @if (bulkForm.get('capacity')?.hasError('min')) {
              <mat-error>At least 1</mat-error>
              }
            </mat-form-field>
          </div>

          <!-- Preview -->
          @if (generatedRoomNumbers.length > 0) {
          <div class="preview-section">
            <div class="preview-header">
              <mat-icon>preview</mat-icon>
              <span>Preview ({{ generatedRoomNumbers.length }} rooms)</span>
            </div>
            <div class="preview-chips">
              @for (roomNum of generatedRoomNumbers.slice(0, 12); track roomNum) {
              <span class="room-chip">{{ roomNum }}</span>
              } @if (generatedRoomNumbers.length > 12) {
              <span class="room-chip more">+{{ generatedRoomNumbers.length - 12 }} more</span>
              }
            </div>
          </div>
          }
        </form>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button
          mat-stroked-button
          (click)="onCancel()"
          [disabled]="isSubmitting"
          class="cancel-btn"
        >
          Cancel
        </button>
        <button
          mat-raised-button
          color="primary"
          (click)="onSubmit()"
          [disabled]="!bulkForm.valid || isSubmitting || generatedRoomNumbers.length === 0"
          class="submit-btn"
        >
          @if (isSubmitting) {
          <mat-spinner diameter="20" class="button-spinner"></mat-spinner>
          Creating... } @if (!isSubmitting) {
          <mat-icon>add</mat-icon>
          Create {{ generatedRoomNumbers.length }} Rooms }
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      /* Mobile-first base */
      .dialog-container {
        width: 100%;
      }

      .dialog-header {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        padding: 16px 16px 0;
      }

      .header-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 44px;
        height: 44px;
        min-width: 44px;
        background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
        border-radius: 12px;
        border: 1px solid #a7f3d0;
      }

      .header-icon mat-icon {
        font-size: 22px;
        width: 22px;
        height: 22px;
        color: #059669;
      }

      .header-text h2 {
        margin: 0 0 4px 0;
        font-size: 18px;
        font-weight: 700;
        color: #1e293b;
      }

      .header-text .subtitle {
        margin: 0;
        font-size: 13px;
        color: #64748b;
      }

      mat-dialog-content {
        padding: 16px !important;
        max-height: 65vh;
        overflow-y: auto;
      }

      .bulk-form {
        display: flex;
        flex-direction: column;
        gap: 4px;
      }

      .form-row {
        display: grid;
        grid-template-columns: 1fr;
        gap: 4px;
      }

      .form-field,
      .full-width {
        width: 100%;
      }

      mat-icon[matPrefix] {
        color: #94a3b8;
        margin-right: 8px;
      }

      /* Preview Section */
      .preview-section {
        background: #f0fdf4;
        border: 1px solid #bbf7d0;
        border-radius: 10px;
        padding: 12px;
        margin-top: 4px;
      }

      .preview-header {
        display: flex;
        align-items: center;
        gap: 6px;
        margin-bottom: 10px;
        font-size: 13px;
        font-weight: 600;
        color: #166534;
      }

      .preview-header mat-icon {
        font-size: 18px;
        width: 18px;
        height: 18px;
        color: #16a34a;
      }

      .preview-chips {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
      }

      .room-chip {
        display: inline-flex;
        align-items: center;
        padding: 4px 10px;
        background: white;
        border: 1px solid #d1fae5;
        border-radius: 6px;
        font-size: 12px;
        font-weight: 600;
        color: #065f46;
      }

      .room-chip.more {
        background: #dcfce7;
        border-color: #a7f3d0;
        color: #15803d;
        font-style: italic;
      }

      /* Dialog Actions */
      mat-dialog-actions {
        padding: 12px 16px 16px !important;
        gap: 10px;
        border-top: 1px solid #f1f5f9;
        margin-top: 8px;
      }

      .cancel-btn {
        border-radius: 10px !important;
        padding: 0 16px !important;
        height: 44px;
        color: #64748b;
        border-color: #e2e8f0 !important;
        flex: 1;
      }

      .submit-btn {
        border-radius: 10px !important;
        padding: 0 16px !important;
        height: 44px;
        font-weight: 600;
        display: flex;
        align-items: center;
        gap: 6px;
        flex: 1;
        justify-content: center;
      }

      .submit-btn mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
      }

      .button-spinner {
        display: inline-block;
        margin-right: 6px;
      }

      /* Tablet+ */
      @media (min-width: 480px) {
        .dialog-header {
          gap: 16px;
          padding: 24px 24px 0;
        }

        .header-icon {
          width: 56px;
          height: 56px;
          min-width: 56px;
          border-radius: 16px;
        }

        .header-icon mat-icon {
          font-size: 28px;
          width: 28px;
          height: 28px;
        }

        .header-text h2 {
          font-size: 22px;
        }

        .header-text .subtitle {
          font-size: 14px;
        }

        mat-dialog-content {
          padding: 24px !important;
        }

        .form-row {
          grid-template-columns: 1fr 1fr;
          gap: 16px;
        }

        mat-dialog-actions {
          padding: 16px 24px 24px !important;
          gap: 12px;
        }

        .cancel-btn,
        .submit-btn {
          flex: none;
          padding: 0 24px !important;
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
    MatSelectModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatIconModule,
    MatChipsModule,
  ],
})
export class BulkRoomFormDialogComponent {
  bulkForm: FormGroup;
  isSubmitting = false;
  generatedRoomNumbers: string[] = [];

  constructor(
    private fb: FormBuilder,
    private roomService: RoomService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<BulkRoomFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: {
      buildings: BuildingResponse[];
    }
  ) {
    this.bulkForm = this.fb.group({
      buildingId: ['', Validators.required],
      prefix: [''],
      startNumber: [1, [Validators.required, Validators.min(1)]],
      count: [5, [Validators.required, Validators.min(1), Validators.max(100)]],
      capacity: [2, [Validators.required, Validators.min(1)]],
    });

    // Regenerate preview when inputs change
    this.bulkForm.valueChanges.subscribe(() => this.generatePreview());
    this.generatePreview();
  }

  generatePreview(): void {
    const { prefix, startNumber, count } = this.bulkForm.value;
    if (!startNumber || !count || count < 1 || count > 100) {
      this.generatedRoomNumbers = [];
      return;
    }

    this.generatedRoomNumbers = [];
    for (let i = 0; i < count; i++) {
      this.generatedRoomNumbers.push(`${prefix || ''}${startNumber + i}`);
    }
  }

  onSubmit(): void {
    if (this.bulkForm.valid && this.generatedRoomNumbers.length > 0) {
      this.isSubmitting = true;
      const { buildingId, capacity } = this.bulkForm.value;

      const requests: RoomRequest[] = this.generatedRoomNumbers.map((roomNumber) => ({
        roomNumber,
        capacity,
        currentOccupancy: 0,
        status: 'AVAILABLE',
        buildingId,
      }));

      this.roomService.createBulkRooms(requests).subscribe({
        next: (rooms) => {
          this.snackBar.open(`Successfully created ${rooms.length} rooms`, 'Close', {
            duration: 3000,
          });
          this.dialogRef.close(true);
        },
        error: (error) => {
          console.error('Error creating rooms:', error);
          this.snackBar.open(
            error.error?.message || 'Failed to create rooms. Check for duplicate room numbers.',
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
