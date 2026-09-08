import { Component, OnInit, OnDestroy, inject } from '@angular/core';
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
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, takeUntil } from 'rxjs/operators';
import {
  BuildingService,
  BuildingRequest,
  BuildingResponse,
} from '../../../../core/services/building.service';
import { InstitutionService } from '../../../../core/services/institution.service';
import { Institution } from '../../../../core/models/institution.model';
import { GeocodingService, AddressSuggestion } from '../../../../core/services/geocoding.service';
import { TenantService, Tenant } from '../../../../core/services/tenant.service';
import { RoleService } from '../../../../core/services/role.service';

@Component({
  selector: 'app-building-form-dialog',
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <div class="header-icon-wrap" aria-hidden="true">
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

          @if (showCompanySelector) {
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Property Company</mat-label>
            <mat-icon matPrefix>business</mat-icon>
            <mat-select formControlName="tenantId">
              @for (company of companies; track company.id) {
              <mat-option [value]="company.id">{{ company.name }}</mat-option>
              }
            </mat-select>
            <mat-hint>The company that will own this building</mat-hint>
            @if (buildingForm.get('tenantId')?.hasError('required') &&
            buildingForm.get('tenantId')?.touched) {
            <mat-error>Choose the company that will own this building</mat-error>
            }
          </mat-form-field>
          }

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Institutions Served</mat-label>
            <mat-icon matPrefix>school</mat-icon>
            <mat-select formControlName="institutionIds" multiple>
              @for (institution of institutions; track institution.id) {
              <mat-option [value]="institution.id">{{ institution.name }}</mat-option>
              }
            </mat-select>
            <mat-hint>Leave empty to serve all institutions</mat-hint>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Description</mat-label>
            <mat-icon matPrefix>description</mat-icon>
            <textarea
              matInput
              formControlName="description"
              rows="3"
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
          (click)="onSubmit()"
          [disabled]="!buildingForm.valid || isSubmitting"
          class="submit-btn"
        >
          @if (isSubmitting) {
          <mat-spinner diameter="18" class="button-spinner"></mat-spinner>
          {{ data.mode === 'create' ? 'Creating...' : 'Updating...' }}
          } @else {
          <mat-icon>{{ data.mode === 'create' ? 'add' : 'check' }}</mat-icon>
          {{ data.mode === 'create' ? 'Create Building' : 'Update Building' }}
          }
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .dialog-container {
        width: 100%;
      }

      /* ── Header band ── */
      .dialog-header {
        display: flex;
        align-items: center;
        gap: 0.875rem;
        padding: 1.25rem 1.5rem 1.125rem;
        background: #0f172a;
        position: relative;
        flex-shrink: 0;
      }
      .dialog-header::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 0;
        right: 0;
        height: 3px;
        background: linear-gradient(90deg, #c2410c 0%, #f97316 100%);
      }
      .header-icon-wrap {
        width: 40px;
        height: 40px;
        min-width: 40px;
        border-radius: 10px;
        background: linear-gradient(135deg, #f97316 0%, #c2410c 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }
      .header-icon-wrap mat-icon {
        color: white;
        font-size: 1.25rem;
        width: 1.25rem;
        height: 1.25rem;
      }
      .header-text h2 {
        margin: 0 0 0.125rem 0;
        font-size: 1.0625rem;
        font-weight: 700;
        color: white;
        letter-spacing: -0.01em;
      }
      .header-text .subtitle {
        margin: 0;
        font-size: 0.8125rem;
        color: #94a3b8;
      }

      /* ── Content ── */
      mat-dialog-content {
        padding: 1.25rem 1.5rem !important;
        max-height: 65vh;
        overflow-y: auto;
      }
      .building-form {
        display: flex;
        flex-direction: column;
        gap: 0.875rem;
      }
      .full-width {
        width: 100%;
      }
      .half-width {
        flex: 1;
      }
      .coords-row {
        display: flex;
        gap: 0.75rem;
      }

      mat-icon[matPrefix] {
        color: #94a3b8;
        margin-right: 0.5rem;
        font-size: 1.125rem;
        width: 1.125rem;
        height: 1.125rem;
      }

      .suggestion-icon {
        font-size: 1rem;
        width: 1rem;
        height: 1rem;
        color: #ea580c;
        margin-right: 0.5rem;
        vertical-align: middle;
      }
      .suggestion-text {
        font-size: 0.8125rem;
        color: #1e293b;
        white-space: normal;
        line-height: 1.4;
      }
      .searching-hint {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        color: #94a3b8;
        font-size: 0.8125rem;
      }

      /* ── Actions ── */
      mat-dialog-actions {
        padding: 0.875rem 1.5rem 1.25rem !important;
        gap: 0.75rem;
        border-top: 1px solid #f1f5f9;
        flex-direction: column-reverse;
        margin-top: 0;
      }
      .cancel-btn {
        border-radius: 8px !important;
        height: 44px;
        width: 100%;
        color: #64748b;
        border-color: #e2e8f0 !important;
        font-weight: 600;
      }
      .submit-btn {
        border-radius: 8px !important;
        height: 44px;
        width: 100%;
        background: #ea580c !important;
        color: white !important;
        font-weight: 700;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.375rem;
      }
      .submit-btn:hover:not(:disabled) {
        background: #f97316 !important;
      }
      .submit-btn:disabled {
        opacity: 0.6;
      }
      .submit-btn mat-icon {
        font-size: 1.125rem;
        width: 1.125rem;
        height: 1.125rem;
      }
      .button-spinner {
        display: inline-block;
        margin-right: 0.375rem;
      }

      /* ── Tablet+ ── */
      @media (min-width: 480px) {
        .dialog-header {
          padding: 1.5rem 1.75rem 1.375rem;
          gap: 1rem;
        }
        .header-icon-wrap {
          width: 44px;
          height: 44px;
        }
        .header-text h2 {
          font-size: 1.125rem;
        }
        mat-dialog-content {
          padding: 1.5rem 1.75rem !important;
        }
        mat-dialog-actions {
          padding: 1rem 1.75rem 1.5rem !important;
        }
      }

      @media (min-width: 600px) {
        .dialog-container {
          min-width: 520px;
          max-width: 600px;
        }
        mat-dialog-content {
          max-height: 70vh;
        }
        mat-dialog-actions {
          flex-direction: row;
          justify-content: flex-end;
        }
        .cancel-btn,
        .submit-btn {
          width: auto;
          padding: 0 1.5rem !important;
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
  private fb = inject(FormBuilder);
  private buildingService = inject(BuildingService);
  private institutionService = inject(InstitutionService);
  private geocodingService = inject(GeocodingService);
  private tenantService = inject(TenantService);
  private roleService = inject(RoleService);
  private snackBar = inject(MatSnackBar);
  dialogRef = inject<MatDialogRef<BuildingFormDialogComponent>>(MatDialogRef);
  data = inject<{
    mode: 'create' | 'edit';
    building?: BuildingResponse;
  }>(MAT_DIALOG_DATA);

  buildingForm: FormGroup;
  isSubmitting = false;
  institutions: Institution[] = [];
  companies: Tenant[] = [];

  /**
   * A platform admin's own tenant is the platform sentinel, which cannot own buildings, so they
   * must name the company. A tenant admin has exactly one and the backend derives it (GH-213).
   */
  readonly showCompanySelector = this.data.mode === 'create' && this.roleService.isPlatformAdmin();
  addressSuggestions: AddressSuggestion[] = [];
  isSearching = false;

  private destroy$ = new Subject<void>();
  private searchSubject = new Subject<string>();

  constructor() {
    this.buildingForm = this.fb.group({
      name: ['', Validators.required],
      address: [''],
      placeId: [''],
      latitude: [null],
      longitude: [null],
      totalFloors: [null],
      description: [''],
      institutionIds: [[]],
      tenantId: [null],
    });

    if (this.showCompanySelector) {
      this.buildingForm.get('tenantId')?.addValidators(Validators.required);
    }
  }

  ngOnInit(): void {
    this.loadInstitutions();
    this.setupAddressSearch();

    if (this.showCompanySelector) {
      this.loadCompanies();
    }

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
          return this.geocodingService.search(query);
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

  loadCompanies(): void {
    this.tenantService
      .getAllTenants()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (companies) => {
          this.companies = companies.filter((company) => company.id !== 0);
        },
        error: () => {
          this.snackBar.open('Could not load property companies', 'Close', { duration: 5000 });
        },
      });
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

    // Only on create, and only from a platform admin. Sending it on an edit is refused by the
    // backend, because moving a building between companies would re-home everything under it.
    if (this.showCompanySelector) {
      buildingData.tenantId = formValue.tenantId;
    }

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
