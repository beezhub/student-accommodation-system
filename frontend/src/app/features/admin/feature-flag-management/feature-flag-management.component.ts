import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FeatureFlagService, FeatureFlag } from '../../../core/services/feature-flag.service';
import { ErrorHandlingService } from '../../../core/services/error-handling.service';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';

@Component({
  selector: 'app-feature-flag-management',
  templateUrl: './feature-flag-management.component.html',
  styleUrls: ['./feature-flag-management.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatIconModule,
    MatButtonModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    SkeletonLoaderComponent,
    EmptyStateComponent,
  ],
})
export class FeatureFlagManagementComponent implements OnInit {
  private readonly featureFlagService = inject(FeatureFlagService);
  private readonly errorHandlingService = inject(ErrorHandlingService);
  private readonly cdr = inject(ChangeDetectorRef);

  flags: FeatureFlag[] = [];
  loading = true;
  displayedColumns = ['name', 'description', 'group', 'status'];

  ngOnInit(): void {
    this.loadFlags();
  }

  loadFlags(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.featureFlagService.getAll().subscribe({
      next: (flags) => {
        this.flags = flags;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.errorHandlingService.handleError(err);
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  toggleFlag(flag: FeatureFlag): void {
    const originalState = flag.enable;
    flag.enable = !flag.enable;
    this.cdr.markForCheck();

    const request$ = flag.enable
      ? this.featureFlagService.enable(flag.uid)
      : this.featureFlagService.disable(flag.uid);

    request$.subscribe({
      next: () => {
        this.errorHandlingService.showSuccess(
          `Feature flag "${flag.uid}" ${flag.enable ? 'enabled' : 'disabled'}.`
        );
      },
      error: (err) => {
        flag.enable = originalState;
        this.cdr.markForCheck();
        this.errorHandlingService.handleError(err);
      },
    });
  }
}
