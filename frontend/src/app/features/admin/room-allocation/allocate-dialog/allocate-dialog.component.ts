import { Component, Inject, OnInit } from '@angular/core';

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
import {
  RoomAllocationService,
  RoomAllocationRequest,
  AllocationEligibilityResponse,
  RoomAvailabilityResponse,
} from '../../../../core/services/room-allocation.service';
import { RoomService, RoomResponse } from '../../../../core/services/room.service';
import { AdminService, ApplicationResponse } from '../../../../core/services/admin.service';
import { debounceTime, distinctUntilChanged, switchMap, of } from 'rxjs';

@Component({
  selector: 'app-allocate-dialog',
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <div class="header-icon">
          <mat-icon>assignment_ind</mat-icon>
        </div>
        <div class="header-text">
          <h2 mat-dialog-title>Allocate Room to Student</h2>
          <p class="subtitle">Assign a room to an eligible student</p>
        </div>
      </div>

      <mat-dialog-content>
        <form [formGroup]="allocateForm" class="allocate-form">
          <!-- Student ID -->
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Student ID</mat-label>
            <mat-icon matPrefix>badge</mat-icon>
            <input
              matInput
              type="number"
              formControlName="studentId"
              placeholder="Enter student ID"
              required
              (blur)="checkEligibility()"
            />
            @if (allocateForm.get('studentId')?.hasError('required')) {
            <mat-error>Student ID is required</mat-error>
            } @if (allocateForm.get('studentId')?.hasError('min')) {
            <mat-error>Invalid student ID</mat-error>
            }
          </mat-form-field>

          <!-- Eligibility Status -->
          @if (eligibilityChecked) {
          <div
            class="status-message"
            [class.success]="eligibility?.isEligible"
            [class.error]="!eligibility?.isEligible"
          >
            @if (eligibility?.isEligible) {
            <mat-icon>check_circle</mat-icon>
            <span>Student is eligible for room allocation</span>
            } @if (!eligibility?.isEligible) {
            <mat-icon>error</mat-icon>
            <span>{{ eligibility?.reason || 'Student is not eligible' }}</span>
            }
          </div>
          }

          <!-- Room Selection -->
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Room</mat-label>
            <mat-icon matPrefix>meeting_room</mat-icon>
            <mat-select formControlName="roomId" required (selectionChange)="checkAvailability()">
              @for (room of availableRooms; track room.id) {
              <mat-option [value]="room.id">
                <div class="room-option">
                  <span class="room-name">{{ room.roomNumber }} - {{ room.buildingName }}</span>
                  <span class="room-capacity"
                    >({{ room.currentOccupancy }}/{{ room.capacity }})</span
                  >
                </div>
              </mat-option>
              }
            </mat-select>
            @if (allocateForm.get('roomId')?.hasError('required')) {
            <mat-error>Room is required</mat-error>
            }
          </mat-form-field>

          <!-- Room Availability Status -->
          @if (availabilityChecked) {
          <div
            class="status-message"
            [class.success]="availability?.isAvailable"
            [class.error]="!availability?.isAvailable"
          >
            @if (availability?.isAvailable) {
            <mat-icon>check_circle</mat-icon>
            <span>Room is available ({{ availability?.availableSpots }} spots remaining)</span>
            } @if (!availability?.isAvailable) {
            <mat-icon>error</mat-icon>
            <span>Room is not available ({{ availability?.status }})</span>
            }
          </div>
          }

          <!-- Application Selection (Optional) -->
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Application (Optional)</mat-label>
            <mat-icon matPrefix>description</mat-icon>
            <mat-select formControlName="applicationId" (selectionChange)="onApplicationSelected()">
              <mat-option value="">No Application</mat-option>
              @for (app of studentApplications; track app.id) {
              <mat-option [value]="app.id">
                <div class="app-option">
                  <span class="app-code">{{ app.applicationCode }}</span>
                  <span class="app-status" [class]="app.status.toLowerCase()">{{
                    app.status
                  }}</span>
                </div>
              </mat-option>
              }
            </mat-select>
            <mat-hint>
              <mat-icon class="hint-icon">info</mat-icon>
              Link this allocation to an existing application
            </mat-hint>
          </mat-form-field>

          <!-- Allocation Date -->
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Allocation Date</mat-label>
            <mat-icon matPrefix>event</mat-icon>
            <input matInput [matDatepicker]="picker" formControlName="allocationDate" required />
            <mat-datepicker-toggle matSuffix [for]="picker"></mat-datepicker-toggle>
            <mat-datepicker #picker></mat-datepicker>
            @if (allocateForm.get('allocationDate')?.hasError('required')) {
            <mat-error>Allocation date is required</mat-error>
            }
          </mat-form-field>

          <!-- Information Panel -->
          @if (allocateForm.get('studentId')?.value) {
          <div class="info-panel">
            <mat-icon>lightbulb</mat-icon>
            <div class="info-content">
              <strong>Allocation Process</strong>
              <ul>
                <li>Student eligibility will be verified</li>
                <li>Room availability will be checked</li>
                <li>Allocation will be created with ACTIVE status</li>
                @if (allocateForm.get('applicationId')?.value) {
                <li>Application status will be updated</li>
                }
              </ul>
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
          [disabled]="!canSubmit() || isSubmitting"
          class="submit-btn"
        >
          @if (isSubmitting) {
          <mat-spinner diameter="20" class="button-spinner"></mat-spinner>
          Allocating... } @if (!isSubmitting) {
          <mat-icon>assignment_turned_in</mat-icon>
          Allocate Room }
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .dialog-container {
        min-width: 550px;
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
        background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
        border-radius: 16px;
        border: 1px solid #ddd6fe;
      }

      .header-icon mat-icon {
        font-size: 28px;
        width: 28px;
        height: 28px;
        color: #7c3aed;
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

      .allocate-form {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }

      .full-width {
        width: 100%;
      }

      mat-icon[matPrefix] {
        color: #94a3b8;
        margin-right: 8px;
      }

      /* Room Option */
      .room-option {
        display: flex;
        justify-content: space-between;
        align-items: center;
        width: 100%;
      }

      .room-name {
        font-weight: 500;
      }

      .room-capacity {
        font-size: 12px;
        color: #64748b;
        background: #f1f5f9;
        padding: 2px 8px;
        border-radius: 4px;
      }

      /* App Option */
      .app-option {
        display: flex;
        justify-content: space-between;
        align-items: center;
        width: 100%;
      }

      .app-code {
        font-weight: 500;
        font-family: 'SF Mono', 'Fira Code', monospace;
      }

      .app-status {
        font-size: 11px;
        font-weight: 600;
        padding: 2px 8px;
        border-radius: 10px;
        text-transform: uppercase;
      }

      .app-status.approved {
        background: #d1fae5;
        color: #059669;
      }

      .app-status.pending {
        background: #fef3c7;
        color: #d97706;
      }

      /* Hint styling */
      mat-hint {
        display: flex;
        align-items: center;
        gap: 4px;
        color: #64748b;
      }

      .hint-icon {
        font-size: 14px !important;
        width: 14px !important;
        height: 14px !important;
        color: #94a3b8;
      }

      /* Status Messages */
      .status-message {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 14px 16px;
        border-radius: 12px;
        font-size: 14px;
        font-weight: 500;
        margin: -4px 0 8px 0;
      }

      .status-message mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
      }

      .status-message.success {
        background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
        color: #047857;
        border: 1px solid #a7f3d0;
      }

      .status-message.success mat-icon {
        color: #10b981;
      }

      .status-message.error {
        background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
        color: #b91c1c;
        border: 1px solid #fca5a5;
      }

      .status-message.error mat-icon {
        color: #ef4444;
      }

      /* Info Panel */
      .info-panel {
        display: flex;
        gap: 14px;
        padding: 16px;
        background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
        border-radius: 12px;
        border: 1px solid #93c5fd;
        margin-top: 8px;
      }

      .info-panel > mat-icon {
        color: #2563eb;
        font-size: 22px;
        width: 22px;
        height: 22px;
        margin-top: 2px;
      }

      .info-content strong {
        display: block;
        color: #1e40af;
        font-size: 14px;
        margin-bottom: 8px;
      }

      .info-panel ul {
        margin: 0;
        padding-left: 18px;
      }

      .info-panel li {
        margin: 4px 0;
        font-size: 13px;
        color: #1e40af;
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
          (room) => room.status === 'AVAILABLE' || room.status === 'OCCUPIED'
        );
      },
      error: (error) => {
        console.error('Failed to load rooms:', error);
        this.snackBar.open('Failed to load available rooms', 'Close', { duration: 3000 });
      },
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
      },
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

  onApplicationSelected(): void {
    const applicationId = this.allocateForm.get('applicationId')?.value;
    if (applicationId) {
      // If an application is selected, we can use the allocateByApplication endpoint
      console.log('Application selected:', applicationId);
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
    if (this.canSubmit()) {
      this.isSubmitting = true;
      const applicationId = this.allocateForm.get('applicationId')?.value;

      // If application is selected, use allocateByApplication
      if (applicationId) {
        const roomId = this.allocateForm.get('roomId')?.value;
        this.allocationService
          .allocateByApplication({
            applicationId: applicationId,
            roomId: roomId,
          })
          .subscribe({
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
      } else {
        // Otherwise, use manual allocation
        const allocationDate = this.allocateForm.get('allocationDate')?.value;
        const formattedDate =
          allocationDate instanceof Date ? allocationDate.toISOString() : allocationDate;

        const request: RoomAllocationRequest = {
          studentId: this.allocateForm.get('studentId')?.value,
          roomId: this.allocateForm.get('roomId')?.value,
          allocationDate: formattedDate,
        };

        this.allocationService.createAllocation(request).subscribe({
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
    }
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
