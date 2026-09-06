import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
  RoomAllocationService,
  RoomAllocationResponse,
  PageResponse,
} from '../../../core/services/room-allocation.service';
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
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { ModernStatusBadgeComponent } from '../../../shared/modern-status-badge/modern-status-badge.component';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';

@Component({
  selector: 'app-room-allocation',
  templateUrl: './room-allocation.component.html',
  styleUrls: ['./room-allocation.component.scss'],
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
    ModernStatusBadgeComponent,
    SkeletonLoaderComponent,
    EmptyStateComponent,
  ],
})
export class RoomAllocationComponent implements OnInit {
  private allocationService = inject(RoomAllocationService);
  private buildingService = inject(BuildingService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  allocations: RoomAllocationResponse[] = [];
  buildings: BuildingResponse[] = [];
  displayedColumns: string[] = [
    'studentName',
    'roomNumber',
    'buildingName',
    'allocationDate',
    'status',
    'actions',
  ];

  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;

  // Filter panel state
  filtersVisible = false;

  filterForm: FormGroup;

  statuses = [
    'OFFERED',
    'ACCEPTED',
    'DECLINED',
    'EXPIRED',
    'CHECK_IN_EXPIRED',
    'ACTIVE',
    'COMPLETED',
    // Ending a lease early now writes TERMINATED rather than COMPLETED (GH-138).
    'TERMINATED',
    'CANCELLED',
  ];

  constructor() {
    this.filterForm = this.fb.group({
      studentName: [''],
      roomNumber: [''],
      buildingId: [''],
      status: [''],
    });
  }

  ngOnInit(): void {
    this.loadBuildings();
    this.loadAllocations();
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

  loadAllocations(): void {
    this.isLoading = true;
    const filters = {
      ...this.filterForm.value,
      page: this.currentPage,
      size: this.pageSize,
    };

    this.allocationService.searchAllocations(filters).subscribe({
      next: (response: PageResponse<RoomAllocationResponse>) => {
        this.allocations = response.content;
        this.totalElements = response.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load allocations:', error);
        this.snackBar.open('Failed to load allocations', 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  onFilter(): void {
    this.currentPage = 0;
    this.loadAllocations();
  }

  onClearFilters(): void {
    this.filterForm.reset();
    this.currentPage = 0;
    this.loadAllocations();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadAllocations();
  }

  openAllocateDialog(): void {
    import('./allocate-dialog/allocate-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.AllocateDialogComponent, {
        width: '700px',
        data: {},
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.loadAllocations();
        }
      });
    });
  }

  moveStudent(allocation: RoomAllocationResponse): void {
    import('../allocation-move-dialog/allocation-move-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.AllocationMoveDialogComponent, {
        width: '600px',
        data: {
          allocationId: allocation.id,
          studentId: allocation.studentId,
          studentName: `${allocation.studentFirstName} ${allocation.studentLastName}`,
          currentRoom: {
            buildingName: allocation.buildingName,
            roomNumber: allocation.roomNumber,
          },
        },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.loadAllocations();
        }
      });
    });
  }

  endAllocation(allocation: RoomAllocationResponse): void {
    if (
      confirm(
        `Are you sure you want to end the allocation for ${allocation.studentFirstName} ${allocation.studentLastName}?`
      )
    ) {
      this.allocationService.endAllocation(allocation.id).subscribe({
        next: () => {
          this.snackBar.open('Allocation ended successfully', 'Close', { duration: 3000 });
          this.loadAllocations();
        },
        error: (error) => {
          console.error('Failed to end allocation:', error);
          this.snackBar.open('Failed to end allocation', 'Close', { duration: 5000 });
        },
      });
    }
  }

  deleteAllocation(allocation: RoomAllocationResponse): void {
    if (confirm(`Are you sure you want to delete this allocation? This action cannot be undone.`)) {
      this.allocationService.deleteAllocation(allocation.id).subscribe({
        next: () => {
          this.snackBar.open('Allocation deleted successfully', 'Close', { duration: 3000 });
          this.loadAllocations();
        },
        error: (error) => {
          console.error('Failed to delete allocation:', error);
          this.snackBar.open('Failed to delete allocation', 'Close', { duration: 5000 });
        },
      });
    }
  }

  checkIn(allocation: RoomAllocationResponse): void {
    if (
      confirm(`Confirm check-in for ${allocation.studentFirstName} ${allocation.studentLastName}?`)
    ) {
      this.allocationService.checkIn(allocation.id).subscribe({
        next: () => {
          this.snackBar.open('Check-in confirmed successfully', 'Close', { duration: 3000 });
          this.loadAllocations();
        },
        error: (error) => {
          console.error('Failed to confirm check-in:', error);
          this.snackBar.open('Failed to confirm check-in', 'Close', { duration: 5000 });
        },
      });
    }
  }

  checkOut(allocation: RoomAllocationResponse): void {
    if (
      confirm(`Confirm check-out for ${allocation.studentFirstName} ${allocation.studentLastName}?`)
    ) {
      this.allocationService.checkOut(allocation.id).subscribe({
        next: () => {
          this.snackBar.open('Check-out confirmed successfully', 'Close', { duration: 3000 });
          this.loadAllocations();
        },
        error: (error) => {
          console.error('Failed to confirm check-out:', error);
          this.snackBar.open('Failed to confirm check-out', 'Close', { duration: 5000 });
        },
      });
    }
  }

  // ── UI helpers ──────────────────────────────────────────────

  get activeFilterCount(): number {
    const v = this.filterForm.value;
    return [v.studentName, v.roomNumber, v.buildingId, v.status].filter(
      (x) => x !== null && x !== '' && x !== undefined
    ).length;
  }

  toggleFilters(): void {
    this.filtersVisible = !this.filtersVisible;
  }

  getInitials(allocation: RoomAllocationResponse): string {
    const f = allocation.studentFirstName?.[0] ?? '';
    const l = allocation.studentLastName?.[0] ?? '';
    return (f + l).toUpperCase();
  }

  getStatusClass(status: string): string {
    return status.toLowerCase();
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'check_circle';
      case 'COMPLETED':
        return 'done_all';
      case 'TERMINATED':
        return 'gavel';
      case 'CANCELLED':
        return 'cancel';
      case 'EXPIRED':
        return 'timer_off';
      case 'CHECK_IN_EXPIRED':
        return 'event_busy';
      default:
        return 'help';
    }
  }
}
