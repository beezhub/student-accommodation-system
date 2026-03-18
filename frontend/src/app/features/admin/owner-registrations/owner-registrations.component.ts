import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  OwnerRegistrationAdminService,
  OwnerRegistrationSummary,
} from './owner-registration-admin.service';
import { OwnerRegistrationReviewDialogComponent } from './owner-registration-review-dialog/owner-registration-review-dialog.component';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';

@Component({
  selector: 'app-owner-registrations',
  templateUrl: './owner-registrations.component.html',
  styleUrls: ['./owner-registrations.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatChipsModule,
    MatDialogModule,
    MatSnackBarModule,
    MatPaginatorModule,
    MatTooltipModule,
    SkeletonLoaderComponent,
    EmptyStateComponent,
  ],
})
export class OwnerRegistrationsComponent implements OnInit {
  private service = inject(OwnerRegistrationAdminService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  registrations: OwnerRegistrationSummary[] = [];
  isLoading = true;

  displayedColumns = ['name', 'email', 'organisation', 'phone', 'submitted', 'status', 'actions'];

  pageSize = 10;
  pageIndex = 0;

  get pagedRegistrations(): OwnerRegistrationSummary[] {
    const start = this.pageIndex * this.pageSize;
    return this.registrations.slice(start, start + this.pageSize);
  }

  ngOnInit(): void {
    this.loadRegistrations();
  }

  loadRegistrations(): void {
    this.isLoading = true;
    this.service.getPendingRegistrations().subscribe({
      next: (data) => {
        this.registrations = data;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.snackBar.open('Failed to load registrations.', 'Close', {
          duration: 4000,
          panelClass: ['error-snackbar'],
        });
      },
    });
  }

  openReview(registration: OwnerRegistrationSummary): void {
    const ref = this.dialog.open(OwnerRegistrationReviewDialogComponent, {
      width: '560px',
      data: registration,
    });
    ref.afterClosed().subscribe((refreshNeeded) => {
      if (refreshNeeded) this.loadRegistrations();
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PENDING': return 'status-pending';
      case 'PENDING_REVIEW': return 'status-review';
      case 'APPROVED': return 'status-approved';
      case 'REJECTED': return 'status-rejected';
      default: return '';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'PENDING': return 'Pending Email';
      case 'PENDING_REVIEW': return 'Pending Review';
      case 'APPROVED': return 'Approved';
      case 'REJECTED': return 'Rejected';
      default: return status;
    }
  }
}
