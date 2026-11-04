import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatStepper, MatStepperModule } from '@angular/material/stepper';
import { MatTableModule } from '@angular/material/table';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SelectionModel } from '@angular/cdk/collections';

import {
  AdminService,
  ApplicationResponse,
  initialsOf,
} from '../../../core/services/admin.service';
import { StagedAllocationService } from '../../../core/services/staged-allocation.service';
import { StagedAllocation, BatchAllocationProposal } from '../../../core/models/allocation.models';
import { ModernStatusBadgeComponent } from '../../../shared/modern-status-badge/modern-status-badge.component';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';

@Component({
  selector: 'app-staged-allocation',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatStepperModule,
    MatTableModule,
    MatPaginatorModule,
    MatCheckboxModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatChipsModule,
    MatTooltipModule,
    ModernStatusBadgeComponent,
    SkeletonLoaderComponent,
    EmptyStateComponent,
  ],
  templateUrl: './staged-allocation.component.html',
  styleUrls: ['./staged-allocation.component.scss'],
})
export class StagedAllocationComponent implements OnInit {
  private fb = inject(FormBuilder);
  private adminService = inject(AdminService);
  private stagedAllocationService = inject(StagedAllocationService);
  private snackBar = inject(MatSnackBar);

  @ViewChild('stepper') stepper!: MatStepper;

  // Step 1: Selection
  selectionForm: FormGroup;
  availableApplications: ApplicationResponse[] = [];
  selection = new SelectionModel<ApplicationResponse>(true, []);
  displayedColumnsSelection: string[] = [
    'select',
    'studentName',
    'applicationCode',
    'preferredBuilding',
    'status',
  ];
  isLoadingApplications = false;

  // Step 2: Proposal
  isProposing = false;
  batchId: string | null = null;
  proposalError: string | null = null;

  // Step 3: Review
  proposals: StagedAllocation[] = [];
  displayedColumnsReview: string[] = [
    'studentName',
    'room',
    'score',
    'warnings',
    'status',
    'actions',
  ];
  confirmedCount = 0;

  // Existing proposals (loaded from DB on init)
  existingProposals: StagedAllocation[] = [];
  isLoadingExistingProposals = false;

  pageIndex = 0;
  pageSize = 25;
  totalApplications = 0;

  constructor() {
    this.selectionForm = this.fb.group({
      selectedCount: [0, [Validators.required, Validators.min(1), Validators.max(50)]],
    });
  }

  ngOnInit(): void {
    this.loadApplications();
    this.loadExistingProposals();
  }

  // --- Step 1: Selection Logic ---

  /**
   * One page of the awaiting-allocation queue.
   *
   * <p>Was a fixed 100, which silently hid everyone past that: a queue longer than 100 could not
   * be worked through from this screen at all (GH-157).
   */
  loadApplications(): void {
    this.isLoadingApplications = true;
    this.adminService
      .searchApplications(
        {
          status: 'AWAITING_ALLOCATION',
          page: this.pageIndex,
          size: this.pageSize,
        },
        true
      )
      .subscribe({
        next: (page) => {
          this.availableApplications = page.content;
          this.totalApplications = page.totalElements;
          this.isLoadingApplications = false;
        },
        error: (err) => {
          console.error('Failed to load applications', err);
          this.isLoadingApplications = false;
          this.snackBar.open('Error loading applications', 'Close', { duration: 3000 });
        },
      });
  }

  onApplicationsPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    // Selections refer to rows on the page that is going away, so they are dropped rather than
    // silently carried into a proposal the admin can no longer see.
    this.selection.clear();
    this.loadApplications();
  }

  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.availableApplications.length;
    return numSelected === numRows;
  }

  toggleAllRows(): void {
    if (this.isAllSelected()) {
      this.selection.clear();
      return;
    }

    this.selection.select(...this.availableApplications);
    this.updateSelectionCount();
  }

  checkboxLabel(row?: ApplicationResponse): string {
    if (!row) {
      return `${this.isAllSelected() ? 'deselect' : 'select'} all`;
    }
    return `${this.selection.isSelected(row) ? 'deselect' : 'select'} row ${row.applicationCode}`;
  }

  onSelectionChange(): void {
    this.updateSelectionCount();
  }

  updateSelectionCount(): void {
    const count = this.selection.selected.length;
    this.selectionForm.patchValue({ selectedCount: count });
  }

  loadExistingProposals(): void {
    this.isLoadingExistingProposals = true;
    this.stagedAllocationService.getExistingProposals().subscribe({
      next: (page) => {
        this.existingProposals = page.content;
        this.isLoadingExistingProposals = false;
      },
      error: (err) => {
        console.error('Failed to load existing proposals', err);
        this.isLoadingExistingProposals = false;
      },
    });
  }

  confirmExistingAllocation(proposal: StagedAllocation): void {
    if (proposal.status !== 'PROPOSED') return;
    this.stagedAllocationService.confirmAllocation(proposal.id).subscribe({
      next: () => {
        proposal.status = 'CONFIRMED';
        this.confirmedCount++;
        this.snackBar.open('Allocation confirmed — student will now see the offer', 'Close', {
          duration: 3000,
        });
      },
      error: (err) => {
        console.error('Confirm failed', err);
        this.snackBar.open('Failed to confirm allocation', 'Close', { duration: 3000 });
      },
    });
  }

  rejectExistingAllocation(proposal: StagedAllocation): void {
    if (proposal.status !== 'PROPOSED') return;
    this.stagedAllocationService
      .rejectAllocation(proposal.id, 'Admin rejected from pending proposals')
      .subscribe({
        next: () => {
          proposal.status = 'REJECTED';
          this.snackBar.open('Allocation rejected', 'Close', { duration: 2000 });
        },
        error: (err) => {
          console.error('Reject failed', err);
          this.snackBar.open('Failed to reject allocation', 'Close', { duration: 3000 });
        },
      });
  }

  get pendingExistingProposals(): StagedAllocation[] {
    return this.existingProposals.filter((p) => p.status === 'PROPOSED');
  }

  // --- Step 2: Propose Logic ---

  generateBatchId(): void {
    // Format: BATCH-YYYYMMDD-HHMMSS
    const now = new Date();
    const timestamp = now
      .toISOString()
      .replace(/[-:T.]/g, '')
      .slice(0, 14);
    this.batchId = `BATCH-${timestamp}`;
  }

  proposeBatch(): void {
    if (this.selection.selected.length === 0) return;

    this.isProposing = true;
    this.proposalError = null;
    this.generateBatchId();

    const studentIds = this.selection.selected.map((app) => app.studentId);

    // Call service
    this.stagedAllocationService.proposeAllocation(studentIds, this.batchId!).subscribe({
      next: (response: BatchAllocationProposal) => {
        this.proposals = response.proposals;
        this.isProposing = false;
        this.stepper.next(); // Move to Step 3
      },
      error: (err) => {
        console.error('Proposal failed', err);
        this.isProposing = false;
        this.proposalError = 'Failed to generate proposals. Please try again.';
        this.snackBar.open('Proposal generation failed', 'Close', { duration: 5000 });
      },
    });
  }

  // --- Step 3: Review Logic ---

  confirmAllocation(proposal: StagedAllocation): void {
    if (proposal.status !== 'PROPOSED') return;

    this.stagedAllocationService.confirmAllocation(proposal.id).subscribe({
      next: () => {
        // Update local state
        proposal.status = 'CONFIRMED'; // Optimistic update or use returned object if mapped
        this.confirmedCount++;
        this.snackBar.open('Allocation confirmed', 'Close', { duration: 2000 });
      },
      error: (err) => {
        console.error('Confirm failed', err);
        this.snackBar.open('Failed to confirm allocation', 'Close', { duration: 3000 });
      },
    });
  }

  rejectAllocation(proposal: StagedAllocation): void {
    if (proposal.status !== 'PROPOSED') return;

    // Simple rejection for now, maybe add reason later
    this.stagedAllocationService
      .rejectAllocation(proposal.id, 'Admin skipped in batch review')
      .subscribe({
        next: () => {
          proposal.status = 'REJECTED';
          this.snackBar.open('Allocation skipped', 'Close', { duration: 2000 });
        },
        error: (err) => {
          console.error('Reject failed', err);
          this.snackBar.open('Failed to reject allocation', 'Close', { duration: 3000 });
        },
      });
  }

  // ── UI helpers ──────────────────────────────────────────────

  get hasPendingProposals(): boolean {
    return this.proposals.some((p) => p.status === 'PROPOSED');
  }

  getInitials(app: ApplicationResponse): string {
    return initialsOf(app.studentName);
  }

  getScoreRingStyle(score: number): { [key: string]: string } {
    const maxScore = 200;
    const pct = Math.min(100, Math.round((score / maxScore) * 100));
    const color = score >= 140 ? '#10b981' : score >= 70 ? '#f59e0b' : '#ef4444';
    return { '--score-pct': `${pct}`, '--ring-color': color };
  }

  confirmAllRemaining(): void {
    // Filter proposed only
    const pending = this.proposals.filter((p) => p.status === 'PROPOSED');
    if (pending.length === 0) return;

    if (this.batchId) {
      this.stagedAllocationService.confirmBatch(this.batchId).subscribe({
        next: (res) => {
          this.snackBar.open(`Confirmed ${res.confirmed} allocations`, 'Close', { duration: 3000 });
          // Refresh or mark all as confirmed locally
          this.proposals.forEach((p) => {
            if (p.status === 'PROPOSED') p.status = 'CONFIRMED';
          });
        },
        error: () => {
          this.snackBar.open('Batch confirm failed', 'Close', { duration: 3000 });
        },
      });
    }
  }
}
