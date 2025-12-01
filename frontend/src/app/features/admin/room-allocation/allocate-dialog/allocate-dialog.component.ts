import { Component, Inject, OnInit } from '@angular/core';
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
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { RoomAllocationService, RoomAllocationRequest, AllocationEligibilityResponse, RoomAvailabilityResponse } from '../../../../core/services/room-allocation.service';
import { RoomService, RoomResponse } from '../../../../core/services/room.service';
import { AdminService, ApplicationResponse } from '../../../../core/services/admin.service';
import { debounceTime, distinctUntilChanged, switchMap, of } from 'rxjs';

@Component({
  selector: 'app-allocate-dialog',
  template: `
    <h2 mat-dialog-title>Allocate Room to Student</h2>
    <mat-dialog-content>
      <form [formGroup]="allocateForm" class="allocate-form">
        <!-- Student ID -->
        <mat-form-field appearance="outline">
          <mat-label>Student ID *</mat-label>
          <input matInput type="number" formControlName="studentId"
                 placeholder="Enter student ID" required
                 (blur)="checkEligibility()">
          <mat-error *ngIf="allocateForm.get('studentId')?.hasError('required')">
            Student ID is required
          </mat-error>
          <mat-error *ngIf="allocateForm.get('studentId')?.hasError('min')">
            Invalid student ID
          </mat-error>
        </mat-form-field>

        <!-- Eligibility Status -->
        <div *ngIf="eligibilityChecked" class="status-message">
          <div *ngIf="eligibility?.isEligible" class="success-message">
            <mat-icon>check_circle</mat-icon>
            <span>Student is eligible for room allocation</span>
          </div>
          <div *ngIf="!eligibility?.isEligible" class="error-message">
            <mat-icon>error</mat-icon>
            <span>{{ eligibility?.reason || 'Student is not eligible' }}</span>
          </div>
        </div>

        <!-- Room Selection -->
        <mat-form-field appearance="outline">
          <mat-label>Room *</mat-label>
          <mat-select formControlName="roomId" required (selectionChange)="checkAvailability()">
            <mat-option *ngFor="let room of availableRooms" [value]="room.id">
              {{ room.roomNumber }} - {{ room.buildingName }}
              ({{ room.currentOccupancy }}/{{ room.capacity }} occupied)
            </mat-option>
          </mat-select>
          <mat-error *ngIf="allocateForm.get('roomId')?.hasError('required')">
            Room is required
          </mat-error>
        </mat-form-field>

        <!-- Room Availability Status -->
        <div *ngIf="availabilityChecked" class="status-message">
          <div *ngIf="availability?.isAvailable" class="success-message">
            <mat-icon>check_circle</mat-icon>
            <span>Room is available ({{ availability?.availableSpots }} spots remaining)</span>
          </div>
          <div *ngIf="!availability?.isAvailable" class="error-message">
            <mat-icon>error</mat-icon>
            <span>Room is not available ({{ availability?.status }})</span>
          </div>
        </div>

        <!-- Application Selection (Optional) -->
        <mat-form-field appearance="outline">
          <mat-label>Application (Optional)</mat-label>
          <mat-select formControlName="applicationId" (selectionChange)="onApplicationSelected()">
            <mat-option value="">No Application</mat-option>
            <mat-option *ngFor="let app of studentApplications" [value]="app.id">
              {{ app.applicationCode }} - {{ app.status }}
            </mat-option>
          </mat-select>
          <mat-hint>Link this allocation to an existing application</mat-hint>
        </mat-form-field>

        <!-- Allocation Date -->
        <mat-form-field appearance="outline">
          <mat-label>Allocation Date *</mat-label>
          <input matInput [matDatepicker]="picker" formControlName="allocationDate" required>
          <mat-datepicker-toggle matSuffix [for]="picker"></mat-datepicker-toggle>
          <mat-datepicker #picker></mat-datepicker>
          <mat-error *ngIf="allocateForm.get('allocationDate')?.hasError('required')">
            Allocation date is required
          </mat-error>
        </mat-form-field>

        <!-- Information Panel -->
        <div class="info-panel" *ngIf="allocateForm.get('studentId')?.value">
          <mat-icon>info</mat-icon>
          <div>
            <strong>Allocation Process:</strong>
            <ul>
              <li>Student eligibility will be verified</li>
              <li>Room availability will be checked</li>
              <li>Allocation will be created with ACTIVE status</li>
              <li *ngIf="allocateForm.get('applicationId')?.value">
                Application status will be updated
              </li>
            </ul>
          </div>
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()" [disabled]="isSubmitting">Cancel</button>
      <button mat-raised-button color="primary" (click)="onSubmit()"
              [disabled]="!canSubmit() || isSubmitting">
        <mat-spinner *ngIf="isSubmitting" diameter="20"></mat-spinner>
        <span *ngIf="!isSubmitting">Allocate Room</span>
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .allocate-form {
      display: flex;
      flex-direction: column;
      gap: 16px;
      min-width: 500px;
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

    .status-message {
      padding: 12px;
      border-radius: 4px;
      margin: -8px 0;
    }

    .success-message {
      background-color: #e8f5e9;
      color: #2e7d32;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .success-message mat-icon {
      color: #2e7d32;
    }

    .error-message {
      background-color: #ffebee;
      color: #c62828;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .error-message mat-icon {
      color: #c62828;
    }

    .info-panel {
      background-color: #e3f2fd;
      padding: 12px;
      border-radius: 4px;
      display: flex;
      gap: 12px;
      align-items: flex-start;
    }

    .info-panel mat-icon {
      color: #1976d2;
      margin-top: 2px;
    }

    .info-panel ul {
      margin: 8px 0 0 0;
      padding-left: 20px;
    }

    .info-panel li {
      margin: 4px 0;
      font-size: 14px;
    }
  `],
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
    MatAutocompleteModule,
    MatDatepickerModule,
    MatNativeDateModule,
  ],
})
export class AllocateDialogComponent implements OnInit {
  allocateForm: FormGroup;
  isSubmitting = false;
  availableRooms: RoomResponse[] = [];
  studentApplications: ApplicationResponse[] = [];

