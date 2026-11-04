import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
  BuildingService,
  BuildingResponse,
  PageResponse,
} from '../../../core/services/building.service';
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
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { confirmAction } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-building-management',
  templateUrl: './building-management.component.html',
  styleUrls: ['./building-management.component.scss'],
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
    MatDialogModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatMenuModule,
    MatDividerModule,
    SkeletonLoaderComponent,
    EmptyStateComponent,
  ],
})
export class BuildingManagementComponent implements OnInit {
  private buildingService = inject(BuildingService);
  private fb = inject(FormBuilder);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
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

  filtersVisible = false;

  filterForm: FormGroup;

  constructor() {
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
    confirmAction(this.dialog, {
      title: 'Delete this building?',
      message: `"${building.name}" is removed permanently. This cannot be undone.`,
      confirmLabel: 'Delete',
      destructive: true,
    }).subscribe((confirmed) => {
      if (!confirmed) return;
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
    });
  }

  // ── UI helpers ──────────────────────────────────────────────

  get activeFilterCount(): number {
    const v = this.filterForm.value;
    return [v.name, v.address].filter((x) => x !== null && x !== '' && x !== undefined).length;
  }

  toggleFilters(): void {
    this.filtersVisible = !this.filtersVisible;
  }

  getBuildingInitials(building: BuildingResponse): string {
    const words = (building.name ?? '').trim().split(/\s+/);
    const f = words[0]?.[0] ?? '';
    const l = words[1]?.[0] ?? '';
    return (f + l).toUpperCase();
  }

  async openAccessibilityDialog(building: BuildingResponse): Promise<void> {
    const m = await import(
      './building-accessibility-dialog/building-accessibility-dialog.component'
    );
    this.dialog.open(m.BuildingAccessibilityDialogComponent, {
      width: '560px',
      maxWidth: '95vw',
      data: { building },
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
