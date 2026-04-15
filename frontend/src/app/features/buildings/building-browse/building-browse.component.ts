import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import {
  BuildingService,
  BuildingResponse,
  PageResponse,
} from '../../../core/services/building.service';
import { InstitutionService } from '../../../core/services/institution.service';
import { Institution } from '../../../core/models/institution.model';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';

export interface BrowseFilters {
  search: string;
  minPrice: number | null;
  maxPrice: number | null;
  available: boolean;
  institutionId: number | null;
}

@Component({
  selector: 'app-building-browse',
  templateUrl: './building-browse.component.html',
  styleUrls: ['./building-browse.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatChipsModule,
  ],
})
export class BuildingBrowseComponent implements OnInit, OnDestroy {
  private readonly buildingService = inject(BuildingService);
  private readonly institutionService = inject(InstitutionService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private readonly destroy$ = new Subject<void>();
  private readonly searchSubject = new Subject<string>();

  buildings: BuildingResponse[] = [];
  institutions: Institution[] = [];
  isLoading = false;
  isLoadingMore = false;
  error: string | null = null;

  // Pagination
  currentPage = 0;
  pageSize = 12;
  totalElements = 0;
  totalPages = 0;
  hasMore = false;

  // Filters
  filters: BrowseFilters = {
    search: '',
    minPrice: null,
    maxPrice: null,
    available: false,
    institutionId: null,
  };
  filtersVisible = false;

  ngOnInit(): void {
    this.loadInstitutions();
    this.initFiltersFromUrl();

    // Debounce search input
    this.searchSubject
      .pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => {
        this.resetAndLoad();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Initialization ────────────────────────────────────────

  private initFiltersFromUrl(): void {
    const params = this.route.snapshot.queryParams;
    this.filters.search = params['search'] || '';
    this.filters.minPrice = params['minPrice'] ? +params['minPrice'] : null;
    this.filters.maxPrice = params['maxPrice'] ? +params['maxPrice'] : null;
    this.filters.available = params['available'] === 'true';
    this.filters.institutionId = params['institutionId'] ? +params['institutionId'] : null;

    if (
      this.filters.minPrice ||
      this.filters.maxPrice ||
      this.filters.available ||
      this.filters.institutionId
    ) {
      this.filtersVisible = true;
    }

    this.loadBuildings(false);
  }

  private loadInstitutions(): void {
    this.institutionService.getInstitutions().subscribe({
      next: (institutions) => {
        this.institutions = institutions;
        this.cdr.markForCheck();
      },
      error: () => {
        // Non-critical — filter just won't have institution options
      },
    });
  }

  // ── Data Loading ──────────────────────────────────────────

  loadBuildings(append: boolean): void {
    if (append) {
      this.isLoadingMore = true;
    } else {
      this.isLoading = true;
    }
    this.error = null;

    this.buildingService
      .browseBuildingsPaginated({
        search: this.filters.search || undefined,
        minPrice: this.filters.minPrice ?? undefined,
        maxPrice: this.filters.maxPrice ?? undefined,
        available: this.filters.available || undefined,
        institutionId: this.filters.institutionId ?? undefined,
        page: this.currentPage,
        size: this.pageSize,
      })
      .subscribe({
        next: (response: PageResponse<BuildingResponse>) => {
          if (append) {
            this.buildings = [...this.buildings, ...response.content];
          } else {
            this.buildings = response.content;
          }
          this.totalElements = response.totalElements;
          this.totalPages = response.totalPages;
          this.hasMore = this.currentPage < this.totalPages - 1;
          this.isLoading = false;
          this.isLoadingMore = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.error = 'Failed to load buildings. Please try again.';
          this.isLoading = false;
          this.isLoadingMore = false;
          this.cdr.markForCheck();
        },
      });
  }

  // ── Search ────────────────────────────────────────────────

  onSearchInput(): void {
    this.searchSubject.next(this.filters.search);
  }

  clearSearch(): void {
    this.filters.search = '';
    this.resetAndLoad();
  }

  // ── Filters ───────────────────────────────────────────────

  toggleFilters(): void {
    this.filtersVisible = !this.filtersVisible;
  }

  applyFilters(): void {
    this.resetAndLoad();
  }

  clearFilters(): void {
    this.filters = {
      search: this.filters.search, // Keep search text
      minPrice: null,
      maxPrice: null,
      available: false,
      institutionId: null,
    };
    this.resetAndLoad();
  }

  get activeFilterCount(): number {
    let count = 0;
    if (this.filters.minPrice != null) count++;
    if (this.filters.maxPrice != null) count++;
    if (this.filters.available) count++;
    if (this.filters.institutionId != null) count++;
    return count;
  }

  // ── Pagination ────────────────────────────────────────────

  loadMore(): void {
    if (!this.hasMore || this.isLoadingMore) return;
    this.currentPage++;
    this.loadBuildings(true);
  }

  // ── URL Sync ──────────────────────────────────────────────

  private syncFiltersToUrl(): void {
    const queryParams: Record<string, string> = {};
    if (this.filters.search) queryParams['search'] = this.filters.search;
    if (this.filters.minPrice != null) queryParams['minPrice'] = this.filters.minPrice.toString();
    if (this.filters.maxPrice != null) queryParams['maxPrice'] = this.filters.maxPrice.toString();
    if (this.filters.available) queryParams['available'] = 'true';
    if (this.filters.institutionId != null)
      queryParams['institutionId'] = this.filters.institutionId.toString();

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      replaceUrl: true,
    });
  }

  private resetAndLoad(): void {
    this.currentPage = 0;
    this.syncFiltersToUrl();
    this.loadBuildings(false);
  }

  // ── Display Helpers ───────────────────────────────────────

  getPriceDisplay(building: BuildingResponse): string {
    if (building.minMonthlyRate == null) return 'Price on request';
    const min = building.minMonthlyRate;
    const max = building.maxMonthlyRate;
    if (max == null || min === max) {
      return `From R${this.formatNumber(min)}/month`;
    }
    return `R${this.formatNumber(min)} – R${this.formatNumber(max)}/month`;
  }

  getAvailabilityText(building: BuildingResponse): string {
    const count = building.availableRoomCount ?? 0;
    if (count === 0) return 'No rooms available';
    return `${count} room${count !== 1 ? 's' : ''} available`;
  }

  getAvailabilityClass(building: BuildingResponse): string {
    const count = building.availableRoomCount ?? 0;
    if (count === 0) return 'availability-none';
    if (count <= 2) return 'availability-limited';
    return 'availability-good';
  }

  getCapacityTypes(building: BuildingResponse): string[] {
    if (!building.rooms || building.rooms.length === 0) return [];
    const capacities = [...new Set(building.rooms.map((r) => r.capacity))].sort((a, b) => a - b);
    return capacities.map((c) => this.capacityLabel(c));
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
