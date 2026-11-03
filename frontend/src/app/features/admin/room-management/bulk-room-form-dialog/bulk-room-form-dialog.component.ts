import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
        <div class="header-icon-wrap" aria-hidden="true">
          <mat-icon>library_add</mat-icon>
        </div>
        <div class="header-text">
          <h2 mat-dialog-title>Bulk Add Rooms</h2>
          <p class="subtitle">Create multiple rooms at once</p>
        </div>
      </div>

      <mat-dialog-content>
        <form [formGroup]="bulkForm" class="bulk-form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Building</mat-label>
            <mat-icon matPrefix>apartment</mat-icon>
            <mat-select formControlName="buildingId" required>
              @for (building of data.buildings; track building.id) {
              <mat-option [value]="building.id">{{ building.name }}</mat-option>
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
              <span>Preview &mdash; {{ generatedRoomNumbers.length }} rooms</span>
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
          (click)="onSubmit()"
          [disabled]="!bulkForm.valid || isSubmitting || generatedRoomNumbers.length === 0"
          class="submit-btn"
        >
          @if (isSubmitting) {
          <mat-spinner diameter="18" class="button-spinner"></mat-spinner>
          Creating... } @if (!isSubmitting) {
          <mat-icon>add</mat-icon>
          Create {{ generatedRoomNumbers.length }} Rooms }
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .dialog-container {
        width: 100%;
      }

      /* ── Header band (cyan = rooms) ── */
      .dialog-header {
        display: flex;
        align-items: center;
        gap: 0.875rem;
        padding: 1.25rem 1.5rem 1.125rem;
        background: #0f172a;
        position: relative;
        flex-shrink: 0;
      }
      .dialog-header::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 0;
        right: 0;
        height: 3px;
        background: linear-gradient(90deg, #0891b2 0%, #06b6d4 100%);
      }
      .header-icon-wrap {
        width: 40px;
        height: 40px;
        min-width: 40px;
        border-radius: 10px;
        background: linear-gradient(135deg, #06b6d4 0%, #0891b2 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }
      .header-icon-wrap mat-icon {
        color: white;
        font-size: 1.25rem;
        width: 1.25rem;
        height: 1.25rem;
      }
      .header-text h2 {
        margin: 0 0 0.125rem 0;
        font-size: 1.0625rem;
        font-weight: 700;
        color: white;
        letter-spacing: -0.01em;
      }
      .header-text .subtitle {
        margin: 0;
        font-size: 0.8125rem;
        color: #94a3b8;
      }

      /* ── Content ── */
      mat-dialog-content {
        padding: 1.25rem 1.5rem !important;
        max-height: 65vh;
        overflow-y: auto;
      }
      .bulk-form {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
      }
      .form-row {
        display: grid;
        grid-template-columns: 1fr;
        gap: 0.25rem;
      }
      .form-field,
      .full-width {
        width: 100%;
      }
      mat-icon[matPrefix] {
        color: #94a3b8;
        margin-right: 0.5rem;
        font-size: 1.125rem;
        width: 1.125rem;
        height: 1.125rem;
      }

      /* Preview Section */
      .preview-section {
        background: #ecfeff;
        border: 1px solid #a5f3fc;
        border-radius: 10px;
        padding: 0.875rem;
        margin-top: 0.5rem;
      }
      .preview-header {
        display: flex;
        align-items: center;
        gap: 0.375rem;
        margin-bottom: 0.625rem;
        font-size: 0.8125rem;
        font-weight: 700;
        color: #0e7490;
      }
      .preview-header mat-icon {
        font-size: 1.125rem;
        width: 1.125rem;
        height: 1.125rem;
        color: #0891b2;
      }
      .preview-chips {
        display: flex;
        flex-wrap: wrap;
        gap: 0.375rem;
      }
      .room-chip {
        display: inline-flex;
        align-items: center;
        padding: 0.1875rem 0.5rem;
        background: white;
        border: 1px solid #cffafe;
        border-radius: 5px;
        font-size: 0.75rem;
        font-weight: 600;
        color: #0891b2;
        font-family: monospace;
      }
      .room-chip.more {
        background: #cffafe;
        border-color: #a5f3fc;
        color: #0e7490;
        font-style: italic;
        font-family: inherit;
      }

      /* ── Actions ── */
      mat-dialog-actions {
        padding: 0.875rem 1.5rem 1.25rem !important;
        gap: 0.75rem;
        border-top: 1px solid #f1f5f9;
        flex-direction: column-reverse;
        margin-top: 0;
      }
      .cancel-btn {
        border-radius: 8px !important;
        height: 44px;
        width: 100%;
        color: #64748b;
        border-color: #e2e8f0 !important;
        font-weight: 600;
      }
      .submit-btn {
        border-radius: 8px !important;
        height: 44px;
        width: 100%;
        background: #0891b2 !important;
        color: white !important;
        font-weight: 700;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.375rem;
      }
      .submit-btn:hover:not(:disabled) {
        background: #06b6d4 !important;
      }
      .submit-btn:disabled {
        opacity: 0.6;
      }
      .submit-btn mat-icon {
        font-size: 1.125rem;
        width: 1.125rem;
        height: 1.125rem;
      }
      .button-spinner {
        display: inline-block;
        margin-right: 0.375rem;
      }

      /* ── Tablet+ ── */
      @media (min-width: 480px) {
        .dialog-header {
          padding: 1.5rem 1.75rem 1.375rem;
          gap: 1rem;
        }
        .header-icon-wrap {
          width: 44px;
          height: 44px;
        }
        .header-text h2 {
          font-size: 1.125rem;
        }
        mat-dialog-content {
          padding: 1.25rem 1.75rem !important;
        }
        .form-row {
          grid-template-columns: 1fr 1fr;
          gap: 1rem;
        }
        mat-dialog-actions {
          padding: 1rem 1.75rem 1.5rem !important;
          flex-direction: row;
          justify-content: flex-end;
        }
        .cancel-btn,
        .submit-btn {
          width: auto;
          padding: 0 1.5rem !important;
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
  private fb = inject(FormBuilder);
  private roomService = inject(RoomService);
  private snackBar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);
  dialogRef = inject<MatDialogRef<BulkRoomFormDialogComponent>>(MatDialogRef);
  data = inject<{
    buildings: BuildingResponse[];
  }>(MAT_DIALOG_DATA);

  bulkForm: FormGroup;
  isSubmitting = false;
  generatedRoomNumbers: string[] = [];

  constructor() {
    this.bulkForm = this.fb.group({
      buildingId: ['', Validators.required],
      prefix: [''],
      startNumber: [1, [Validators.required, Validators.min(1)]],
      count: [5, [Validators.required, Validators.min(1), Validators.max(100)]],
      capacity: [2, [Validators.required, Validators.min(1)]],
    });

    this.bulkForm.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.generatePreview());
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
