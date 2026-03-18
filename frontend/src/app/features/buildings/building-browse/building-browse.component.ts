import {
  Component,
  OnInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BuildingService, BuildingResponse } from '../../../core/services/building.service';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-building-browse',
  templateUrl: './building-browse.component.html',
  styleUrls: ['./building-browse.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
})
export class BuildingBrowseComponent implements OnInit {
  private readonly buildingService = inject(BuildingService);
  private readonly cdr = inject(ChangeDetectorRef);

  buildings: BuildingResponse[] = [];
  filteredBuildings: BuildingResponse[] = [];
  isLoading = false;
  searchQuery = '';
  error: string | null = null;

  ngOnInit(): void {
    this.loadBuildings();
  }

  loadBuildings(): void {
    this.isLoading = true;
    this.error = null;
    this.buildingService.getAllBuildingsNoPagination().subscribe({
      next: (buildings) => {
        this.buildings = buildings;
        this.filteredBuildings = buildings;
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load buildings. Please try again.';
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  onSearch(): void {
    const q = this.searchQuery.toLowerCase().trim();
    if (!q) {
      this.filteredBuildings = this.buildings;
    } else {
      this.filteredBuildings = this.buildings.filter(
        (b) =>
          b.name.toLowerCase().includes(q) ||
          (b.address && b.address.toLowerCase().includes(q)) ||
          (b.description && b.description.toLowerCase().includes(q))
      );
    }
    this.cdr.markForCheck();
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.filteredBuildings = this.buildings;
    this.cdr.markForCheck();
  }
}
