import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RoomService, RoomResponse, PageResponse } from '../../../core/services/room.service';
import { BuildingService, BuildingResponse } from '../../../core/services/building.service';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatChipsModule } from '@angular/material/chips';

@Component({
  selector: 'app-room-management',
  templateUrl: './room-management.component.html',
  styleUrls: ['./room-management.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatMenuModule,
    MatChipsModule,
  ],
})
export class RoomManagementComponent implements OnInit {
  rooms: RoomResponse[] = [];
  buildings: BuildingResponse[] = [];
  displayedColumns: string[] = ['roomNumber', 'buildingName', 'capacity', 'currentOccupancy', 'status', 'actions'];

  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;

  filterForm: FormGroup;

  statuses = ['AVAILABLE', 'OCCUPIED', 'FULL', 'MAINTENANCE'];

  constructor(
    private roomService: RoomService,
    private buildingService: BuildingService,
    private fb: FormBuilder,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {
    this.filterForm = this.fb.group({
      roomNumber: [''],
      buildingId: [''],
      minCapacity: [''],
      maxCapacity: [''],
    });
  }

  ngOnInit(): void {
    this.loadBuildings();
    this.loadRooms();
  }

  loadBuildings(): void {
    this.buildingService.getAllBuildingsNoPagination().subscribe({
      next: (buildings) => {
        this.buildings = buildings;
      },
      error: (error) => {
        console.error('Failed to load buildings:', error);
      },
    });
  }

  loadRooms(): void {
    this.isLoading = true;
    const filters = {
      ...this.filterForm.value,
      page: this.currentPage,
      size: this.pageSize,
    };

    this.roomService.searchRooms(filters).subscribe({
      next: (response: PageResponse<RoomResponse>) => {
        this.rooms = response.content;
        this.totalElements = response.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load rooms:', error);
        this.snackBar.open('Failed to load rooms', 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  onFilter(): void {
    this.currentPage = 0;
    this.loadRooms();
  }

  onClearFilters(): void {
    this.filterForm.reset();
    this.currentPage = 0;
    this.loadRooms();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadRooms();
  }

  openCreateDialog(): void {
    import('./room-form-dialog/room-form-dialog.component').then(m => {
      const dialogRef = this.dialog.open(m.RoomFormDialogComponent, {
        width: '600px',
        data: { mode: 'create', buildings: this.buildings }
      });

      dialogRef.afterClosed().subscribe(result => {
        if (result) {
          this.loadRooms();
        }
      });
    });
  }

  openEditDialog(room: RoomResponse): void {
    import('./room-form-dialog/room-form-dialog.component').then(m => {
      const dialogRef = this.dialog.open(m.RoomFormDialogComponent, {
        width: '600px',
        data: { mode: 'edit', room, buildings: this.buildings }
      });

      dialogRef.afterClosed().subscribe(result => {
        if (result) {
          this.loadRooms();
        }
      });
    });
  }

  deleteRoom(room: RoomResponse): void {
    if (confirm(`Are you sure you want to delete room "${room.roomNumber}"? This action cannot be undone.`)) {
      this.roomService.deleteRoom(room.id).subscribe({
        next: () => {
          this.snackBar.open('Room deleted successfully', 'Close', { duration: 3000 });
          this.loadRooms();
        },
        error: (error) => {
          console.error('Failed to delete room:', error);
          this.snackBar.open('Failed to delete room. It may have active allocations.', 'Close', { duration: 5000 });
        },
      });
    }
  }

  getStatusClass(status: string): string {
    return status.toLowerCase();
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'AVAILABLE':
        return 'check_circle';
      case 'OCCUPIED':
        return 'people';
      case 'FULL':
        return 'do_not_disturb';
      case 'MAINTENANCE':
        return 'build';
      default:
        return 'help';
    }
  }

  getOccupancyText(room: RoomResponse): string {
    return `${room.currentOccupancy} / ${room.capacity}`;
  }

  getOccupancyClass(room: RoomResponse): string {
    const percentage = (room.currentOccupancy / room.capacity) * 100;
    if (percentage === 0) return 'empty';
    if (percentage < 100) return 'partial';
    return 'full';
  }
}
