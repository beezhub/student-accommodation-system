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
        <div class="header-icon">
          <mat-icon>{{ data.mode === 'create' ? 'add_circle' : 'edit' }}</mat-icon>
        </div>
        <div class="header-text">
          <h2 mat-dialog-title>{{ data.mode === 'create' ? 'Add New Room' : 'Edit Room' }}</h2>
          <p class="subtitle">
            {{
              data.mode === 'create' ? 'Create a new room in a building' : 'Update room information'
            }}
          </p>
        </div>
      </div>

      <mat-dialog-content>
        <form [formGroup]="roomForm" class="room-form">
          <div class="form-row">
            <mat-form-field appearance="outline" class="form-field">
              <mat-label>Room Number</mat-label>
              <mat-icon matPrefix>meeting_room</mat-icon>
              <input
                matInput
                formControlName="roomNumber"
                placeholder="e.g., 101, A-201"
                required
              />
              @if (roomForm.get('roomNumber')?.hasError('required')) {
              <mat-error>Room number is required</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="form-field">
              <mat-label>Building</mat-label>
              <mat-icon matPrefix>apartment</mat-icon>
              <mat-select formControlName="buildingId" required>
                @for (building of data.buildings; track building.id) {
                <mat-option [value]="building.id">
                  {{ building.name }}
                </mat-option>
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
              <input
                matInput
                type="number"
                formControlName="capacity"
                placeholder="e.g., 2"
                min="1"
                required
              />
              @if (roomForm.get('capacity')?.hasError('required')) {
              <mat-error>Capacity is required</mat-error>
              } @if (roomForm.get('capacity')?.hasError('min')) {
              <mat-error>Capacity must be at least 1</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="form-field">
              <mat-label>Current Occupancy</mat-label>
              <mat-icon matPrefix>person</mat-icon>
              <input
                matInput
                type="number"
                formControlName="currentOccupancy"
                placeholder="e.g., 0"
                min="0"
              />
              <mat-hint>Leave as 0 for new rooms</mat-hint>
            </mat-form-field>
          </div>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Status</mat-label>
            <mat-icon matPrefix>info</mat-icon>
            <mat-select formControlName="status" required>
              <mat-option value="AVAILABLE">
                <div class="status-option">
                  <mat-icon class="available">check_circle</mat-icon>
                  Available
                </div>
              </mat-option>
              <mat-option value="OCCUPIED">
                <div class="status-option">
                  <mat-icon class="occupied">people</mat-icon>
                  Occupied
                </div>
              </mat-option>
              <mat-option value="FULL">
                <div class="status-option">
                  <mat-icon class="full">do_not_disturb</mat-icon>
                  Full
                </div>
              </mat-option>
              <mat-option value="MAINTENANCE">
                <div class="status-option">
                  <mat-icon class="maintenance">build</mat-icon>
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
          [disabled]="!roomForm.valid || isSubmitting"
          class="submit-btn"
        >
          @if (isSubmitting) {
          <mat-spinner diameter="20" class="button-spinner"></mat-spinner>
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
        background: linear-gradient(135deg, #ecfeff 0%, #cffafe 100%);
        border-radius: 16px;
        border: 1px solid #a5f3fc;
      }

      .header-icon mat-icon {
        font-size: 28px;
        width: 28px;
        height: 28px;
        color: #0891b2;
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

      .room-form {
        display: flex;
        flex-direction: column;
        gap: 4px;
      }

      .form-row {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 16px;
      }

      .form-field,
      .full-width {
        width: 100%;
      }

      mat-icon[matPrefix] {
        color: #94a3b8;
        margin-right: 8px;
      }

      /* Status Option */
      .status-option {
        display: flex;
        align-items: center;
        gap: 10px;
      }

      .status-option mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
      }

      .status-option mat-icon.available {
        color: #059669;
      }

      .status-option mat-icon.occupied {
        color: #2563eb;
      }

      .status-option mat-icon.full {
        color: #dc2626;
      }

      .status-option mat-icon.maintenance {
        color: #525252;
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

        .form-row {
          grid-template-columns: 1fr;
          gap: 4px;
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
