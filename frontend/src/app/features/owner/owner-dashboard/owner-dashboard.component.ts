import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { OwnerPortalService, OwnerBuildingSummary } from '../owner-portal.service';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';

@Component({
  selector: 'app-owner-dashboard',
  templateUrl: './owner-dashboard.component.html',
  styleUrls: ['./owner-dashboard.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatSnackBarModule,
    SkeletonLoaderComponent,
    EmptyStateComponent,
  ],
})
export class OwnerDashboardComponent implements OnInit {
  private portalService = inject(OwnerPortalService);
  private snackBar = inject(MatSnackBar);

  buildings: OwnerBuildingSummary[] = [];
  isLoading = true;

  ngOnInit(): void {
    this.loadBuildings();
  }

  loadBuildings(): void {
    this.isLoading = true;
    this.portalService.getMyBuildings().subscribe({
      next: (data) => {
        this.buildings = data;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.snackBar.open('Failed to load buildings.', 'Close', {
          duration: 4000,
          panelClass: ['error-snackbar'],
        });
      },
    });
  }

  occupancyPercent(building: OwnerBuildingSummary): number {
    if (!building.totalRooms) return 0;
    return Math.round((building.occupiedRooms / building.totalRooms) * 100);
  }
}
