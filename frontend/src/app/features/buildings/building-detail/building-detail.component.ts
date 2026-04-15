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
  RoomSummary,
} from '../../../core/services/building.service';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';

export interface CapacityGroup {
  label: string;
  capacity: number;
  total: number;
  available: number;
  minPrice: number | null;
  maxPrice: number | null;
}

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

  // Computed from rooms
  capacityGroups: CapacityGroup[] = [];
  accessibilityFeatures: string[] = [];
  totalRooms = 0;
  availableRooms = 0;

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
        this.computeRoomData(building);
        this.parseAccessibilityFeatures(building);
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

  // ── Room Data Computation ─────────────────────────────────

  private computeRoomData(building: BuildingResponse): void {
    const rooms = building.rooms ?? [];
    this.totalRooms = rooms.length;
    this.availableRooms = rooms.filter((r) => r.status === 'AVAILABLE').length;

    // Group by capacity
    const groupMap = new Map<number, RoomSummary[]>();
    rooms.forEach((room) => {
      const existing = groupMap.get(room.capacity) ?? [];
      existing.push(room);
      groupMap.set(room.capacity, existing);
    });

    this.capacityGroups = [...groupMap.entries()]
      .sort(([a], [b]) => a - b)
      .map(([capacity, groupRooms]) => {
        const pricedRooms = groupRooms.filter((r) => r.monthlyRate != null);
        const prices = pricedRooms.map((r) => r.monthlyRate!);
        return {
          label: this.capacityLabel(capacity),
          capacity,
          total: groupRooms.length,
          available: groupRooms.filter((r) => r.status === 'AVAILABLE').length,
          minPrice: prices.length > 0 ? Math.min(...prices) : null,
          maxPrice: prices.length > 0 ? Math.max(...prices) : null,
        };
      });
  }

  private parseAccessibilityFeatures(building: BuildingResponse): void {
    this.accessibilityFeatures = [];
    if (building.accessibilityFeatures) {
      try {
        const parsed = JSON.parse(building.accessibilityFeatures);
        if (Array.isArray(parsed)) {
          this.accessibilityFeatures = parsed.filter((f) => typeof f === 'string' && f.trim());
        }
      } catch {
        // Not valid JSON — ignore
      }
    }
  }

  // ── Display Helpers ───────────────────────────────────────

  get availabilitySummary(): string {
    if (this.totalRooms === 0) return '';
    if (this.availableRooms === 0) return 'Fully occupied';
    return `${this.availableRooms} of ${this.totalRooms} rooms available`;
  }

  getGroupPriceDisplay(group: CapacityGroup): string {
    if (group.minPrice == null) return 'Price on request';
    if (group.minPrice === group.maxPrice) {
      return `R${this.formatNumber(group.minPrice)}/month`;
    }
    return `R${this.formatNumber(group.minPrice)} – R${this.formatNumber(group.maxPrice!)}/month`;
  }

  private capacityLabel(capacity: number): string {
    switch (capacity) {
      case 1:
        return 'Single';
      case 2:
        return 'Double';
      case 3:
        return 'Triple';
      default:
        return `Shared (${capacity}-person)`;
    }
  }

  private formatNumber(value: number): string {
    return value.toLocaleString('en-ZA', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    });
  }
}
