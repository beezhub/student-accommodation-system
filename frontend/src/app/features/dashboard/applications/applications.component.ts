import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ApplicationService } from '../../../core/services/application.service';
import { Application } from '../../../core/models/application.model';

// Material imports
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

// Shared Components
import { ModernStatusBadgeComponent } from '../../../shared/modern-status-badge/modern-status-badge.component';

@Component({
  selector: 'app-applications',
  templateUrl: './applications.component.html',
  styleUrls: ['./applications.component.scss'],
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatTableModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    ModernStatusBadgeComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApplicationsComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly applicationService = inject(ApplicationService);
  private readonly cdr = inject(ChangeDetectorRef);

  currentUser: any;
  applications: Application[] = [];
  isLoading = false;

  // Material table
  readonly displayedColumns: readonly string[] = [
    'applicationId',
    'year',
    'status',
    'submittedDate',
    'type',
    'duration',
    'actions',
  ] as const;
  dataSource = new MatTableDataSource<Application>(this.applications);

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    this.loadApplications();
  }

  loadApplications(): void {
    this.isLoading = true;
    this.applicationService.getMyApplications().subscribe({
      next: (applications) => {
        // Sort by date descending (newest first)
        this.applications = applications.sort((a, b) => {
          const dateA = a.createdAt
            ? new Date(a.createdAt).getTime()
            : a.applicationDate
            ? new Date(a.applicationDate).getTime()
            : 0;
          const dateB = b.createdAt
            ? new Date(b.createdAt).getTime()
            : b.applicationDate
            ? new Date(b.applicationDate).getTime()
            : 0;
          return dateB - dateA;
        });
        this.dataSource.data = this.applications;
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to fetch applications:', err);
        this.applications = [];
        this.dataSource.data = [];
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  getStatusClass(status: string): string {
    switch (status.toLowerCase()) {
      case 'approved':
        return 'status-approved';
      case 'pending':
        return 'status-pending';
      case 'rejected':
        return 'status-rejected';
      default:
        return 'status-default';
    }
  }

  cancelApplication(applicationId: number) {
    // Note: Application cancellation is not currently supported by the backend API
    // This is a placeholder for future implementation
    alert('Application cancellation is not currently available. Please contact support if you need to cancel your application.');
  }
}
