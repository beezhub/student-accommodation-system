import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { AdminService, ApplicationResponse, PageResponse } from '../../../core/services/admin.service';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SelectionModel } from '@angular/cdk/collections';

@Component({
  selector: 'app-application-review',
  templateUrl: './application-review.component.html',
  styleUrls: ['./application-review.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    MatCheckboxModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
  ],
})
export class ApplicationReviewComponent implements OnInit {
  applications: ApplicationResponse[] = [];
  displayedColumns: string[] = [
    'select',
    'applicationCode',
    'studentName',
    'studentEmail',
    'applicationDate',
    'status',
    'actions',
  ];

  isLoading = false;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;

  filterForm: FormGroup;
  selection = new SelectionModel<ApplicationResponse>(true, []);

  statuses = ['PENDING', 'APPROVED', 'REJECTED'];

  constructor(
    private adminService: AdminService,
    private fb: FormBuilder,
    private snackBar: MatSnackBar
  ) {
    this.filterForm = this.fb.group({
      applicationCode: [''],
      studentName: [''],
      status: [''],
      startDate: [''],
      endDate: [''],
    });
  }

  ngOnInit(): void {
    this.loadApplications();
  }

  loadApplications(): void {
    this.isLoading = true;
    const filters = {
      ...this.filterForm.value,
      page: this.currentPage,
      size: this.pageSize,
    };

    this.adminService.searchApplications(filters).subscribe({
      next: (response: PageResponse<ApplicationResponse>) => {
        this.applications = response.content;
        this.totalElements = response.totalElements;
        this.isLoading = false;
        this.selection.clear();
      },
      error: (error) => {
        console.error('Failed to load applications:', error);
        this.snackBar.open('Failed to load applications', 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  onFilter(): void {
    this.currentPage = 0;
    this.loadApplications();
  }

  onClearFilters(): void {
    this.filterForm.reset();
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
        this.snackBar.open(`Application ${status.toLowerCase()} successfully`, 'Close', {
          duration: 3000,
        });
        this.loadApplications();
      },
      error: (error) => {
        console.error('Failed to update status:', error);
        this.snackBar.open('Failed to update application status', 'Close', {
          duration: 3000,
        });
      },
    });
  }

  bulkApprove(): void {
    if (this.selection.isEmpty()) {
      this.snackBar.open('Please select applications to approve', 'Close', { duration: 3000 });
      return;
    }

    const selectedIds = this.selection.selected.map(app => app.id);
    let completed = 0;
    let failed = 0;

    selectedIds.forEach(id => {
      this.adminService.updateApplicationStatus(id, 'APPROVED').subscribe({
        next: () => {
          completed++;
          if (completed + failed === selectedIds.length) {
            this.finalizeBulkOperation(completed, failed, 'approved');
          }
        },
        error: () => {
          failed++;
          if (completed + failed === selectedIds.length) {
            this.finalizeBulkOperation(completed, failed, 'approved');
          }
        },
      });
    });
  }

  bulkReject(): void {
    if (this.selection.isEmpty()) {
      this.snackBar.open('Please select applications to reject', 'Close', { duration: 3000 });
      return;
    }

    const selectedIds = this.selection.selected.map(app => app.id);
    let completed = 0;
    let failed = 0;

    selectedIds.forEach(id => {
      this.adminService.updateApplicationStatus(id, 'REJECTED').subscribe({
        next: () => {
          completed++;
          if (completed + failed === selectedIds.length) {
            this.finalizeBulkOperation(completed, failed, 'rejected');
          }
        },
        error: () => {
          failed++;
          if (completed + failed === selectedIds.length) {
            this.finalizeBulkOperation(completed, failed, 'rejected');
          }
        },
      });
    });
  }

  private finalizeBulkOperation(completed: number, failed: number, action: string): void {
    if (failed === 0) {
      this.snackBar.open(
        `Successfully ${action} ${completed} application(s)`,
        'Close',
        { duration: 3000 }
      );
    } else {
      this.snackBar.open(
        `${completed} application(s) ${action}, ${failed} failed`,
        'Close',
        { duration: 5000 }
      );
    }
    this.loadApplications();
  }

  isAllSelected(): boolean {
    return this.selection.selected.length === this.applications.length && this.applications.length > 0;
  }

  toggleAllRows(): void {
    if (this.isAllSelected()) {
      this.selection.clear();
    } else {
      this.applications.forEach(row => this.selection.select(row));
    }
  }

  getStatusClass(status: string): string {
    return status.toLowerCase();
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
}
