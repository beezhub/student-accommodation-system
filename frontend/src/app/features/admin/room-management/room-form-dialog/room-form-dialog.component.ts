import { Component, OnInit, inject } from '@angular/core';

import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { RoomService, RoomRequest, RoomResponse } from '../../../../core/services/room.service';
import { BuildingResponse } from '../../../../core/services/building.service';

@Component({
  selector: 'app-room-form-dialog',
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <div class="header-icon-wrap" aria-hidden="true">
          <mat-icon>{{ data.mode === 'create' ? 'add_circle' : 'edit' }}</mat-icon>
        </div>
        <div class="header-text">
          <h2 mat-dialog-title>{{ data.mode === 'create' ? 'Add New Room' : 'Edit Room' }}</h2>
          <p class="subtitle">
            {{ data.mode === 'create' ? 'Create a new room in a building' : 'Update room information' }}
          </p>
        </div>
      </div>

      <mat-dialog-content>
        <form [formGroup]="roomForm" class="room-form">
          <div class="form-row">
            <mat-form-field appearance="outline" class="form-field">
              <mat-label>Room Number</mat-label>
              <mat-icon matPrefix>meeting_room</mat-icon>
              <input matInput formControlName="roomNumber" placeholder="e.g., 101, A-201" required />
              @if (roomForm.get('roomNumber')?.hasError('required')) {
                <mat-error>Room number is required</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="form-field">
              <mat-label>Building</mat-label>
              <mat-icon matPrefix>apartment</mat-icon>
              <mat-select formControlName="buildingId" required>
                @for (building of data.buildings; track building.id) {
                  <mat-option [value]="building.id">{{ building.name }}</mat-option>
                }
              </mat-select>
              @if (roomForm.get('buildingId')?.hasError('required')) {
                <mat-error>Building is required</mat-error>
              }
            </mat-form-field>
          </div>

          <div class="form-row">
            <mat-form-field appearance="outline" class="form-field">
              <mat-label>Capacity</mat-label>
              <mat-icon matPrefix>group</mat-icon>
              <input matInput type="number" formControlName="capacity" placeholder="e.g., 2" min="1" required />
              @if (roomForm.get('capacity')?.hasError('required')) {
                <mat-error>Capacity is required</mat-error>
              }
              @if (roomForm.get('capacity')?.hasError('min')) {
                <mat-error>Capacity must be at least 1</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="form-field">
              <mat-label>Current Occupancy</mat-label>
              <mat-icon matPrefix>person</mat-icon>
              <input matInput type="number" formControlName="currentOccupancy" placeholder="e.g., 0" min="0" />
              <mat-hint>Leave as 0 for new rooms</mat-hint>
            </mat-form-field>
          </div>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Status</mat-label>
            <mat-icon matPrefix>info_outline</mat-icon>
            <mat-select formControlName="status" required>
              <mat-option value="AVAILABLE">
                <div class="status-option">
                  <mat-icon class="status-available">check_circle</mat-icon>
                  Available
                </div>
              </mat-option>
              <mat-option value="OCCUPIED">
                <div class="status-option">
                  <mat-icon class="status-occupied">people</mat-icon>
                  Occupied
                </div>
              </mat-option>
              <mat-option value="FULL">
                <div class="status-option">
                  <mat-icon class="status-full">do_not_disturb</mat-icon>
                  Full
                </div>
              </mat-option>
              <mat-option value="MAINTENANCE">
                <div class="status-option">
                  <mat-icon class="status-maintenance">build</mat-icon>
                  Maintenance
                </div>
              </mat-option>
            </mat-select>
            @if (roomForm.get('status')?.hasError('required')) {
              <mat-error>Status is required</mat-error>
            }
          </mat-form-field>
        </form>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-stroked-button (click)="onCancel()" [disabled]="isSubmitting" class="cancel-btn">
          Cancel
        </button>
        <button
          mat-raised-button
          (click)="onSubmit()"
          [disabled]="!roomForm.valid || isSubmitting"
          class="submit-btn"
        >
          @if (isSubmitting) {
            <mat-spinner diameter="18" class="button-spinner"></mat-spinner>
            {{ data.mode === 'create' ? 'Creating...' : 'Updating...' }}
          } @if (!isSubmitting) {
            <mat-icon>{{ data.mode === 'create' ? 'add' : 'check' }}</mat-icon>
            {{ data.mode === 'create' ? 'Create Room' : 'Update Room' }}
          }
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .dialog-container { width: 100%; }

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
        bottom: 0; left: 0; right: 0;
        height: 3px;
        background: linear-gradient(90deg, #0891b2 0%, #06b6d4 100%);
      }
      .header-icon-wrap {
        width: 40px; height: 40px; min-width: 40px;
        border-radius: 10px;
        background: linear-gradient(135deg, #06b6d4 0%, #0891b2 100%);
        display: flex; align-items: center; justify-content: center;
        flex-shrink: 0;
      }
      .header-icon-wrap mat-icon {
        color: white;
        font-size: 1.25rem; width: 1.25rem; height: 1.25rem;
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
        max-height: 60vh;
        overflow-y: auto;
      }
      .room-form {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
      }
      .form-row {
        display: grid;
        grid-template-columns: 1fr;
        gap: 0.25rem;
      }
      .form-field, .full-width { width: 100%; }

      mat-icon[matPrefix] {
        color: #94a3b8;
        margin-right: 0.5rem;
        font-size: 1.125rem; width: 1.125rem; height: 1.125rem;
      }

      .status-option {
        display: flex; align-items: center; gap: 0.625rem;
      }
      .status-option mat-icon { font-size: 1.25rem; width: 1.25rem; height: 1.25rem; }
      .status-available { color: #059669; }
      .status-occupied  { color: #2563eb; }
      .status-full      { color: #dc2626; }
      .status-maintenance { color: #525252; }

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
        height: 44px; width: 100%;
        color: #64748b;
        border-color: #e2e8f0 !important;
        font-weight: 600;
      }
      .submit-btn {
        border-radius: 8px !important;
        height: 44px; width: 100%;
        background: #0891b2 !important;
        color: white !important;
        font-weight: 700;
        display: flex; align-items: center;
        justify-content: center; gap: 0.375rem;
      }
      .submit-btn:hover:not(:disabled) { background: #06b6d4 !important; }
      .submit-btn:disabled { opacity: 0.6; }
      .submit-btn mat-icon { font-size: 1.125rem; width: 1.125rem; height: 1.125rem; }
      .button-spinner { display: inline-block; margin-right: 0.375rem; }

      /* ── Tablet+ ── */
      @media (min-width: 480px) {
        .dialog-header { padding: 1.5rem 1.75rem 1.375rem; gap: 1rem; }
        .header-icon-wrap { width: 44px; height: 44px; }
        .header-text h2 { font-size: 1.125rem; }
        mat-dialog-content { padding: 1.25rem 1.75rem !important; }
        .form-row { grid-template-columns: 1fr 1fr; gap: 1rem; }
        mat-dialog-actions { padding: 1rem 1.75rem 1.5rem !important; }
      }
      @media (min-width: 600px) {
        .dialog-container { min-width: 500px; }
        mat-dialog-actions {
          flex-direction: row;
          justify-content: flex-end;
        }
        .cancel-btn, .submit-btn { width: auto; padding: 0 1.5rem !important; }
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
    MatSelectModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatIconModule,
  ],
})
export class RoomFormDialogComponent implements OnInit {
  private fb = inject(FormBuilder);
  private roomService = inject(RoomService);
  private snackBar = inject(MatSnackBar);
  dialogRef = inject<MatDialogRef<RoomFormDialogComponent>>(MatDialogRef);
  data = inject<{
    mode: 'create' | 'edit';
    room?: RoomResponse;
    buildings: BuildingResponse[];
  }>(MAT_DIALOG_DATA);

  roomForm: FormGroup;
  isSubmitting = false;

  constructor() {
    this.roomForm = this.fb.group({
      roomNumber: ['', Validators.required],
      buildingId: ['', Validators.required],
      capacity: [1, [Validators.required, Validators.min(1)]],
      currentOccupancy: [0, Validators.min(0)],
      status: ['AVAILABLE', Validators.required],
    });
  }

  ngOnInit(): void {
    if (this.data.mode === 'edit' && this.data.room) {
      this.roomForm.patchValue({
        roomNumber: this.data.room.roomNumber,
        buildingId: this.data.room.buildingId,
        capacity: this.data.room.capacity,
        currentOccupancy: this.data.room.currentOccupancy,
        status: this.data.room.status,
      });
    }
  }

  onSubmit(): void {
    if (this.roomForm.valid) {
      this.isSubmitting = true;
      const roomData: RoomRequest = this.roomForm.value;

      const operation =
        this.data.mode === 'create'
          ? this.roomService.createRoom(roomData)
          : this.roomService.updateRoom(this.data.room!.id, roomData);

      operation.subscribe({
        next: () => {
          this.snackBar.open(
            `Room ${this.data.mode === 'create' ? 'created' : 'updated'} successfully`,
            'Close',
            { duration: 3000 }
          );
          this.dialogRef.close(true);
        },
        error: (error) => {
          console.error('Error saving room:', error);
          this.snackBar.open(error.error?.message || 'Failed to save room', 'Close', {
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
