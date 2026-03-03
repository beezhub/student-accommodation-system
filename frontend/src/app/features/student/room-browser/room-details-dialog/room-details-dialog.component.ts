import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { RoomResponse } from '../../../../core/services/room.service';

@Component({
  selector: 'app-room-details-dialog',
  template: `
    <h2 mat-dialog-title>
      <mat-icon>meeting_room</mat-icon>
      Room {{ data.room.roomNumber }}
    </h2>
    <mat-dialog-content>
      <div class="room-details">
        <!-- Building Information -->
        <div class="detail-section">
          <h3>
            <mat-icon>apartment</mat-icon>
            Building Information
          </h3>
          <div class="detail-item">
            <span class="label">Building:</span>
            <span class="value">{{ data.room.buildingName }}</span>
          </div>
          <div class="detail-item">
            <span class="label">Room Number:</span>
            <span class="value">{{ data.room.roomNumber }}</span>
          </div>
        </div>

        <mat-divider></mat-divider>

        <!-- Capacity Information -->
        <div class="detail-section">
          <h3>
            <mat-icon>people</mat-icon>
            Capacity & Occupancy
          </h3>
          <div class="detail-item">
            <span class="label">Total Capacity:</span>
            <span class="value"
              >{{ data.room.capacity }} {{ data.room.capacity === 1 ? 'person' : 'people' }}</span
            >
          </div>
          <div class="detail-item">
            <span class="label">Current Occupancy:</span>
            <span class="value"
              >{{ data.room.currentOccupancy }}
              {{ data.room.currentOccupancy === 1 ? 'person' : 'people' }}</span
            >
          </div>
          <div class="detail-item">
            <span class="label">Available Spots:</span>
            <span
              class="value availability"
              [class.available]="getAvailableSpots() > 0"
              [class.full]="getAvailableSpots() === 0"
            >
              {{ getAvailableSpots() }} {{ getAvailableSpots() === 1 ? 'spot' : 'spots' }}
            </span>
          </div>

          <!-- Visual Occupancy Bar -->
          <div class="occupancy-visual">
            <div class="occupancy-bar">
              <div
                class="occupancy-fill"
                [class]="getOccupancyClass()"
                [style.width.%]="getOccupancyPercentage()"
              ></div>
            </div>
            <span class="occupancy-percentage"
              >{{ getOccupancyPercentage() | number : '1.0-0' }}% occupied</span
            >
          </div>
        </div>

        <mat-divider></mat-divider>

        <!-- Status Information -->
        <div class="detail-section">
          <h3>
            <mat-icon>info</mat-icon>
            Status
          </h3>
          <mat-chip [color]="getStatusColor()" selected class="status-chip">
            <mat-icon>{{ getStatusIcon() }}</mat-icon>
            {{ data.room.status }}
          </mat-chip>

          <div class="status-description">
            @if (data.room.status === 'AVAILABLE') {
            <p>This room is fully available for new allocations.</p>
            } @if (data.room.status === 'OCCUPIED') {
            <p>
              This room is currently occupied.
              {{
                getAvailableSpots() > 0
                  ? 'Additional spots are available.'
                  : 'No additional spots available.'
              }}
            </p>
            } @if (data.room.status === 'FULL') {
            <p>This room is currently at full capacity.</p>
            } @if (data.room.status === 'MAINTENANCE') {
            <p>This room is currently under maintenance and unavailable for allocation.</p>
            }
          </div>
        </div>

        <mat-divider></mat-divider>

        <!-- Application Information -->
        <div class="detail-section info-box">
          <mat-icon>help_outline</mat-icon>
          <div>
            <h4>How to Apply</h4>
            <p>
              To request accommodation in this room or building, please submit an application
              through the Applications page. Once approved, the admin will allocate a suitable room
              for you.
            </p>
          </div>
        </div>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onClose()">Close</button>
      @if (getAvailableSpots() > 0) {
      <button mat-raised-button color="primary">
        <mat-icon>assignment</mat-icon>
        Apply for Accommodation
      </button>
      }
    </mat-dialog-actions>
  `,
  styles: [
    `
      h2[mat-dialog-title] {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 16px;
      }

      h2[mat-dialog-title] mat-icon {
        color: #1976d2;
      }

      mat-dialog-content {
        max-height: 70vh;
        overflow-y: auto;
      }

      .room-details {
        padding: 8px 0;
      }

      .detail-section {
        margin: 20px 0;
      }

      .detail-section h3 {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 16px;
        font-weight: 500;
        margin-bottom: 16px;
        color: #333;
      }

      .detail-section h3 mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
        color: #1976d2;
      }

      .detail-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 8px 0;
        border-bottom: 1px solid #f0f0f0;
      }

      .detail-item:last-of-type {
        border-bottom: none;
      }

      .label {
        font-weight: 500;
        color: #666;
      }

      .value {
        color: #333;
        font-weight: 500;
      }

      .value.availability {
        padding: 4px 12px;
        border-radius: 12px;
        font-size: 14px;
      }

      .value.availability.available {
        background-color: #e8f5e9;
        color: #2e7d32;
      }

      .value.availability.full {
        background-color: #ffebee;
        color: #c62828;
      }

      /* Occupancy Visual */
      .occupancy-visual {
        margin-top: 16px;
        padding: 12px;
        background-color: #f5f5f5;
        border-radius: 8px;
      }

      .occupancy-bar {
        height: 12px;
        background-color: #e0e0e0;
        border-radius: 6px;
        overflow: hidden;
        margin-bottom: 8px;
      }

      .occupancy-fill {
        height: 100%;
        transition: width 0.3s ease;
      }

      .occupancy-fill.empty {
        background-color: #4caf50;
      }

      .occupancy-fill.low {
        background-color: #8bc34a;
      }

      .occupancy-fill.medium {
        background-color: #ff9800;
      }

      .occupancy-fill.full {
        background-color: #f44336;
      }

      .occupancy-percentage {
        display: block;
        text-align: center;
        font-size: 13px;
        color: #666;
        font-weight: 500;
      }

      /* Status */
      .status-chip {
        display: inline-flex;
        align-items: center;
        gap: 4px;
      }

      .status-chip mat-icon {
        font-size: 18px;
        width: 18px;
        height: 18px;
      }

      .status-description {
        margin-top: 12px;
        padding: 12px;
        background-color: #f5f5f5;
        border-radius: 4px;
      }

      .status-description p {
        margin: 0;
        font-size: 14px;
        color: #666;
        line-height: 1.5;
      }

      /* Info Box */
      .info-box {
        background-color: #e3f2fd;
        padding: 16px;
        border-radius: 8px;
        display: flex;
        gap: 12px;
        align-items: flex-start;
      }

      .info-box mat-icon {
        color: #1976d2;
        margin-top: 2px;
      }

      .info-box h4 {
        margin: 0 0 8px 0;
        font-size: 15px;
        font-weight: 500;
        color: #1976d2;
      }

      .info-box p {
        margin: 0;
        font-size: 14px;
        color: #666;
        line-height: 1.5;
      }

      mat-divider {
        margin: 16px 0;
      }

      mat-dialog-actions {
        padding: 16px 0 0 0 !important;
        margin: 0 !important;
      }

      mat-dialog-actions button {
        display: inline-flex;
        align-items: center;
        gap: 6px;
      }
    `,
  ],
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
  ],
})
export class RoomDetailsDialogComponent {
  public dialogRef = inject(MatDialogRef<RoomDetailsDialogComponent>);
  public data: { room: RoomResponse } = inject(MAT_DIALOG_DATA);

  constructor() {}

  getAvailableSpots(): number {
    return this.data.room.capacity - this.data.room.currentOccupancy;
  }

  getOccupancyPercentage(): number {
    return (this.data.room.currentOccupancy / this.data.room.capacity) * 100;
  }

  getOccupancyClass(): string {
    const percentage = this.getOccupancyPercentage();
    if (percentage === 0) return 'empty';
    if (percentage < 50) return 'low';
    if (percentage < 100) return 'medium';
    return 'full';
  }

  getStatusColor(): string {
    if (this.data.room.status === 'AVAILABLE') return 'primary';
    if (this.data.room.status === 'OCCUPIED') return 'accent';
    if (this.data.room.status === 'FULL') return 'warn';
    return '';
  }

  getStatusIcon(): string {
    switch (this.data.room.status) {
      case 'AVAILABLE':
        return 'check_circle';
      case 'OCCUPIED':
        return 'schedule';
      case 'FULL':
        return 'block';
      case 'MAINTENANCE':
        return 'build';
      default:
        return 'help';
    }
  }

  onClose(): void {
    this.dialogRef.close();
  }
}
