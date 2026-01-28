import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogRef,
  MatDialogModule,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';

import { RoomService, RoomResponse } from '../../../core/services/room.service';
import { BuildingService, BuildingResponse } from '../../../core/services/building.service';
import { StagedAllocationService } from '../../../core/services/staged-allocation.service';

export interface AllocationMoveDialogData {
  allocationId: number;
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
    MatCardModule
  ],
  templateUrl: './allocation-move-dialog.component.html',
  styleUrls: ['./allocation-move-dialog.component.css']
})
export class AllocationMoveDialogComponent implements OnInit {
  searchForm: FormGroup;
  reasonForm: FormGroup;
  
  buildings: BuildingResponse[] = [];
  rooms: RoomResponse[] = [];
  selectedRoom: RoomResponse | null = null;
  
  isLoadingRooms = false;
  isMoving = false;
  
  displayedColumns: string[] = ['roomNumber', 'building', 'capacity', 'actions'];

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<AllocationMoveDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AllocationMoveDialogData,
    private roomService: RoomService,
    private buildingService: BuildingService,
    private stagedAllocationService: StagedAllocationService,
    private snackBar: MatSnackBar
  ) {
    this.searchForm = this.fb.group({
      buildingId: [''],
      roomNumber: ['']
    });

    this.reasonForm = this.fb.group({
      reason: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]]
    });
  }

  ngOnInit(): void {
    this.loadBuildings();
  }

  loadBuildings(): void {
    this.buildingService.getAllBuildingsNoPagination().subscribe({
      next: (data) => this.buildings = data,
      error: (err) => console.error('Failed to load buildings', err)
    });
  }

  searchRooms(): void {
    this.isLoadingRooms = true;
    this.selectedRoom = null; // Reset selection on new search
    const filters = this.searchForm.value;
    
    this.roomService.searchRooms({
      buildingId: filters.buildingId || undefined,
      roomNumber: filters.roomNumber || undefined,
      page: 0,
      size: 20
    }).subscribe({
      next: (page) => {
        // Client-side filter for availability (if backend doesn't support 'availableOnly' yet)
        // Ideally backend should handle this, but for now we filter what we can
        this.rooms = page.content.filter(r => r.currentOccupancy < r.capacity);
        this.isLoadingRooms = false;
      },
      error: (err) => {
        console.error('Failed to search rooms', err);
        this.isLoadingRooms = false;
        this.snackBar.open('Error searching rooms', 'Close', { duration: 3000 });
      }
    });
  }

  selectRoom(room: RoomResponse): void {
    this.selectedRoom = room;
  }

  confirmMove(): void {
    if (!this.selectedRoom || this.reasonForm.invalid) return;
    
    this.isMoving = true;
    const reason = this.reasonForm.get('reason')?.value;

    this.stagedAllocationService.moveStudent(
      this.data.allocationId,
      this.selectedRoom.id,
      reason
    ).subscribe({
      next: (result) => {
        this.snackBar.open('Student moved successfully', 'Close', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: (err) => {
        console.error('Move failed', err);
        this.isMoving = false;
        this.snackBar.open('Failed to move student. Please try again.', 'Close', { duration: 5000 });
      }
    });
  }
}
