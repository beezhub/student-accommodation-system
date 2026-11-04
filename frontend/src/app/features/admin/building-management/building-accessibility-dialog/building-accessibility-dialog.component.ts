import { Component, OnInit, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatChipInputEvent, MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { BuildingResponse } from '../../../../core/services/building.service';
import { BuildingAttributesService } from '../../../../core/services/building-attributes.service';

export interface BuildingAccessibilityDialogData {
  building: BuildingResponse;
}

/**
 * Edits the accessibility features shown as chips on a building's public page (#305). They are the
 * one building attribute still in use: gender is managed on rooms, and desirability is deprecated.
 */
@Component({
  selector: 'app-building-accessibility-dialog',
  templateUrl: './building-accessibility-dialog.component.html',
  styleUrls: ['./building-accessibility-dialog.component.scss'],
  standalone: true,
  imports: [
    MatDialogModule,
    MatButtonModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
})
export class BuildingAccessibilityDialogComponent implements OnInit {
  private attributesService = inject(BuildingAttributesService);
  private snackBar = inject(MatSnackBar);
  dialogRef = inject<MatDialogRef<BuildingAccessibilityDialogComponent>>(MatDialogRef);
  data = inject<BuildingAccessibilityDialogData>(MAT_DIALOG_DATA);

  readonly separatorKeys = [ENTER, COMMA];

  features: string[] = [];
  isLoading = true;
  isSaving = false;
  loadFailed = false;
  saveError: string | null = null;
  private isChanged = false;
  /** Text in the box that has not become a chip yet: a Save right after typing must include it. */
  private typed = '';

  ngOnInit(): void {
    this.attributesService.getBuildingAttributes(this.data.building.id).subscribe({
      next: (attributes) => {
        this.features = this.parse(attributes.accessibilityFeatures);
        this.isLoading = false;
      },
      error: () => {
        this.loadFailed = true;
        this.isLoading = false;
      },
    });
  }

  /** The public building page reads the same format and ignores anything that is not a list. */
  private parse(stored?: string | null): string[] {
    if (!stored) return [];
    try {
      const parsed: unknown = JSON.parse(stored);
      return Array.isArray(parsed) ? parsed.filter((item) => typeof item === 'string') : [];
    } catch {
      return [];
    }
  }

  get canSave(): boolean {
    // After a failed load the list is empty, so saving it would wipe the features it failed to show.
    const hasChange = this.isChanged || this.typed.trim().length > 0;
    return hasChange && !this.isLoading && !this.loadFailed && !this.isSaving;
  }

  onType(value: string): void {
    this.typed = value;
  }

  addFeature(value: string): void {
    const feature = value.trim();
    const alreadyListed = this.features.some((f) => f.toLowerCase() === feature.toLowerCase());
    if (!feature || alreadyListed) return;
    this.features = [...this.features, feature];
    this.isChanged = true;
  }

  onChipInput(event: MatChipInputEvent): void {
    this.addFeature(event.value);
    this.typed = '';
    event.chipInput.clear();
  }

  removeFeature(feature: string): void {
    this.features = this.features.filter((f) => f !== feature);
    this.isChanged = true;
  }

  save(): void {
    if (!this.canSave) return;
    this.addFeature(this.typed);
    this.typed = '';
    this.isSaving = true;
    this.saveError = null;
    this.attributesService
      .saveAccessibilityFeatures(this.data.building.id, this.features)
      .subscribe({
        next: () => {
          this.snackBar.open(
            `Accessibility features saved for ${this.data.building.name}.`,
            'Close',
            {
              duration: 3000,
              panelClass: 'snack-success',
            }
          );
          this.dialogRef.close(true);
        },
        error: (err: HttpErrorResponse) => {
          this.isSaving = false;
          this.saveError =
            err.error?.message ||
            err.error?.errors?.[0] ||
            'The features could not be saved. Try again.';
        },
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
