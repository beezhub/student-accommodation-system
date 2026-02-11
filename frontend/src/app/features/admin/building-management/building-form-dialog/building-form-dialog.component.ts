import { Component, Inject, OnInit, ViewChild, ElementRef, AfterViewInit } from '@angular/core';

import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { CommonModule } from '@angular/common';
import {
  BuildingService,
  BuildingRequest,
  BuildingResponse,
} from '../../../../core/services/building.service';
import { InstitutionService } from '../../../../core/services/institution.service';
import { Institution } from '../../../../core/models/institution.model';
import { GoogleMapsService } from '../../../../core/services/google-maps.service';
import { GoogleMapsAlternativeService, AutocompleteHandle } from '../../../../core/services/google-maps-alternative.service';
import { PlaceDetails } from '../../../../core/models/place-details.model';

@Component({
  selector: 'app-building-form-dialog',
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <div class="header-icon">
          <mat-icon>{{ data.mode === 'create' ? 'add_business' : 'edit' }}</mat-icon>
        </div>
        <div class="header-text">
          <h2 mat-dialog-title>{{ data.mode === 'create' ? 'Add New Building' : 'Edit Building' }}</h2>
          <p class="subtitle">{{ data.mode === 'create' ? 'Create a new accommodation building' : 'Update building information' }}</p>
        </div>
      </div>

      <mat-dialog-content>
        <form [formGroup]="buildingForm" class="building-form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Building Name</mat-label>
            <mat-icon matPrefix>apartment</mat-icon>
            <input matInput formControlName="name" placeholder="e.g., North Hall" required />
            @if (buildingForm.get('name')?.hasError('required')) {
              <mat-error>Building name is required</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Address</mat-label>
            <mat-icon matPrefix>location_on</mat-icon>
            <input
              #addressInput
              matInput
              formControlName="address"
              placeholder="Start typing to search..."
            />
            @if (mapsEnabled) {
              <mat-hint>Google Places autocomplete enabled</mat-hint>
            }
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Total Floors</mat-label>
            <mat-icon matPrefix>layers</mat-icon>
            <input
              matInput
              type="number"
              formControlName="totalFloors"
              placeholder="e.g., 5"
              min="1"
            />
            <mat-hint>Number of floors in the building</mat-hint>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Institutions Served</mat-label>
            <mat-icon matPrefix>school</mat-icon>
            <mat-select formControlName="institutionIds" multiple>
              @for (institution of institutions; track institution.id) {
                <mat-option [value]="institution.id">{{ institution.name }}</mat-option>
              }
            </mat-select>
            <mat-hint>Select which institutions this building serves (leave empty for all)</mat-hint>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Description</mat-label>
            <mat-icon matPrefix>description</mat-icon>
            <textarea
              matInput
              formControlName="description"
              rows="4"
              placeholder="Additional information about the building"
            ></textarea>
          </mat-form-field>
        </form>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-stroked-button (click)="onCancel()" [disabled]="isSubmitting" class="cancel-btn">
          Cancel
        </button>
        <button
          mat-raised-button
          color="primary"
          (click)="onSubmit()"
          [disabled]="!buildingForm.valid || isSubmitting"
          class="submit-btn"
        >
          @if (isSubmitting) {
            <mat-spinner diameter="20" class="button-spinner"></mat-spinner>
            {{ data.mode === 'create' ? 'Creating...' : 'Updating...' }}
          } @else {
            <ng-container>
              <mat-icon>{{ data.mode === 'create' ? 'add' : 'check' }}</mat-icon>
              {{ data.mode === 'create' ? 'Create Building' : 'Update Building' }}
            </ng-container>
          }
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      /* ===== Mobile-first base styles ===== */
      .dialog-container {
        width: 100%;
      }

      /* Dialog Header */
      .dialog-header {
        display: flex;
        align-items: flex-start;
        gap: 14px;
        padding: 16px 16px 0;
      }

      .header-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 48px;
        height: 48px;
        min-width: 48px;
        background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%);
        border-radius: 14px;
        border: 1px solid #fed7aa;
        box-shadow: 0 4px 12px rgba(234, 88, 12, 0.1);
      }

      .header-icon mat-icon {
        font-size: 24px;
        width: 24px;
        height: 24px;
        color: #ea580c;
      }

      .header-text h2 {
        margin: 0 0 4px 0;
        font-size: 18px;
        font-weight: 700;
        color: #1e293b;
        background: linear-gradient(135deg, #1e293b 0%, #ea580c 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
      }

      .header-text .subtitle {
        margin: 0;
        font-size: 13px;
        color: #64748b;
        font-weight: 500;
      }

      /* Dialog Content */
      mat-dialog-content {
        padding: 16px !important;
        max-height: 65vh;
        overflow-y: auto;
      }

      .building-form {
        display: flex;
        flex-direction: column;
        gap: 14px;
      }

      .full-width {
        width: 100%;
      }

      mat-form-field {
        margin-bottom: 4px;
      }

      mat-form-field.mat-form-field-outline {
        border-radius: 12px;
      }

      mat-icon[matPrefix] {
        color: #94a3b8;
        margin-right: 8px;
        font-size: 18px;
        width: 18px;
        height: 18px;
      }

      mat-hint {
        color: #94a3b8;
        font-size: 11px;
        margin-top: 4px;
      }

      mat-error {
        color: #dc2626;
        font-size: 12px;
        font-weight: 500;
      }

      /* Multi-select styling */
      mat-select[multiple] {
        padding: 4px 0;
      }

      /* Dialog Actions */
      mat-dialog-actions {
        flex-direction: column-reverse;
        padding: 16px !important;
        gap: 10px;
        border-top: 1px solid #f1f5f9;
        margin-top: 8px;
      }

      .cancel-btn {
        border-radius: 12px !important;
        padding: 0 20px !important;
        width: 100%;
        height: 44px;
        color: #64748b;
        border-color: #e2e8f0 !important;
        font-weight: 600;
        transition: all 0.2s ease;
      }

      .cancel-btn:hover {
        background-color: #f8fafc;
        border-color: #cbd5e1 !important;
        color: #475569;
      }

      .submit-btn {
        border-radius: 12px !important;
        padding: 0 20px !important;
        width: 100%;
        height: 44px;
        font-weight: 700;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
        transition: all 0.2s ease;
      }

      .submit-btn:hover {
        transform: translateY(-1px);
        box-shadow: 0 6px 16px rgba(59, 130, 246, 0.4);
      }

      .submit-btn:disabled {
        transform: none;
        box-shadow: none;
      }

      .submit-btn mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
      }

      .button-spinner {
        display: inline-block;
        margin-right: 8px;
      }

      /* Google Places Autocomplete Element Styling — mobile-first */
      ::ng-deep gmp-place-autocomplete {
        width: 100%;
        display: block;
        border: 1px solid #cbd5e1;
        border-radius: 10px;
        padding: 10px 12px;
        background-color: #fff;
        transition: border-color 0.2s ease, box-shadow 0.2s ease;
        min-height: 44px;
        box-sizing: border-box;
      }

      ::ng-deep gmp-place-autocomplete:hover {
        border-color: #94a3b8;
      }

      ::ng-deep gmp-place-autocomplete:focus-within {
        border-color: #ea580c;
        box-shadow: 0 0 0 2px rgba(234, 88, 12, 0.15);
      }

      /* Force through shadow DOM for autocomplete input */
      ::ng-deep gmp-place-autocomplete input,
      ::ng-deep gmp-place-autocomplete input[type="text"],
      gmp-place-autocomplete input,
      gmp-place-autocomplete input[type="text"] {
        width: 100% !important;
        border: none !important;
        outline: none !important;
        padding: 0 !important;
        margin: 0 !important;
        font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif !important;
        font-size: 14px !important;
        line-height: 1.5 !important;
        color: #1e293b !important;
        background-color: transparent !important;
        box-shadow: none !important;
        -webkit-text-fill-color: #1e293b !important;
        -webkit-appearance: none !important;
      }

      ::ng-deep gmp-place-autocomplete input::placeholder,
      gmp-place-autocomplete input::placeholder {
        color: #94a3b8 !important;
        opacity: 1 !important;
      }

      /* ===== Tablet breakpoint (min-width: 480px) ===== */
      @media (min-width: 480px) {
        .dialog-header {
          padding: 20px 20px 0;
          gap: 16px;
        }

        .header-icon {
          width: 52px;
          height: 52px;
          min-width: 52px;
          border-radius: 16px;
        }

        .header-icon mat-icon {
          font-size: 26px;
          width: 26px;
          height: 26px;
        }

        .header-text h2 {
          font-size: 20px;
        }

        .header-text .subtitle {
          font-size: 14px;
        }

        mat-dialog-content {
          padding: 20px !important;
        }

        .building-form {
          gap: 16px;
        }

        mat-dialog-actions {
          padding: 20px !important;
          gap: 12px;
        }

        mat-icon[matPrefix] {
          margin-right: 10px;
          font-size: 20px;
          width: 20px;
          height: 20px;
        }

        mat-hint {
          font-size: 12px;
        }

        ::ng-deep gmp-place-autocomplete {
          padding: 12px 14px;
        }
      }

      /* ===== Desktop breakpoint (min-width: 600px) ===== */
      @media (min-width: 600px) {
        .dialog-container {
          min-width: 520px;
          max-width: 600px;
        }

        .dialog-header {
          padding: 28px 28px 0;
          gap: 18px;
        }

        .header-icon {
          width: 60px;
          height: 60px;
          min-width: 60px;
          border-radius: 18px;
        }

        .header-icon mat-icon {
          font-size: 30px;
          width: 30px;
          height: 30px;
        }

        .header-text h2 {
          margin-bottom: 6px;
          font-size: 24px;
        }

        .header-text .subtitle {
          font-size: 15px;
        }

        mat-dialog-content {
          padding: 28px !important;
        }

        .building-form {
          gap: 20px;
        }

        mat-icon[matPrefix] {
          margin-right: 12px;
        }

        mat-dialog-actions {
          flex-direction: row;
          justify-content: flex-end;
          padding: 20px 28px 28px !important;
          gap: 16px;
          margin-top: 12px;
        }

        .cancel-btn {
          width: auto;
          padding: 0 24px !important;
          height: 48px;
        }

        .submit-btn {
          width: auto;
          padding: 0 28px !important;
          height: 48px;
        }

        .submit-btn mat-icon {
          font-size: 22px;
          width: 22px;
          height: 22px;
        }
      }
    `,
  ],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatIconModule,
    MatSelectModule,
    MatChipsModule,
  ],
})
export class BuildingFormDialogComponent implements OnInit, AfterViewInit {
  @ViewChild('addressInput') addressInput!: ElementRef<HTMLInputElement>;

  buildingForm: FormGroup;
  isSubmitting = false;
  institutions: Institution[] = [];
  mapsEnabled = false;
  private autocomplete: AutocompleteHandle | null = null;

  constructor(
    private fb: FormBuilder,
    private buildingService: BuildingService,
    private institutionService: InstitutionService,
    private googleMapsService: GoogleMapsService,
    private googleMapsAlternativeService: GoogleMapsAlternativeService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<BuildingFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { mode: 'create' | 'edit'; building?: BuildingResponse }
  ) {
    this.buildingForm = this.fb.group({
      name: ['', Validators.required],
      address: [''],
      placeId: [''],
      latitude: [null],
      longitude: [null],
      totalFloors: [null],
      description: [''],
      institutionIds: [[]],
    });
  }

  ngOnInit(): void {
    this.loadInstitutions();
    this.loadMapsConfig();

    if (this.data.mode === 'edit' && this.data.building) {
      this.buildingForm.patchValue({
        name: this.data.building.name,
        address: this.data.building.address,
        placeId: this.data.building.placeId,
        latitude: this.data.building.latitude,
        longitude: this.data.building.longitude,
        totalFloors: this.data.building.totalFloors,
        description: this.data.building.description,
        institutionIds: this.data.building.institutions?.map(i => i.id) || [],
      });
    }
  }

  ngAfterViewInit(): void {
    // Autocomplete is initialized after Maps API loads (see loadMapsConfig)
  }

  loadMapsConfig(): void {
    this.buildingService.getMapsConfig().subscribe({
      next: (config) => {
        if (config.enabled && config.apiKey) {
          this.mapsEnabled = true;
          // Use ALTERNATIVE service instead
          this.googleMapsAlternativeService.loadGoogleMapsAPI(config.apiKey)
            .then(() => {
              // Initialize autocomplete after Maps API is loaded AND view is initialized
              setTimeout(() => {
                if (this.addressInput && this.addressInput.nativeElement) {
                  this.initializeAutocomplete();
                }
              }, 500); // Increased timeout for API initialization
            })
            .catch((error) => {
              console.error('Failed to load Google Maps API:', error);
              this.mapsEnabled = false;
            });
        }
      },
      error: (error) => {
        console.error('Error loading maps config:', error);
        this.mapsEnabled = false;
      }
    });
  }

  initializeAutocomplete(): void {
    if (!this.addressInput || !this.addressInput.nativeElement) {
      return;
    }

    this.autocomplete = this.googleMapsAlternativeService.initializeAutocomplete(
      this.addressInput.nativeElement,
      (placeDetails: PlaceDetails | null) => {
        if (placeDetails) {
          // Use setValue on individual controls to ensure Angular picks up the changes
          this.buildingForm.get('address')?.setValue(placeDetails.formattedAddress);
          this.buildingForm.get('placeId')?.setValue(placeDetails.placeId);
          this.buildingForm.get('latitude')?.setValue(placeDetails.latitude);
          this.buildingForm.get('longitude')?.setValue(placeDetails.longitude);

          // Mark controls as dirty and touched
          ['address', 'placeId', 'latitude', 'longitude'].forEach(field => {
            this.buildingForm.get(field)?.markAsDirty();
            this.buildingForm.get(field)?.markAsTouched();
          });
          this.buildingForm.updateValueAndValidity();
        }
      }
    );
  }

  loadInstitutions(): void {
    this.institutionService.getInstitutions().subscribe({
      next: (institutions) => {
        this.institutions = institutions;
      },
      error: (error) => {
        console.error('Error loading institutions:', error);
      }
    });
  }

  async onSubmit(): Promise<void> {
    if (!this.buildingForm.valid) {
      return;
    }

    this.isSubmitting = true;

    // Always resolve address from autocomplete before submitting
    // This ensures updated addresses are synced from the autocomplete element
    if (this.autocomplete) {
      try {
        await this.autocomplete.resolveAddress();
      } catch {
        // Address resolution failed, continue with available data
      }
    }

    const buildingData: BuildingRequest = this.buildingForm.value;

    const operation =
      this.data.mode === 'create'
        ? this.buildingService.createBuilding(buildingData)
        : this.buildingService.updateBuilding(this.data.building!.id, buildingData);

    operation.subscribe({
      next: () => {
        this.snackBar.open(
          `Building ${this.data.mode === 'create' ? 'created' : 'updated'} successfully`,
          'Close',
          { duration: 3000 }
        );
        this.dialogRef.close(true);
      },
      error: (error) => {
        console.error('Error saving building:', error);
        this.snackBar.open(error.error?.message || 'Failed to save building', 'Close', {
          duration: 5000,
        });
        this.isSubmitting = false;
      },
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
