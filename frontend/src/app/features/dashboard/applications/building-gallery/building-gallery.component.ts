import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { BuildingResponse } from '../../../../core/services/building.service';

@Component({
  selector: 'app-building-gallery',
  templateUrl: './building-gallery.component.html',
  styleUrls: ['./building-gallery.component.scss'],
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule],
})
export class BuildingGalleryComponent {
  @Input() buildings: BuildingResponse[] = [];
  @Input() selectedBuildingId: number | null = null;
  @Output() buildingSelected = new EventEmitter<number | null>();

  selectBuilding(buildingId: number | null): void {
    this.selectedBuildingId = buildingId;
    this.buildingSelected.emit(buildingId);
  }

  isSelected(buildingId: number | null): boolean {
    return this.selectedBuildingId === buildingId;
  }
}