  eligibility: AllocationEligibilityResponse | null = null;
  eligibilityChecked = false;
  availability: RoomAvailabilityResponse | null = null;
  availabilityChecked = false;

  constructor(
    private fb: FormBuilder,
    private allocationService: RoomAllocationService,
    private roomService: RoomService,
    private adminService: AdminService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<AllocateDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    this.allocateForm = this.fb.group({
      studentId: ['', [Validators.required, Validators.min(1)]],
      roomId: ['', Validators.required],
      applicationId: [''],
      allocationDate: [new Date(), Validators.required],
    });
  }

  ngOnInit(): void {
    this.loadAvailableRooms();
    this.loadApplications();
  }

  loadAvailableRooms(): void {
    // Load rooms that are not full
    this.roomService.searchRooms({ page: 0, size: 1000 }).subscribe({
      next: (response) => {
        this.availableRooms = response.content.filter(
          room => room.status === 'AVAILABLE' || room.status === 'OCCUPIED'
        );
      },
      error: (error) => {
        console.error('Failed to load rooms:', error);
        this.snackBar.open('Failed to load available rooms', 'Close', { duration: 3000 });
      }
    });
  }

  loadApplications(): void {
    // Load approved applications for linking to allocations
    this.adminService.searchApplications({ status: 'APPROVED', page: 0, size: 1000 }).subscribe({
      next: (response) => {
        this.studentApplications = response.content;
      },
      error: (error) => {
        console.error('Failed to load applications:', error);
      }
    });
  }

  checkEligibility(): void {
    const studentId = this.allocateForm.get('studentId')?.value;
    if (studentId && studentId > 0) {
      this.allocationService.checkStudentEligibility(studentId).subscribe({
        next: (response) => {
          this.eligibility = response;
          this.eligibilityChecked = true;
        },
        error: (error) => {
          console.error('Failed to check eligibility:', error);
          this.eligibility = {
            studentId: studentId,
            isEligible: false,
            reason: 'Failed to check eligibility',
            hasApprovedApplication: false,
            hasActiveAllocation: false
          };
          this.eligibilityChecked = true;
        }
      });
    } else {
      this.eligibilityChecked = false;
      this.eligibility = null;
    }
  }

  checkAvailability(): void {
    const roomId = this.allocateForm.get('roomId')?.value;
    if (roomId) {
      this.allocationService.getRoomAvailability(roomId).subscribe({
        next: (response) => {
          this.availability = response;
          this.availabilityChecked = true;
        },
        error: (error) => {
          console.error('Failed to check availability:', error);
          const roomId = this.allocateForm.get('roomId')?.value;
          this.availability = {
            roomId: roomId,
            roomNumber: '',
            buildingName: '',
            capacity: 0,
            currentOccupancy: 0,
            availableSpots: 0,
            isAvailable: false,
            status: 'ERROR'
          };
          this.availabilityChecked = true;
        }
      });
    } else {
      this.availabilityChecked = false;
      this.availability = null;
    }
  }

  onApplicationSelected(): void {
    const applicationId = this.allocateForm.get('applicationId')?.value;
    if (applicationId) {
      // If an application is selected, we can use the allocateByApplication endpoint
      console.log('Application selected:', applicationId);
    }
  }

  canSubmit(): boolean {
    return this.allocateForm.valid &&
           this.eligibilityChecked &&
           this.eligibility?.isEligible === true &&
           this.availabilityChecked &&
           this.availability?.isAvailable === true;
  }

  onSubmit(): void {
    if (this.canSubmit()) {
      this.isSubmitting = true;
      const applicationId = this.allocateForm.get('applicationId')?.value;

      // If application is selected, use allocateByApplication
      if (applicationId) {
        const roomId = this.allocateForm.get('roomId')?.value;
        this.allocationService.allocateByApplication({
          applicationId: applicationId,
          roomId: roomId
        }).subscribe({
          next: () => {
            this.snackBar.open('Room allocated successfully', 'Close', { duration: 3000 });
            this.dialogRef.close(true);
          },
          error: (error) => {
            console.error('Error allocating room:', error);
            this.snackBar.open(
              error.error?.message || 'Failed to allocate room',
              'Close',
              { duration: 5000 }
            );
            this.isSubmitting = false;
          },
        });
      } else {
        // Otherwise, use manual allocation
        const allocationDate = this.allocateForm.get('allocationDate')?.value;
        const formattedDate = allocationDate instanceof Date
          ? allocationDate.toISOString().split('T')[0]
          : allocationDate;

        const request: RoomAllocationRequest = {
          studentId: this.allocateForm.get('studentId')?.value,
          roomId: this.allocateForm.get('roomId')?.value,
          allocationDate: formattedDate,
          status: 'ACTIVE'
        };

        this.allocationService.createAllocation(request).subscribe({
          next: () => {
            this.snackBar.open('Room allocated successfully', 'Close', { duration: 3000 });
            this.dialogRef.close(true);
          },
          error: (error) => {
            console.error('Error allocating room:', error);
            this.snackBar.open(
              error.error?.message || 'Failed to allocate room',
              'Close',
              { duration: 5000 }
            );
            this.isSubmitting = false;
          },
        });
      }
    }
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
