import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { BuildingResponse } from '../../../../core/services/building.service';

@Component({
  selector: 'app-building-gallery',
  templateUrl: './building-gallery.component.html',
  styleUrls: ['./building-gallery.component.scss'],
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, MatButtonModule],
})
export class BuildingGalleryComponent {
  @Input() buildings: BuildingResponse[] = [];
  /** Ordered list of selected building IDs (index 0 = 1st preference). Max 3. */
  @Input() selectedBuildingIds: number[] = [];
  @Output() buildingSelectionChanged = new EventEmitter<number[]>();

  readonly MAX_SELECTIONS = 3;
  readonly RANK_LABELS = ['1st', '2nd', '3rd'];

  get isAtMax(): boolean {
    return this.selectedBuildingIds.length >= this.MAX_SELECTIONS;
  }

  getRank(buildingId: number): number | null {
    const idx = this.selectedBuildingIds.indexOf(buildingId);
    return idx === -1 ? null : idx + 1;
  }

  isSelected(buildingId: number): boolean {
    return this.selectedBuildingIds.includes(buildingId);
  }

  addBuilding(buildingId: number): void {
    if (this.isSelected(buildingId) || this.isAtMax) return;
    const updated = [...this.selectedBuildingIds, buildingId];
    this.buildingSelectionChanged.emit(updated);
  }

  removeBuilding(buildingId: number): void {
    const updated = this.selectedBuildingIds.filter((id) => id !== buildingId);
    this.buildingSelectionChanged.emit(updated);
  }

  clearAll(): void {
    this.buildingSelectionChanged.emit([]);
  }

  getBuildingById(id: number): BuildingResponse | undefined {
    return this.buildings.find((b) => b.id === id);
  }

  /** Available buildings — those not yet selected */
  get availableBuildings(): BuildingResponse[] {
    return this.buildings.filter((b) => !this.isSelected(b.id));
  }
}
