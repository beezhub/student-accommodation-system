import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import {
  BuildingService,
  BuildingResponse,
  BuildingRequest,
  PageResponse,
} from '../../../core/services/building.service';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';

@Component({
  selector: 'app-building-management',
  templateUrl: './building-management.component.html',
  styleUrls: ['./building-management.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatMenuModule,
    SkeletonLoaderComponent,
  ],
})
export class BuildingManagementComponent implements OnInit {
  buildings: BuildingResponse[] = [];
  displayedColumns: string[] = [
    'name',
    'address',
    'institutions',
    'totalFloors',
    'createdAt',
    'actions',
  ];

  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;

  filterForm: FormGroup;

  constructor(
    private buildingService: BuildingService,
    private fb: FormBuilder,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private router: Router
  ) {
    this.filterForm = this.fb.group({
      name: [''],
      address: [''],
    });
  }

  ngOnInit(): void {
    this.loadBuildings();
  }

  loadBuildings(): void {
    this.isLoading = true;
    const filters = {
      ...this.filterForm.value,
      page: this.currentPage,
      size: this.pageSize,
    };

    this.buildingService.searchBuildings(filters).subscribe({
      next: (response: PageResponse<BuildingResponse>) => {
        this.buildings = response.content;
        this.totalElements = response.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load buildings:', error);
        this.snackBar.open('Failed to load buildings', 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  onFilter(): void {
    this.currentPage = 0;
    this.loadBuildings();
  }

  onClearFilters(): void {
    this.filterForm.reset();
    this.currentPage = 0;
    this.loadBuildings();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadBuildings();
  }

  openCreateDialog(): void {
    import('./building-form-dialog/building-form-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.BuildingFormDialogComponent, {
        width: '600px',
        maxWidth: '95vw',
        data: { mode: 'create' },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.loadBuildings();
        }
      });
    });
  }

  openEditDialog(building: BuildingResponse): void {
    import('./building-form-dialog/building-form-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.BuildingFormDialogComponent, {
        width: '600px',
        maxWidth: '95vw',
        data: { mode: 'edit', building },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.loadBuildings();
        }
      });
    });
  }

  deleteBuilding(building: BuildingResponse): void {
    if (
      confirm(
        `Are you sure you want to delete building "${building.name}"? This action cannot be undone.`
      )
    ) {
      this.buildingService.deleteBuilding(building.id).subscribe({
        next: () => {
          this.snackBar.open('Building deleted successfully', 'Close', { duration: 3000 });
          this.loadBuildings();
        },
        error: (error) => {
          console.error('Failed to delete building:', error);
          this.snackBar.open('Failed to delete building. It may have associated rooms.', 'Close', {
            duration: 5000,
          });
        },
      });
    }
  }

  navigateToAllocationRules(buildingId: number): void {
    // Navigate to allocation rules page with the building pre-selected
    this.router.navigate(['/admin/allocation-rules'], {
      queryParams: { buildingId: buildingId },
    });
  }

  openImageDialog(building: BuildingResponse): void {
    import('./building-image-dialog/building-image-dialog.component').then((m) => {
      const dialogRef = this.dialog.open(m.BuildingImageDialogComponent, {
        width: '800px',
        maxWidth: '95vw',
        data: { building },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.loadBuildings();
        }
      });
    });
  }
}
