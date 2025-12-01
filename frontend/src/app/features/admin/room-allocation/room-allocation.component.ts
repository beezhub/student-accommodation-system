import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RoomAllocationService, RoomAllocationResponse, PageResponse } from '../../../core/services/room-allocation.service';
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
  selector: 'app-room-allocation',
  templateUrl: './room-allocation.component.html',
  styleUrls: ['./room-allocation.component.css'],
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
export class RoomAllocationComponent implements OnInit {
  allocations: RoomAllocationResponse[] = [];
  buildings: BuildingResponse[] = [];
  displayedColumns: string[] = ['studentName', 'studentEmail', 'roomNumber', 'buildingName', 'allocationDate', 'status', 'actions'];

  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;

  filterForm: FormGroup;

  statuses = ['ACTIVE', 'COMPLETED', 'CANCELLED'];

  constructor(
    private allocationService: RoomAllocationService,
    private buildingService: BuildingService,
    private fb: FormBuilder,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {
    this.filterForm = this.fb.group({
      studentId: [''],
      roomId: [''],
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
    import('./allocate-dialog/allocate-dialog.component').then(m => {
      const dialogRef = this.dialog.open(m.AllocateDialogComponent, {
        width: '700px',
        data: {}
      });

      dialogRef.afterClosed().subscribe(result => {
        if (result) {
          this.loadAllocations();
        }
      });
    });
  }

  endAllocation(allocation: RoomAllocationResponse): void {
    if (confirm(`Are you sure you want to end the allocation for ${allocation.studentFirstName} ${allocation.studentLastName}?`)) {
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

  getStatusClass(status: string): string {
    return status.toLowerCase();
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'check_circle';
      case 'COMPLETED':
        return 'done_all';
      case 'CANCELLED':
        return 'cancel';
      default:
        return 'help';
    }
  }
}
