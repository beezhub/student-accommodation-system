import { Component, EventEmitter, Input, OnDestroy, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { BuildingResponse } from '../../../../core/services/building.service';

@Component({
  selector: 'app-building-gallery',
  templateUrl: './building-gallery.component.html',
  styleUrls: ['./building-gallery.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
  ],
})
export class BuildingGalleryComponent implements OnDestroy {
  @Input() buildings: BuildingResponse[] = [];
  /** Ordered list of selected building IDs (index 0 = 1st preference). Max 3. */
  @Input() selectedBuildingIds: number[] = [];
  @Output() buildingSelectionChanged = new EventEmitter<number[]>();
  /** Emits the trimmed search query; parent is responsible for fetching results. */
  @Output() searchChanged = new EventEmitter<string>();

  readonly MAX_SELECTIONS = 3;
  readonly RANK_LABELS = ['1st', '2nd', '3rd'];

  searchQuery = '';
  private debounceTimer: ReturnType<typeof setTimeout> | null = null;

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

  onSearch(event: Event): void {
    this.searchQuery = (event.target as HTMLInputElement).value;
    if (this.debounceTimer) clearTimeout(this.debounceTimer);
    this.debounceTimer = setTimeout(() => {
      this.searchChanged.emit(this.searchQuery.trim());
    }, 350);
  }

  clearSearch(): void {
    this.searchQuery = '';
    if (this.debounceTimer) clearTimeout(this.debounceTimer);
    this.searchChanged.emit('');
  }

  ngOnDestroy(): void {
    if (this.debounceTimer) clearTimeout(this.debounceTimer);
  }

  getBuildingById(id: number): BuildingResponse | undefined {
    return this.buildings.find((b) => b.id === id);
  }

  /** Available buildings — those not yet selected */
  get availableBuildings(): BuildingResponse[] {
    return this.buildings.filter((b) => !this.isSelected(b.id));
  }
}
