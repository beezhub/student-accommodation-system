import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';

import { BuildingService, BuildingResponse } from '../../../core/services/building.service';
import {
  BuildingAttributesService,
  BuildingAttributes,
  RoomAttributes,
  GenderRestriction,
} from '../../../core/services/building-attributes.service';

@Component({
  selector: 'app-building-attributes-management',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatTableModule,
    MatSnackBarModule,
    MatTabsModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatTooltipModule,
  ],
  templateUrl: './building-attributes-management.component.html',
  styleUrls: ['./building-attributes-management.component.css'],
})
export class BuildingAttributesManagementComponent implements OnInit {
  buildingForm: FormGroup;
  attributesForm: FormGroup;

  buildings: BuildingResponse[] = [];
  roomAttributes: RoomAttributes[] = [];

  isLoading = false;
  isSaving = false;
  selectedBuildingId: number | null = null;

  genderOptions = Object.values(GenderRestriction);
  displayedColumns: string[] = [
    'roomNumber',
    'capacity',
    'genderRestriction',
    'accessible',
    'desirability',
    'actions',
  ];

  private fb = inject(FormBuilder);
  private buildingService = inject(BuildingService);
  private attributesService = inject(BuildingAttributesService);
  private snackBar = inject(MatSnackBar);

  constructor() {
    this.buildingForm = this.fb.group({
      buildingId: [''],
    });

    this.attributesForm = this.fb.group({
      genderRestriction: [GenderRestriction.NONE, Validators.required],
      hasElevator: [false],
      wheelchairAccessible: [false],
      desirabilityScore: [50, [Validators.min(0), Validators.max(100)]],
      accessType: ['KEY'],
      amenities: [''],
    });
  }

  ngOnInit(): void {
    this.loadBuildings();

    this.buildingForm.get('buildingId')?.valueChanges.subscribe((id) => {
      if (id) {
        this.selectedBuildingId = id;
        this.loadAttributes(id);
      }
    });
  }

  loadBuildings(): void {
    this.buildingService.getAllBuildingsNoPagination().subscribe({
      next: (data) => (this.buildings = data),
      error: (err) => console.error('Failed to load buildings', err),
    });
  }

  loadAttributes(buildingId: number): void {
    this.isLoading = true;

    // Load general building attributes
    this.attributesService.getBuildingAttributes(buildingId).subscribe({
      next: (attrs) => {
        this.attributesForm.patchValue({
          genderRestriction: attrs.genderRestriction || GenderRestriction.NONE,
          hasElevator: attrs.hasElevator,
          wheelchairAccessible: attrs.wheelchairAccessible,
          desirabilityScore: attrs.desirabilityScore,
          accessType: attrs.accessType,
          amenities: attrs.amenities ? attrs.amenities.join(', ') : '',
        });
      },
      error: (err) => {
        // If 404, specific handling or create new default?
        // For now, assuming backend returns default if not found or empty
        console.warn('Could not load attributes, might be new configuration', err);
        // Reset to defaults
        this.attributesForm.reset({
          genderRestriction: GenderRestriction.NONE,
          desirabilityScore: 50,
        });
      },
    });

    // Load room attributes
    this.attributesService.getRoomAttributes(buildingId).subscribe({
      next: (rooms) => {
        this.roomAttributes = rooms;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load room attributes', err);
        this.isLoading = false;
      },
    });
  }

  saveBuildingAttributes(): void {
    if (this.attributesForm.invalid || !this.selectedBuildingId) return;

    this.isSaving = true;
    const formValue = this.attributesForm.value;

    // Convert amenities string to array
    const amenitiesArray = formValue.amenities
      .split(',')
      .map((s: string) => s.trim())
      .filter((s: string) => s.length > 0);

    const payload: Partial<BuildingAttributes> = {
      ...formValue,
      amenities: amenitiesArray,
    };

    this.attributesService.saveBuildingAttributes(this.selectedBuildingId, payload).subscribe({
      next: () => {
        this.snackBar.open('Building attributes saved successfully', 'Close', { duration: 3000 });
        this.isSaving = false;
      },
      error: (err) => {
        console.error('Save failed', err);
        this.snackBar.open('Failed to save attributes', 'Close', { duration: 3000 });
        this.isSaving = false;
      },
    });
  }

  updateRoomAttribute(room: RoomAttributes, field: string, value: any): void {
    if (!room.id) return;

    const update: Partial<RoomAttributes> = {};
    // @ts-expect-error dynamic key assignment
    update[field] = value;

    this.attributesService.saveRoomAttributes(room.roomId, update).subscribe({
      next: (updated) => {
        // Update local state
        Object.assign(room, updated);
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        this.snackBar.open('Room updated', 'Close', { duration: 1000 });
      },
      error: (err) => {
        console.error('Room update failed', err);
        this.snackBar.open('Failed to update room', 'Close', { duration: 3000 });
        // Revert local change if possible (complex with table input, skipped for MVP)
      },
    });
  }
}
