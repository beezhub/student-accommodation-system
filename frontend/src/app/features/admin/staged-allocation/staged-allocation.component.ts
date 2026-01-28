import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatStepper, MatStepperModule } from '@angular/material/stepper';
import { MatTableModule } from '@angular/material/table';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { SelectionModel } from '@angular/cdk/collections';

import { AdminService, ApplicationResponse } from '../../../core/services/admin.service';
import { StagedAllocationService } from '../../../core/services/staged-allocation.service';
import { StagedAllocation, BatchAllocationProposal } from '../../../core/models/allocation.models';
import { ModernStatusBadgeComponent } from '../../../shared/modern-status-badge/modern-status-badge.component';

@Component({
  selector: 'app-staged-allocation',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatStepperModule,
    MatTableModule,
    MatCheckboxModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatChipsModule,
    ModernStatusBadgeComponent
  ],
  templateUrl: './staged-allocation.component.html',
  styleUrls: ['./staged-allocation.component.css']
})
export class StagedAllocationComponent implements OnInit {
  @ViewChild('stepper') stepper!: MatStepper;

  // Step 1: Selection
  selectionForm: FormGroup;
  availableApplications: ApplicationResponse[] = [];
  selection = new SelectionModel<ApplicationResponse>(true, []);
  displayedColumnsSelection: string[] = ['select', 'studentName', 'applicationCode', 'preferredBuilding', 'status'];
  isLoadingApplications = false;

  // Step 2: Proposal
  isProposing = false;
  batchId: string | null = null;
  proposalError: string | null = null;

  // Step 3: Review
  proposals: StagedAllocation[] = [];
  displayedColumnsReview: string[] = ['studentName', 'room', 'score', 'warnings', 'status', 'actions'];
  confirmedCount = 0;
  
  constructor(
    private fb: FormBuilder,
    private adminService: AdminService,
    private stagedAllocationService: StagedAllocationService,
    private snackBar: MatSnackBar
  ) {
    this.selectionForm = this.fb.group({
      selectedCount: [0, [Validators.required, Validators.min(1), Validators.max(50)]]
    });
  }

  ngOnInit(): void {
    this.loadApplications();
  }

  // --- Step 1: Selection Logic ---

  loadApplications(): void {
    this.isLoadingApplications = true;
    this.adminService.searchApplications({ status: 'AWAITING_ALLOCATION', size: 100 }).subscribe({
      next: (page) => {
        this.availableApplications = page.content;
        this.isLoadingApplications = false;
      },
      error: (err) => {
        console.error('Failed to load applications', err);
        this.isLoadingApplications = false;
        this.snackBar.open('Error loading applications', 'Close', { duration: 3000 });
      }
    });
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

  // --- Step 2: Propose Logic ---

  generateBatchId(): void {
    // Format: BATCH-YYYYMMDD-HHMMSS
    const now = new Date();
    const timestamp = now.toISOString().replace(/[-:T.]/g, '').slice(0, 14);
    this.batchId = `BATCH-${timestamp}`;
  }

  proposeBatch(): void {
    if (this.selection.selected.length === 0) return;
    
    this.isProposing = true;
    this.proposalError = null;
    this.generateBatchId();

    const studentIds = this.selection.selected.map(app => app.studentId);

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
      }
    });
  }

  // --- Step 3: Review Logic ---

  confirmAllocation(proposal: StagedAllocation): void {
    if (proposal.status !== 'PROPOSED') return;

    this.stagedAllocationService.confirmAllocation(proposal.id).subscribe({
      next: (result) => {
        // Update local state
        proposal.status = 'CONFIRMED'; // Optimistic update or use returned object if mapped
        this.confirmedCount++;
        this.snackBar.open('Allocation confirmed', 'Close', { duration: 2000 });
      },
      error: (err) => {
        console.error('Confirm failed', err);
        this.snackBar.open('Failed to confirm allocation', 'Close', { duration: 3000 });
      }
    });
  }

  rejectAllocation(proposal: StagedAllocation): void {
     if (proposal.status !== 'PROPOSED') return;
     
     // Simple rejection for now, maybe add reason later
     this.stagedAllocationService.rejectAllocation(proposal.id, 'Admin skipped in batch review').subscribe({
       next: () => {
         proposal.status = 'REJECTED';
         this.snackBar.open('Allocation skipped', 'Close', { duration: 2000 });
       },
       error: (err) => {
         console.error('Reject failed', err);
         this.snackBar.open('Failed to reject allocation', 'Close', { duration: 3000 });
       }
     });
  }

  confirmAllRemaining(): void {
    // Filter proposed only
    const pending = this.proposals.filter(p => p.status === 'PROPOSED');
    if (pending.length === 0) return;

    if (this.batchId) {
        this.stagedAllocationService.confirmBatch(this.batchId).subscribe({
            next: (res) => {
                this.snackBar.open(`Confirmed ${res.confirmed} allocations`, 'Close', { duration: 3000 });
                // Refresh or mark all as confirmed locally
                this.proposals.forEach(p => {
                    if (p.status === 'PROPOSED') p.status = 'CONFIRMED';
                });
            },
            error: (err) => {
                this.snackBar.open('Batch confirm failed', 'Close', { duration: 3000 });
            }
        });
    }
  }
}
