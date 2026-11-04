import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
  AdminService,
  ApplicationResponse,
  initialsOf,
  PageResponse,
} from '../../../core/services/admin.service';
import { BuildingService, BuildingResponse } from '../../../core/services/building.service';
import { StagedAllocationService } from '../../../core/services/staged-allocation.service';
import { ApplicationService } from '../../../core/services/application.service';

import { Observable, startWith, map, debounceTime, distinctUntilChanged } from 'rxjs';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatChipsModule } from '@angular/material/chips';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';

// Modern UI Components
import { ModernStatusBadgeComponent } from '../../../shared/modern-status-badge/modern-status-badge.component';
import { ErrorHandlingService, ParsedError } from '../../../core/services/error-handling.service';
import { ErrorDisplayComponent } from '../../../shared/error-display/error-display.component';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import {
  ApplicationDetailDialogComponent,
  ApplicationDetailDialogData,
  ApplicationDetailDialogResult,
} from './application-detail-dialog/application-detail-dialog.component';
import {
  AllocationChoiceDialogComponent,
  AllocationChoiceDialogData,
  AllocationChoiceResult,
} from './allocation-choice-dialog/allocation-choice-dialog.component';
import {
  PreferencesDialogComponent,
  PreferencesDialogData,
} from './preferences-dialog/preferences-dialog.component';
import { askForReason } from '../../../shared/components/reason-dialog/reason-dialog.component';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';

