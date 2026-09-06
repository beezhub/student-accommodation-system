import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RoomService, RoomResponse } from '../../../core/services/room.service';
import { BuildingService, BuildingResponse } from '../../../core/services/building.service';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

// Shared Components
import { ModernStatusBadgeComponent } from '../../../shared/modern-status-badge/modern-status-badge.component';

@Component({
  selector: 'app-room-browser',
  templateUrl: './room-browser.component.html',
  styleUrls: ['./room-browser.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatDialogModule,
    MatSnackBarModule,
    ModernStatusBadgeComponent,
  ],
})
export class RoomBrowserComponent implements OnInit {
  rooms: RoomResponse[] = [];
  filteredRooms: RoomResponse[] = [];
  buildings: BuildingResponse[] = [];
  isLoading = false;

  filterForm: FormGroup;

  capacityOptions = [
    { value: 1, label: 'Single (1)' },
    { value: 2, label: 'Double (2)' },
    { value: 3, label: 'Triple (3)' },
    { value: 4, label: 'Quad (4+)' },
  ];

  private roomService = inject(RoomService);
  private buildingService = inject(BuildingService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  constructor() {
    this.filterForm = this.fb.group({
      buildingId: [''],
      capacity: [''],
    });
  }

  ngOnInit(): void {
    this.loadBuildings();
    this.loadRooms();

    this.filterForm.valueChanges.subscribe(() => {
      this.applyFilters();
    });
  }

  loadBuildings(): void {
    this.buildingService.getAllBuildingsNoPagination().subscribe({
      next: (buildings) => {
        this.buildings = buildings;
      },
      error: (error) => {
        console.error('Failed to load buildings:', error);
        this.snackBar.open('Could not load buildings for filtering.', 'Close', {
          duration: 3000,
          panelClass: ['error-snackbar'],
        });
      },
    });
  }

  pageIndex = 0;
  pageSize = 25;
  totalRooms = 0;

  loadRooms(): void {
    this.isLoading = true;
    // 25, not 1000: this feeds a browsable list, and asking for a thousand rows both moved a
    // large payload and truncated silently past it (GH-157).
    this.roomService.searchRooms({ page: this.pageIndex, size: this.pageSize }).subscribe({
      next: (response) => {
        // Only show available or partially occupied rooms
        this.rooms = response.content.filter(
          (room) =>
            room.status === 'AVAILABLE' ||
            (room.status === 'OCCUPIED' && room.currentOccupancy < room.capacity)
        );
        this.totalRooms = response.totalElements;
        this.applyFilters();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load rooms:', error);
        this.snackBar.open('Failed to load rooms', 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  applyFilters(): void {
    const { buildingId, capacity } = this.filterForm.value;

    this.filteredRooms = this.rooms.filter((room) => {
      let matches = true;

      if (buildingId) {
        matches = matches && room.buildingId === buildingId;
      }

      if (capacity) {
        if (capacity === 4) {
          matches = matches && room.capacity >= 4;
        } else {
          matches = matches && room.capacity === capacity;
        }
      }

      return matches;
    });
  }

  getAvailableSpots(room: RoomResponse): number {
    return room.capacity - room.currentOccupancy;
  }

  getOccupancyPercentage(room: RoomResponse): number {
    return (room.currentOccupancy / room.capacity) * 100;
  }

  getOccupancyClass(room: RoomResponse): string {
    const percentage = this.getOccupancyPercentage(room);
    if (percentage === 0) return 'empty';
    if (percentage < 50) return 'low';
    if (percentage < 100) return 'medium';
    return 'full';
  }

  getStatusColor(room: RoomResponse): string {
    if (room.status === 'AVAILABLE') return 'primary';
    if (room.status === 'OCCUPIED') return 'accent';
    return 'warn';
  }

  openRoomDetails(room: RoomResponse): void {
    import('./room-details-dialog/room-details-dialog.component').then((m) => {
      this.dialog.open(m.RoomDetailsDialogComponent, {
        width: '600px',
        data: { room },
      });
    });
  }

  clearFilters(): void {
    this.filterForm.reset();
  }
}
