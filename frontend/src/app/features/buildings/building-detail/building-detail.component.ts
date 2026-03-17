import {
  Component,
  OnInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import {
  BuildingService,
  BuildingResponse,
  BuildingImageResponse,
} from '../../../core/services/building.service';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';

@Component({
  selector: 'app-building-detail',
  templateUrl: './building-detail.component.html',
  styleUrls: ['./building-detail.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
  ],
})
export class BuildingDetailComponent implements OnInit {
  private readonly buildingService = inject(BuildingService);
  private readonly route = inject(ActivatedRoute);
  private readonly cdr = inject(ChangeDetectorRef);

  building: BuildingResponse | null = null;
  images: BuildingImageResponse[] = [];
  activeImageIndex = 0;
  isLoading = false;
  error: string | null = null;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadBuilding(id);
    }
  }

  loadBuilding(id: number): void {
    this.isLoading = true;
    this.error = null;
    this.buildingService.getBuildingById(id).subscribe({
      next: (building) => {
        this.building = building;
        this.images = building.images ?? [];
        this.activeImageIndex = 0;
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load building details. Please try again.';
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  get activeImage(): BuildingImageResponse | null {
    return this.images[this.activeImageIndex] ?? null;
  }

  prevImage(): void {
    if (this.activeImageIndex > 0) {
      this.activeImageIndex--;
      this.cdr.markForCheck();
    }
  }

  nextImage(): void {
    if (this.activeImageIndex < this.images.length - 1) {
      this.activeImageIndex++;
      this.cdr.markForCheck();
    }
  }

  selectImage(index: number): void {
    this.activeImageIndex = index;
    this.cdr.markForCheck();
  }
}