@Component({
  selector: 'app-application-review',
  templateUrl: './application-review.component.html',
  styleUrls: ['./application-review.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatProgressBarModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatAutocompleteModule,
    MatChipsModule,
    MatCheckboxModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatDialogModule,
    MatDividerModule,
    MatDatepickerModule,
    MatNativeDateModule,
    ModernStatusBadgeComponent,
    ErrorDisplayComponent,
    SkeletonLoaderComponent,
    EmptyStateComponent,
  ],
})
export class ApplicationReviewComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  private adminService = inject(AdminService);
  private applicationService = inject(ApplicationService);
  private buildingService = inject(BuildingService);
  private stagedAllocationService = inject(StagedAllocationService);
  private fb = inject(FormBuilder);
  private snackBar = inject(MatSnackBar);
  private errorService = inject(ErrorHandlingService);
  private dialog = inject(MatDialog);

  applications: ApplicationResponse[] = [];
  displayedColumns: string[] = [
    'studentName',
    'applicationCode',
    'preferredBuilding',
    'allocationScore',
    'documentStatus',
    'allocationStatus',
    'status',
    'applicationDate',
    'actions',
  ];

  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;

  // Filter panel state
  filtersVisible = false;
  activeFilterCount = 0;

  // Metric counts
  counts = { total: 0, pending: 0, awaitingAllocation: 0, allocated: 0 };

  filterForm: FormGroup;

  statuses = [
    { value: 'PENDING', label: 'Pending' },
    { value: 'APPROVED', label: 'Approved' },
    { value: 'REJECTED', label: 'Rejected' },
    { value: 'AWAITING_ALLOCATION', label: 'Awaiting Allocation' },
    { value: 'ALLOCATED', label: 'Allocated' },
  ];

  allocationStatuses = [
    { value: 'PENDING', label: 'Pending' },
    { value: 'ALLOCATED', label: 'Allocated' },
    { value: 'REJECTED', label: 'Rejected' },
  ];

  buildings: BuildingResponse[] = [];
  filteredBuildings$!: Observable<BuildingResponse[]>;

  loadError: ParsedError | null = null;
  isRetrying = false;

  constructor() {
    this.filterForm = this.fb.group({
      applicationCode: [''],
      studentName: [''],
      status: ['PENDING'],
      allocationStatus: [''],
      preferredBuilding: [''],
      startDate: [''],
      endDate: [''],
    });
  }

  ngOnInit(): void {
    this.loadBuildings();
    this.setupBuildingAutocomplete();
    this.loadApplications();
    this.loadCounts();
    // valueChanges never completes, so without this the subscription outlives the component
    // and keeps it, its form and its closures alive for the life of the app (GH-157).
    this.filterForm.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.computeActiveFilterCount());
  }

  toggleFilters(): void {
    this.filtersVisible = !this.filtersVisible;
  }

  computeActiveFilterCount(): void {
    const v = this.filterForm.value;
    let count = 0;
    if (v.applicationCode) count++;
    if (v.studentName) count++;
    if (v.status) count++;
    if (v.allocationStatus) count++;
    if (v.preferredBuilding) count++;
    if (v.startDate) count++;
    if (v.endDate) count++;
    this.activeFilterCount = count;
  }

  loadCounts(): void {
    const fetch = (status: string) =>
      this.adminService.searchApplications({ status, page: 0, size: 1 });

    fetch('PENDING').subscribe({
      next: (r) => (this.counts.pending = r.totalElements),
      error: () => {},
    });
    fetch('AWAITING_ALLOCATION').subscribe({
      next: (r) => (this.counts.awaitingAllocation = r.totalElements),
      error: () => {},
    });
    fetch('ALLOCATED').subscribe({
      next: (r) => (this.counts.allocated = r.totalElements),
      error: () => {},
    });
  }

  loadBuildings(): void {
    this.buildingService.getAllBuildingsNoPagination().subscribe({
      next: (buildings) => {
        this.buildings = buildings;
      },
      error: (error) => {
        console.error('Failed to load buildings:', error);
      },
    });
  }

  setupBuildingAutocomplete(): void {
    this.filteredBuildings$ = this.filterForm.get('preferredBuilding')!.valueChanges.pipe(
      startWith(''),
      debounceTime(200),
      distinctUntilChanged(),
      map((value) => this.filterBuildings(value || ''))
    );
  }

  filterBuildings(value: string): BuildingResponse[] {
    const filterValue = value.toLowerCase();
    return this.buildings.filter((building) => building.name.toLowerCase().includes(filterValue));
  }

  displayBuilding(building: BuildingResponse | string): string {
    if (typeof building === 'string') return building;
    return building ? building.name : '';
  }

  onBuildingSelected(event: MatAutocompleteSelectedEvent): void {
    const building = event.option.value;
    if (typeof building === 'object') {
      this.filterForm.get('preferredBuilding')!.setValue(building.name);
    }
  }

  loadApplications(): void {
    this.isLoading = true;
    this.loadError = null;
    this.isRetrying = true;

    const formValue = this.filterForm.value;
    const filters = {
      applicationCode: formValue.applicationCode,
      studentName: formValue.studentName,
      status: formValue.status,
      allocationStatus: formValue.allocationStatus || undefined,
      preferredBuilding:
        formValue.preferredBuilding && typeof formValue.preferredBuilding === 'object'
          ? formValue.preferredBuilding.name
          : formValue.preferredBuilding || undefined,
      startDate: formValue.startDate ? this.formatDate(formValue.startDate) : undefined,
      endDate: formValue.endDate ? this.formatDate(formValue.endDate) : undefined,
      page: this.currentPage,
      size: this.pageSize,
    };

    this.adminService.searchApplications(filters).subscribe({
      next: (response: PageResponse<ApplicationResponse>) => {
        this.applications = response.content;
        this.totalElements = response.totalElements;
        this.counts.total = response.totalElements;
        this.isLoading = false;
        this.isRetrying = false;
      },
      error: (error) => {
        console.error('Failed to load applications:', error);
        this.isLoading = false;
        this.isRetrying = false;
        this.loadError = this.errorService.parseError(error);
      },
    });
  }

  retryLoadApplications(): void {
    this.loadApplications();
  }
  dismissLoadError(): void {
    this.loadError = null;
  }

  onFilter(): void {
    this.currentPage = 0;
    this.loadApplications();
  }

  onClearFilters(): void {
    this.filterForm.reset({ status: 'PENDING' });
    this.activeFilterCount = 0;
    this.currentPage = 0;
    this.loadApplications();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadApplications();
  }

  updateStatus(application: ApplicationResponse, status: string): void {
    this.adminService.updateApplicationStatus(application.id, status).subscribe({
      next: () => {
        this.errorService.showSuccess(`Application ${status.toLowerCase()} successfully`);
        this.loadApplications();
      },
      error: (error) => {
        const parsedError = this.errorService.parseError(error);
        this.errorService.showErrorSnackbar(parsedError);
      },
    });
  }

  getStatusClass(status: string): string {
    return status.toLowerCase();
  }

  getStudentInitials(app: ApplicationResponse): string {
    return initialsOf(app.studentName) || '?';
  }

  getScoreRingStyle(score: number | null | undefined): Record<string, string> {
    if (score === null || score === undefined) return {};
    const pct = Math.min(100, Math.max(0, score));
    const color = score >= 80 ? '#059669' : score >= 50 ? '#d97706' : '#dc2626';
    return { '--score-pct': `${pct}`, '--ring-color': color };
  }

  private formatDate(date: Date | string): string {
    if (!date) return '';
    if (typeof date === 'string') return date;
    const d = new Date(date);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(
      d.getDate()
    ).padStart(2, '0')}`;
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'pending_actions';
      case 'APPROVED':
        return 'check_circle';
      case 'REJECTED':
        return 'cancel';
      default:
        return 'help';
    }
  }

  openDetailDialog(application: ApplicationResponse): void {
    const dialogRef = this.dialog.open(ApplicationDetailDialogComponent, {
      width: '850px',
      maxWidth: '96vw',
      maxHeight: '90vh',
      data: { applicationId: application.id } as ApplicationDetailDialogData,
      disableClose: false,
    });

    dialogRef.afterClosed().subscribe((result: ApplicationDetailDialogResult | undefined) => {
      if (
        result &&
        (result.action === 'allocated' || result.action === 'rejected' || result.action === 'saved')
      ) {
        this.loadApplications();
      }
    });
  }

  openAllocationDialog(application: ApplicationResponse): void {
    const dialogRef = this.dialog.open(AllocationChoiceDialogComponent, {
      width: '650px',
      maxWidth: '95vw',
      panelClass: 'allocation-choice-dialog',
      data: {
        applicationId: application.id,
        studentId: application.studentId,
      } as AllocationChoiceDialogData,
    });

    dialogRef.afterClosed().subscribe((result: AllocationChoiceResult | undefined) => {
      if (result && result.action === 'allocated') {
        this.loadApplications();
      }
    });
  }

  openPreferencesDialog(application: ApplicationResponse): void {
    this.dialog.open(PreferencesDialogComponent, {
      width: '500px',
      data: { application } as PreferencesDialogData,
    });
  }

  getScoreClass(score: number | undefined): string {
    if (score === undefined || score === null) return '';
    if (score >= 80) return 'score-high';
    if (score >= 50) return 'score-medium';
    return 'score-low';
  }

  runAutoAllocation(application: ApplicationResponse): void {
    if (application.status !== 'AWAITING_ALLOCATION') {
      this.errorService.showWarning(
        'Application must be in "Awaiting Allocation" status for auto-allocation.'
      );
      return;
    }

    const batchId = `auto-${application.id}-${Date.now()}`;
    this.snackBar.open('Running auto-allocation...', 'Close', { duration: 2000 });

    this.stagedAllocationService.proposeAllocation([application.studentId], batchId).subscribe({
      next: (proposal) => {
        if (proposal.proposals && proposal.proposals.length > 0) {
          this.stagedAllocationService.confirmAllocation(proposal.proposals[0].id).subscribe({
            next: () => {
              this.errorService.showSuccess('Room allocated successfully!');
              this.loadApplications();
            },
            error: (error) => {
              this.errorService.showErrorSnackbar(this.errorService.parseError(error));
            },
          });
        } else {
          this.enqueueWaitingListForAutoAllocation(application);
        }
      },
      error: (error) => {
        const parsedError = this.errorService.parseError(error);
        if (
          parsedError.message.includes('No eligible') ||
          parsedError.message.includes('no room') ||
          parsedError.message.includes('No available rooms')
        ) {
          this.enqueueWaitingListForAutoAllocation(application);
        } else {
          this.errorService.showErrorSnackbar(parsedError);
        }
      },
    });
  }

  private enqueueWaitingListForAutoAllocation(application: ApplicationResponse): void {
    this.stagedAllocationService
      .enqueueWaitingList(application.studentId, application.id)
      .subscribe({
        next: (res) => {
          this.errorService.showWarning(
            res.message || 'No suitable room found. Student added to waiting list.'
          );
          this.loadApplications();
        },
        error: () => {
          this.errorService.showWarning('No suitable room found for auto-allocation.');
        },
      });
  }

  markUnderReview(application: ApplicationResponse): void {
    this.applicationService.markUnderReview(application.id).subscribe({
      next: () => {
        this.errorService.showSuccess('Application marked as Under Review');
        this.loadApplications();
      },
      error: (error) => {
        this.errorService.showErrorSnackbar(this.errorService.parseError(error));
      },
    });
  }

  requestChanges(application: ApplicationResponse): void {
    // The reason reaches the student as the whole explanation of what to fix, so it is validated
    // rather than accepted as whatever `prompt` returned — which included a single space (GH-157).
    askForReason(this.dialog, {
      title: 'Request changes',
      message: 'The student sees this, and it is the only guidance they get on what to change.',
      label: 'What needs to change?',
      placeholder: 'e.g. The proof of registration is for the wrong academic year.',
      confirmLabel: 'Request changes',
    }).subscribe((reason) => {
      if (!reason) return;
      this.applicationService.requestChanges(application.id, reason).subscribe({
        next: () => {
          this.errorService.showSuccess('Changes requested successfully');
          this.loadApplications();
        },
        error: (error) => {
          this.errorService.showErrorSnackbar(this.errorService.parseError(error));
        },
      });
    });
  }
}
