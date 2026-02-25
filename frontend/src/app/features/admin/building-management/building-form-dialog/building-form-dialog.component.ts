import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
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
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { CommonModule } from '@angular/common';
import { Subject, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, takeUntil } from 'rxjs/operators';
import {
  BuildingService,
  BuildingRequest,
  BuildingResponse,
} from '../../../../core/services/building.service';
import { InstitutionService } from '../../../../core/services/institution.service';
import { Institution } from '../../../../core/models/institution.model';
import { NominatimService, AddressSuggestion } from '../../../../core/services/nominatim.service';

@Component({
  selector: 'app-building-form-dialog',
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <div class="header-icon">
          <mat-icon>{{ data.mode === 'create' ? 'add_business' : 'edit' }}</mat-icon>
        </div>
        <div class="header-text">
          <h2 mat-dialog-title>
            {{ data.mode === 'create' ? 'Add New Building' : 'Edit Building' }}
          </h2>
          <p class="subtitle">
            {{
              data.mode === 'create'
                ? 'Create a new accommodation building'
                : 'Update building information'
            }}
          </p>
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

          <!-- Address with Nominatim autocomplete -->
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Address</mat-label>
            <mat-icon matPrefix>location_on</mat-icon>
            <input
              matInput
              formControlName="address"
              placeholder="Start typing to search..."
              [matAutocomplete]="addressAuto"
              (input)="onAddressInput($event)"
            />
            <mat-hint>Powered by OpenStreetMap</mat-hint>
            <mat-autocomplete
              #addressAuto="matAutocomplete"
              [displayWith]="displayAddress"
              (optionSelected)="onSuggestionSelected($event.option.value)"
            >
              @if (isSearching) {
              <mat-option disabled>
                <div class="searching-hint">
                  <mat-spinner diameter="16"></mat-spinner>
                  Searching...
                </div>
              </mat-option>
              } @for (suggestion of addressSuggestions; track suggestion.placeId) {
              <mat-option [value]="suggestion">
                <mat-icon class="suggestion-icon">place</mat-icon>
                <span class="suggestion-text">{{ suggestion.displayName }}</span>
              </mat-option>
              }
            </mat-autocomplete>
          </mat-form-field>

          @if (buildingForm.get('latitude')?.value) {
          <div class="coords-row">
            <mat-form-field appearance="outline" class="half-width">
              <mat-label>Latitude</mat-label>
              <mat-icon matPrefix>my_location</mat-icon>
              <input matInput formControlName="latitude" readonly />
            </mat-form-field>
            <mat-form-field appearance="outline" class="half-width">
              <mat-label>Longitude</mat-label>
              <mat-icon matPrefix>my_location</mat-icon>
              <input matInput formControlName="longitude" readonly />
            </mat-form-field>
          </div>
          }

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
            <mat-hint
              >Select which institutions this building serves (leave empty for all)</mat-hint
            >
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
        <button
          mat-stroked-button
          (click)="onCancel()"
          [disabled]="isSubmitting"
          class="cancel-btn"
        >
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

      .half-width {
        flex: 1;
      }

      .coords-row {
        display: flex;
        gap: 12px;
      }

      mat-form-field {
        margin-bottom: 4px;
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

      /* Autocomplete option styling */
      .suggestion-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
        color: #ea580c;
        margin-right: 8px;
        vertical-align: middle;
      }

      .suggestion-text {
        font-size: 13px;
        color: #1e293b;
        white-space: normal;
        line-height: 1.4;
      }

      .searching-hint {
        display: flex;
        align-items: center;
        gap: 8px;
        color: #94a3b8;
        font-size: 13px;
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
    MatAutocompleteModule,
  ],
})
export class BuildingFormDialogComponent implements OnInit, OnDestroy {
  buildingForm: FormGroup;
  isSubmitting = false;
  institutions: Institution[] = [];
  addressSuggestions: AddressSuggestion[] = [];
  isSearching = false;

  private destroy$ = new Subject<void>();
  private searchSubject = new Subject<string>();

  constructor(
    private fb: FormBuilder,
    private buildingService: BuildingService,
    private institutionService: InstitutionService,
    private nominatimService: NominatimService,
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
    this.setupAddressSearch();

    if (this.data.mode === 'edit' && this.data.building) {
      this.buildingForm.patchValue({
        name: this.data.building.name,
        address: this.data.building.address,
        placeId: this.data.building.placeId,
        latitude: this.data.building.latitude,
        longitude: this.data.building.longitude,
        totalFloors: this.data.building.totalFloors,
        description: this.data.building.description,
        institutionIds: this.data.building.institutions?.map((i) => i.id) || [],
      });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupAddressSearch(): void {
    this.searchSubject
      .pipe(
        debounceTime(400),
        distinctUntilChanged(),
        switchMap((query) => {
          if (!query || query.length < 3) {
            this.addressSuggestions = [];
            this.isSearching = false;
            return [];
          }
          this.isSearching = true;
          return this.nominatimService.search(query);
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (suggestions) => {
          this.addressSuggestions = suggestions;
          this.isSearching = false;
        },
        error: () => {
          this.addressSuggestions = [];
          this.isSearching = false;
        },
      });
  }

  onAddressInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    // Clear coordinates when user types a new address
    if (this.buildingForm.get('latitude')?.value) {
      this.buildingForm.patchValue({ placeId: '', latitude: null, longitude: null });
    }
    this.searchSubject.next(value);
  }

  onSuggestionSelected(suggestion: AddressSuggestion): void {
    this.buildingForm.patchValue({
      address: suggestion.displayName,
      placeId: suggestion.placeId,
      latitude: suggestion.latitude,
      longitude: suggestion.longitude,
    });
    this.addressSuggestions = [];
  }

  displayAddress(suggestion: AddressSuggestion | string | null): string {
    if (!suggestion) return '';
    if (typeof suggestion === 'string') return suggestion;
    return suggestion.displayName;
  }

  loadInstitutions(): void {
    this.institutionService.getInstitutions().subscribe({
      next: (institutions) => {
        this.institutions = institutions;
      },
      error: (error) => {
        console.error('Error loading institutions:', error);
      },
    });
  }

  onSubmit(): void {
    if (!this.buildingForm.valid) {
      return;
    }

    this.isSubmitting = true;

    // If user typed an address but didn't select from dropdown, treat it as plain text
    const formValue = this.buildingForm.value;
    const buildingData: BuildingRequest = {
      name: formValue.name,
      address:
        typeof formValue.address === 'object' && formValue.address
          ? (formValue.address as AddressSuggestion).displayName
          : formValue.address,
      placeId: formValue.placeId,
      latitude: formValue.latitude,
      longitude: formValue.longitude,
      totalFloors: formValue.totalFloors,
      description: formValue.description,
      institutionIds: formValue.institutionIds,
    };

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
