import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { Observable } from 'rxjs';
import {
  RoomAllocationService,
  RoomAllocationRequest,
  RoomAllocationResponse,
  AllocationEligibilityResponse,
  RoomAvailabilityResponse,
} from '../../../../core/services/room-allocation.service';
import { AllocationApplicationFieldComponent } from './allocation-application-field/allocation-application-field.component';
import { AllocationRoomFieldComponent } from './allocation-room-field/allocation-room-field.component';
import { AllocationStatusMessageComponent } from './allocation-status-message/allocation-status-message.component';

/**
 * Allocates a room to one student by hand. The room and application pickers load and search their
 * own options; this dialog checks the student's eligibility and the room's availability, and only
 * allocates once both pass.
 */
@Component({
  selector: 'app-allocate-dialog',
  templateUrl: './allocate-dialog.component.html',
  styleUrl: './allocate-dialog.component.scss',
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
    MatDatepickerModule,
    MatNativeDateModule,
    AllocationApplicationFieldComponent,
    AllocationRoomFieldComponent,
    AllocationStatusMessageComponent,
  ],
})
export class AllocateDialogComponent {
  private fb = inject(FormBuilder);
  private allocationService = inject(RoomAllocationService);
  private snackBar = inject(MatSnackBar);
  dialogRef = inject<MatDialogRef<AllocateDialogComponent>>(MatDialogRef);
  data = inject(MAT_DIALOG_DATA);

  allocateForm: FormGroup;
  isSubmitting = false;

  eligibility: AllocationEligibilityResponse | null = null;
  eligibilityChecked = false;
  availability: RoomAvailabilityResponse | null = null;
  availabilityChecked = false;

  constructor() {
    this.allocateForm = this.fb.group({
      studentId: ['', [Validators.required, Validators.min(1)]],
      roomId: ['', Validators.required],
      applicationId: [''],
      allocationDate: [new Date(), Validators.required],
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
            hasActiveAllocation: false,
          };
          this.eligibilityChecked = true;
        },
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
            status: 'ERROR',
          };
          this.availabilityChecked = true;
        },
      });
    } else {
      this.availabilityChecked = false;
      this.availability = null;
    }
  }

  canSubmit(): boolean {
    return (
      this.allocateForm.valid &&
      this.eligibilityChecked &&
      this.eligibility?.isEligible === true &&
      this.availabilityChecked &&
      this.availability?.isAvailable === true
    );
  }

  onSubmit(): void {
    if (!this.canSubmit()) {
      return;
    }
    this.isSubmitting = true;
    this.allocationRequest().subscribe({
      next: () => {
        this.snackBar.open('Room allocated successfully', 'Close', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: (error) => {
        console.error('Error allocating room:', error);
        this.snackBar.open(error.error?.message || 'Failed to allocate room', 'Close', {
          duration: 5000,
        });
        this.isSubmitting = false;
      },
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  /** With an application chosen, allocate through it; otherwise allocate by student and date. */
  private allocationRequest(): Observable<RoomAllocationResponse> {
    const applicationId = this.allocateForm.get('applicationId')?.value;
    const roomId = this.allocateForm.get('roomId')?.value;
    if (applicationId) {
      return this.allocationService.allocateByApplication({ applicationId, roomId });
    }
    const allocationDate = this.allocateForm.get('allocationDate')?.value;
    const request: RoomAllocationRequest = {
      studentId: this.allocateForm.get('studentId')?.value,
      roomId,
      allocationDate:
        allocationDate instanceof Date ? allocationDate.toISOString() : allocationDate,
    };
    return this.allocationService.createAllocation(request);
  }
}
