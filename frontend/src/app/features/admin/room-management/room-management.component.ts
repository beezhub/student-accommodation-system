import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';

import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RoomService, RoomResponse, PageResponse } from '../../../core/services/room.service';
import { BuildingService, BuildingResponse } from '../../../core/services/building.service';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';

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
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { SelectionModel } from '@angular/cdk/collections';

// Modern UI Components
import { ModernStatusBadgeComponent } from '../../../shared/modern-status-badge/modern-status-badge.component';

@Component({
  selector: 'app-room-management',
  templateUrl: './room-management.component.html',
  styleUrls: ['./room-management.component.scss'],
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
    MatDividerModule,
    MatChipsModule,
    MatCheckboxModule,
    ModernStatusBadgeComponent,
    SkeletonLoaderComponent,
    EmptyStateComponent,
  ],
})
export class RoomManagementComponent implements OnInit {
  private roomService = inject(RoomService);
  private buildingService = inject(BuildingService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  rooms: RoomResponse[] = [];
  buildings: BuildingResponse[] = [];
  displayedColumns: string[] = [
    'select',
    'roomNumber',
    'buildingName',
    'capacity',
    'monthlyRate',
    'currentOccupancy',
    'attributes',
    'status',
    'actions',
  ];

  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;

  filtersVisible = false;

  filterForm: FormGroup;

  statuses = ['AVAILABLE', 'OCCUPIED', 'FULL', 'MAINTENANCE'];

  // Selection model for bulk operations
  selection = new SelectionModel<RoomResponse>(true, []);

  constructor() {
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
    this.selection.clear(); // Clear selection when loading new data
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
    import('./room-form-dialog/room-form-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.RoomFormDialogComponent, {
        width: '600px',
        data: { mode: 'create', buildings: this.buildings },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.loadRooms();
        }
      });
    });
  }

  openBulkCreateDialog(): void {
    import('./bulk-room-form-dialog/bulk-room-form-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.BulkRoomFormDialogComponent, {
        width: '600px',
        maxWidth: '95vw',
        data: { buildings: this.buildings },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.loadRooms();
        }
      });
    });
  }

  openEditDialog(room: RoomResponse): void {
    import('./room-form-dialog/room-form-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.RoomFormDialogComponent, {
        width: '600px',
        data: { mode: 'edit', room, buildings: this.buildings },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.loadRooms();
        }
      });
    });
  }

  deleteRoom(room: RoomResponse): void {
    if (
      confirm(
        `Are you sure you want to delete room "${room.roomNumber}"? This action cannot be undone.`
      )
    ) {
      this.roomService.deleteRoom(room.id).subscribe({
        next: () => {
          this.snackBar.open('Room deleted successfully', 'Close', { duration: 3000 });
          this.loadRooms();
        },
        error: (error) => {
          console.error('Failed to delete room:', error);
          this.snackBar.open('Failed to delete room. It may have active allocations.', 'Close', {
            duration: 5000,
          });
        },
      });
    }
  }

  openAttributesDialog(room: RoomResponse): void {
    import('./room-attributes-dialog/room-attributes-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.RoomAttributesDialogComponent, {
        width: '95vw',
        maxWidth: '550px',
        data: {
          roomId: room.id,
          roomNumber: room.roomNumber,
          buildingName: room.buildingName,
        },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          // Refresh rooms data to show updated attributes
          this.loadRooms();
        }
      });
    });
  }

  // ── UI helpers ──────────────────────────────────────────────

  get activeFilterCount(): number {
    const v = this.filterForm.value;
    return [v.roomNumber, v.buildingId, v.minCapacity, v.maxCapacity].filter(
      (x) => x !== null && x !== '' && x !== undefined
    ).length;
  }

  toggleFilters(): void {
    this.filtersVisible = !this.filtersVisible;
  }

  getOccupancyText(room: RoomResponse): string {
    if (!room || room.currentOccupancy === undefined || room.capacity === undefined) {
      return '- / -';
    }
    return `${room.currentOccupancy} / ${room.capacity}`;
  }

  getOccupancyClass(room: RoomResponse): string {
    if (
      !room ||
      room.currentOccupancy === undefined ||
      room.capacity === undefined ||
      room.capacity === 0
    ) {
      return 'empty';
    }
    const percentage = (room.currentOccupancy / room.capacity) * 100;
    if (percentage === 0) return 'empty';
    if (percentage < 100) return 'partial';
    return 'full';
  }

  getYearRestrictionTooltip(room: RoomResponse): string {
    const min = room.minYearOfStudy;
    const max = room.maxYearOfStudy;

    if (min && max) {
      return `Year ${min} - ${max} students only`;
    } else if (min) {
      return `Year ${min}+ students only`;
    } else if (max) {
      return `Up to Year ${max} students only`;
    }
    return 'Year of Study Restriction';
  }

  // Selection methods for bulk operations
  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.rooms.length;
    return numSelected === numRows && numRows > 0;
  }

  isSomeSelected(): boolean {
    return this.selection.selected.length > 0 && !this.isAllSelected();
  }

  toggleAllRows(): void {
    if (this.isAllSelected()) {
      this.selection.clear();
    } else {
      this.rooms.forEach((row) => this.selection.select(row));
    }
  }

  clearSelection(): void {
    this.selection.clear();
  }

  openBulkAttributesDialog(): void {
    if (this.selection.selected.length === 0) {
      this.snackBar.open('Please select at least one room', 'Close', { duration: 3000 });
      return;
    }

    import('./bulk-attributes-dialog/bulk-attributes-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.BulkAttributesDialogComponent, {
        width: '95vw',
        maxWidth: '600px',
        data: {
          rooms: this.selection.selected.map((room) => ({
            id: room.id,
            roomNumber: room.roomNumber,
            buildingName: room.buildingName,
          })),
        },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.selection.clear();
          this.loadRooms();
        }
      });
    });
  }
}
