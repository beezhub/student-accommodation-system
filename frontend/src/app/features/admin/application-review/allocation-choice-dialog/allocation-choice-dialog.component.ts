import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';

import { RoomService, EligibleRoomResponse } from '../../../../core/services/room.service';
import { BuildingService, BuildingResponse } from '../../../../core/services/building.service';
import { StagedAllocationService } from '../../../../core/services/staged-allocation.service';
import { ApplicationService } from '../../../../core/services/application.service';

export interface AllocationChoiceDialogData {
  applicationId: number;
  studentId: number;
}

@Component({
  selector: 'app-allocation-choice-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
    MatChipsModule,
  ],
  templateUrl: './allocation-choice-dialog.component.html',
  styleUrls: ['./allocation-choice-dialog.component.css'],
})
export class AllocationChoiceDialogComponent implements OnInit {
  isLoading = false;

  // Room Selection State
  searchForm: FormGroup;
  buildings: BuildingResponse[] = [];
  eligibleRooms: EligibleRoomResponse[] = [];
  displayedColumns: string[] = ['roomNumber', 'building', 'capacity', 'restrictions', 'actions'];
  isAllocating = false;
  isAddingToWaitingList = false;

  studentId: number | null = null;
  error: string | null = null;
  showEligibleOnly = true;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<AllocationChoiceDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AllocationChoiceDialogData,
    private snackBar: MatSnackBar,
    private roomService: RoomService,
    private buildingService: BuildingService,
    private applicationService: ApplicationService,
    private stagedAllocationService: StagedAllocationService
  ) {
    this.searchForm = this.fb.group({
      buildingId: [''],
      roomNumber: [''],
    });

    // Initialize studentId from data
    if (data && data.studentId) {
      this.studentId = data.studentId;
    }
  }

  ngOnInit(): void {
    this.loadBuildings();

    // If studentId is missing, try to fetch it from application details
    if (!this.studentId && this.data.applicationId) {
      this.isLoading = true;
      this.applicationService.getApplicationById(this.data.applicationId).subscribe({
        next: (app) => {
          if (app.studentId) {
            this.studentId = app.studentId;
            this.loadEligibleRooms();
          } else {
            this.error = 'Could not determine student ID for this application.';
          }
          this.isLoading = false;
        },
        error: () => {
          this.error = 'Failed to load application details.';
          this.isLoading = false;
        },
      });
    } else if (this.studentId) {
      this.loadEligibleRooms();
    }
  }

  loadBuildings(): void {
    this.buildingService.getAllBuildingsNoPagination().subscribe({
      next: (data) => (this.buildings = data),
      error: (err) => console.error('Failed to load buildings', err),
    });
  }

  loadEligibleRooms(): void {
    if (!this.studentId) return;

    this.isLoading = true;
    const buildingId = this.searchForm.get('buildingId')?.value || undefined;

    this.roomService.getEligibleRoomsForStudent(this.studentId, buildingId).subscribe({
      next: (rooms) => {
        this.eligibleRooms = rooms;
        // Apply room number filter if specified
        const roomFilter = this.searchForm.get('roomNumber')?.value?.toLowerCase();
        if (roomFilter) {
          this.eligibleRooms = rooms.filter((r) => r.roomNumber.toLowerCase().includes(roomFilter));
        }
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load eligible rooms', err);
        this.snackBar.open('Failed to load eligible rooms.', 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  onSearch(): void {
    this.loadEligibleRooms();
  }

  addToWaitingList(): void {
    if (!this.studentId || !this.data.applicationId) {
      this.snackBar.open(
        'Missing student or application ID. Cannot add to waiting list.',
        'Close',
        { duration: 3000 }
      );
      return;
    }

    this.isAddingToWaitingList = true;
    this.stagedAllocationService
      .enqueueWaitingList(this.studentId, this.data.applicationId)
      .subscribe({
        next: (res) => {
          this.isAddingToWaitingList = false;
          this.snackBar.open(res.message || 'Student added to waiting list.', 'Close', {
            duration: 3000,
          });
          this.dialogRef.close({ action: 'waitlisted' });
        },
        error: () => {
          this.isAddingToWaitingList = false;
          this.snackBar.open('Failed to add student to waiting list.', 'Close', { duration: 3000 });
        },
      });
  }

  selectRoom(room: EligibleRoomResponse): void {
    if (!this.studentId) {
      this.snackBar.open('Error: Missing student ID. Cannot allocate.', 'Close');
      return;
    }

    this.isAllocating = true;
    this.stagedAllocationService
      .overrideConstraints(this.studentId, room.roomId, 'Manual Selection', [])
      .subscribe({
        next: (stagedAllocation) => {
          this.stagedAllocationService.confirmAllocation(stagedAllocation.id).subscribe({
            next: () => {
              this.isAllocating = false;
              this.dialogRef.close({ action: 'allocated' });
              this.snackBar.open('Room assigned successfully!', 'Close', { duration: 3000 });
            },
            error: () => {
              this.isAllocating = false;
              this.snackBar.open('Failed to confirm assignment.', 'Close', { duration: 3000 });
            },
          });
        },
        error: () => {
          this.isAllocating = false;
          this.snackBar.open('Failed to assign room. It might be full or restricted.', 'Close', {
            duration: 3000,
          });
        },
      });
  }

  getYearRestrictionLabel(room: EligibleRoomResponse): string {
    const attrs = room as any;
    if (!attrs.minYearOfStudy && !attrs.maxYearOfStudy) return '';
    if (attrs.minYearOfStudy && attrs.maxYearOfStudy) {
      return `Year ${attrs.minYearOfStudy}-${attrs.maxYearOfStudy}`;
    }
    if (attrs.minYearOfStudy) return `Year ${attrs.minYearOfStudy}+`;
    return `Year 1-${attrs.maxYearOfStudy}`;
  }
}
