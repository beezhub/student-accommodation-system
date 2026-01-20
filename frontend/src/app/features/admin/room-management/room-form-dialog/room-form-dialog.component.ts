import { Component, Inject, OnInit } from '@angular/core';

import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RoomService, RoomRequest, RoomResponse } from '../../../../core/services/room.service';
import { BuildingResponse } from '../../../../core/services/building.service';

@Component({
  selector: 'app-room-form-dialog',
  template: `
    <h2 mat-dialog-title>{{ data.mode === 'create' ? 'Add Room' : 'Edit Room' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="roomForm" class="room-form">
        <mat-form-field appearance="outline">
          <mat-label>Room Number *</mat-label>
          <input matInput formControlName="roomNumber" placeholder="e.g., 101, A-201" required>
          @if (roomForm.get('roomNumber')?.hasError('required')) {
            <mat-error>
              Room number is required
            </mat-error>
          }
        </mat-form-field>
    
        <mat-form-field appearance="outline">
          <mat-label>Building *</mat-label>
          <mat-select formControlName="buildingId" required>
            @for (building of data.buildings; track building) {
              <mat-option [value]="building.id">
                {{ building.name }}
              </mat-option>
            }
          </mat-select>
          @if (roomForm.get('buildingId')?.hasError('required')) {
            <mat-error>
              Building is required
            </mat-error>
          }
        </mat-form-field>
    
        <mat-form-field appearance="outline">
          <mat-label>Capacity *</mat-label>
          <input matInput type="number" formControlName="capacity" placeholder="e.g., 2" min="1" required>
          @if (roomForm.get('capacity')?.hasError('required')) {
            <mat-error>
              Capacity is required
            </mat-error>
          }
          @if (roomForm.get('capacity')?.hasError('min')) {
            <mat-error>
              Capacity must be at least 1
            </mat-error>
          }
        </mat-form-field>
    
        <mat-form-field appearance="outline">
          <mat-label>Current Occupancy</mat-label>
          <input matInput type="number" formControlName="currentOccupancy" placeholder="e.g., 0" min="0">
          <mat-hint>Leave as 0 for new rooms</mat-hint>
        </mat-form-field>
    
        <mat-form-field appearance="outline">
          <mat-label>Status *</mat-label>
          <mat-select formControlName="status" required>
            <mat-option value="AVAILABLE">Available</mat-option>
            <mat-option value="OCCUPIED">Occupied</mat-option>
            <mat-option value="FULL">Full</mat-option>
            <mat-option value="MAINTENANCE">Maintenance</mat-option>
          </mat-select>
          @if (roomForm.get('status')?.hasError('required')) {
            <mat-error>
              Status is required
            </mat-error>
          }
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()" [disabled]="isSubmitting">Cancel</button>
      <button mat-raised-button color="primary" (click)="onSubmit()"
        [disabled]="!roomForm.valid || isSubmitting">
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
    .room-form {
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
    MatSelectModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
],
})
export class RoomFormDialogComponent implements OnInit {
  roomForm: FormGroup;
  isSubmitting = false;

  constructor(
    private fb: FormBuilder,
    private roomService: RoomService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<RoomFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: {
      mode: 'create' | 'edit';
      room?: RoomResponse;
      buildings: BuildingResponse[];
    }
  ) {
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

      const operation = this.data.mode === 'create'
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
          this.snackBar.open(
            error.error?.message || 'Failed to save room',
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
