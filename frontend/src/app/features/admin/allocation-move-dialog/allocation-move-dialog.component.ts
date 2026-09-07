import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';

import { RoomService, EligibleRoomResponse } from '../../../core/services/room.service';
import { BuildingService, BuildingResponse } from '../../../core/services/building.service';
import { StagedAllocationService } from '../../../core/services/staged-allocation.service';

export interface AllocationMoveDialogData {
  allocationId: number;
  studentId: number;
  studentName: string;
  currentRoom: {
    buildingName: string;
    roomNumber: string;
  };
}

@Component({
  selector: 'app-allocation-move-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatCardModule,
    MatDividerModule,
  ],
  templateUrl: './allocation-move-dialog.component.html',
  styleUrls: ['./allocation-move-dialog.component.scss'],
})
export class AllocationMoveDialogComponent implements OnInit {
  private fb = inject(FormBuilder);
  private dialogRef = inject<MatDialogRef<AllocationMoveDialogComponent>>(MatDialogRef);
  data = inject<AllocationMoveDialogData>(MAT_DIALOG_DATA);
  private roomService = inject(RoomService);
  private buildingService = inject(BuildingService);
  private stagedAllocationService = inject(StagedAllocationService);
  private snackBar = inject(MatSnackBar);

  searchForm: FormGroup;

  buildings: BuildingResponse[] = [];
  rooms: EligibleRoomResponse[] = [];
  selectedRoom: EligibleRoomResponse | null = null;

  isLoadingRooms = false;
  isMoving = false;

  displayedColumns: string[] = ['roomNumber', 'building', 'capacity', 'actions'];

  constructor() {
    this.searchForm = this.fb.group({
      buildingId: [''],
      roomNumber: [''],
    });
  }

  ngOnInit(): void {
    this.loadBuildings();
  }

  loadBuildings(): void {
    this.buildingService.getAllBuildingsNoPagination().subscribe({
      next: (data) => (this.buildings = data),
      error: (err) => console.error('Failed to load buildings', err),
    });
  }

  searchRooms(): void {
    this.isLoadingRooms = true;
    this.selectedRoom = null; // Reset selection on new search
    const filters = this.searchForm.value;

    // Use getEligibleRoomsForStudent to only show rooms that meet student's constraints
    this.roomService
      .getEligibleRoomsForStudent(this.data.studentId, filters.buildingId || undefined)
      .subscribe({
        next: (eligibleRooms) => {
          // Filter by room number if specified
          this.rooms = filters.roomNumber
            ? eligibleRooms.filter((r) =>
                r.roomNumber.toLowerCase().includes(filters.roomNumber.toLowerCase())
              )
            : eligibleRooms;
          this.isLoadingRooms = false;
        },
        error: (err) => {
          console.error('Failed to load eligible rooms', err);
          this.isLoadingRooms = false;
          this.snackBar.open('Error loading eligible rooms', 'Close', { duration: 3000 });
        },
      });
  }

  selectRoom(room: EligibleRoomResponse): void {
    this.selectedRoom = room;
  }

  confirmMove(): void {
    if (!this.selectedRoom) {
      return;
    }

    this.isMoving = true;

    this.stagedAllocationService
      .moveStudent(this.data.allocationId, this.selectedRoom.roomId, 'Manual Move')
      .subscribe({
        next: () => {
          this.snackBar.open('Student moved successfully', 'Close', { duration: 3000 });
          this.dialogRef.close(true);
        },
        error: (err) => {
          console.error('Move failed', err);
          this.isMoving = false;
          const errorMessage =
            err.error?.message || err.error?.error || 'Failed to move student. Please try again.';
          this.snackBar.open(errorMessage, 'Close', { duration: 5000 });
        },
      });
  }
}
