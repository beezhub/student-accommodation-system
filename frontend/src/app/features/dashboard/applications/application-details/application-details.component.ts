import { Component, OnInit, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { ApplicationService } from '../../../../core/services/application.service';
import { AdminService, ApplicationResponse } from '../../../../core/services/admin.service';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

// Material imports
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

// Shared Components
import { ModernStatusBadgeComponent } from '../../../../shared/modern-status-badge/modern-status-badge.component';

@Component({
  selector: 'app-application-details',
  templateUrl: './application-details.component.html',
  styleUrls: ['./application-details.component.css'],
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatDividerModule,
    MatTableModule,
    MatChipsModule,
    MatTooltipModule,
    MatMenuModule,
    MatSnackBarModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    ModernStatusBadgeComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApplicationDetailsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly applicationService = inject(ApplicationService);
  private readonly adminService = inject(AdminService);
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder);

  currentUser: any;
  applicationId: number = 0;
  application: ApplicationResponse | null = null;
  isLoading = false;
  isAdmin = false;

  timeline: Array<{
    date: string;
    status: string;
    description: string;
    icon: string;
  }> = [];

  // Table columns definition for the documents table
  readonly displayedColumns: string[] = ['name', 'status', 'date', 'actions'];

  isSmallScreen = false;
  showRejectDialog = false;
  showApproveDialog = false;
  remarksForm: FormGroup;

  constructor() {
    this.remarksForm = this.fb.group({
      remarks: ['', Validators.required],
    });
  }

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    this.isAdmin = this.currentUser?.userRole === 'ADMIN';

    this.route.params.subscribe((params) => {
      this.applicationId = +params['id'];
      if (this.applicationId) {
        this.loadApplicationDetails();
      }
    });

    this.breakpointObserver
      .observe([Breakpoints.Handset, Breakpoints.Tablet])
      .subscribe((result) => {
        this.isSmallScreen = result.matches;
      });
  }

  loadApplicationDetails(): void {
    this.isLoading = true;
    this.applicationService.getApplicationById(this.applicationId).subscribe({
      next: (application) => {
        // Map the basic application data
        this.application = {
          id: application.id,
          applicationCode: application.applicationCode,
          applicationDate: application.applicationDate,
          status: application.status,
          studentId: 0,
          studentFirstName: '',
          studentLastName: '',
          studentEmail: '',
          createdAt: '',
          updatedAt: '',
        };

        this.buildTimeline();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading application:', error);
        this.snackBar.open('Failed to load application details', 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  buildTimeline(): void {
    if (!this.application) return;

    this.timeline = [];

    // Application submitted
    this.timeline.push({
      date: this.application.applicationDate,
      status: 'Application Submitted',
      description: 'Your application has been received and is being processed',
      icon: 'send',
    });

    // Status-based timeline events
    if (this.application.status === 'APPROVED') {
      this.timeline.push({
        date: this.application.updatedAt || this.application.applicationDate,
        status: 'Application Approved',
        description: 'Your application has been approved',
        icon: 'check_circle',
      });
    } else if (this.application.status === 'REJECTED') {
      this.timeline.push({
        date: this.application.updatedAt || this.application.applicationDate,
        status: 'Application Rejected',
        description: this.application.remarks || 'Your application has been rejected',
        icon: 'cancel',
      });
    } else if (this.application.status === 'PENDING') {
      this.timeline.push({
        date: new Date().toISOString(),
        status: 'Under Review',
        description: 'Application is being reviewed by the housing team',
        icon: 'hourglass_empty',
      });
    }
  }

  getStatusClass(status: string): string {
    switch (status.toUpperCase()) {
      case 'VERIFIED':
      case 'APPROVED':
        return 'status-success';
      case 'PENDING':
      case 'UNDER REVIEW':
        return 'status-warning';
      case 'REJECTED':
        return 'status-danger';
      default:
        return 'status-default';
    }
  }

  canApproveOrReject(): boolean {
    return this.isAdmin && this.application?.status === 'PENDING';
  }

  openApproveDialog(): void {
    this.showApproveDialog = true;
    this.remarksForm.reset();
    this.remarksForm.get('remarks')?.clearValidators();
    this.remarksForm.get('remarks')?.updateValueAndValidity();
  }

  openRejectDialog(): void {
    this.showRejectDialog = true;
    this.remarksForm.reset();
    this.remarksForm.get('remarks')?.setValidators(Validators.required);
    this.remarksForm.get('remarks')?.updateValueAndValidity();
  }

  closeDialogs(): void {
    this.showApproveDialog = false;
    this.showRejectDialog = false;
    this.remarksForm.reset();
  }

  approveApplication(): void {
    if (!this.application) return;

    const remarks = this.remarksForm.get('remarks')?.value || '';

    this.adminService.updateApplicationStatus(this.application.id, 'APPROVED', remarks).subscribe({
      next: () => {
        this.snackBar.open('Application approved successfully', 'Close', { duration: 3000 });
        this.closeDialogs();
        this.loadApplicationDetails();
      },
      error: (error) => {
        console.error('Error approving application:', error);
        this.snackBar.open(error.error?.message || 'Failed to approve application', 'Close', {
          duration: 5000,
        });
      },
    });
  }

  rejectApplication(): void {
    if (!this.application || !this.remarksForm.valid) return;

    const remarks = this.remarksForm.get('remarks')?.value;

    this.adminService.updateApplicationStatus(this.application.id, 'REJECTED', remarks).subscribe({
      next: () => {
        this.snackBar.open('Application rejected', 'Close', { duration: 3000 });
        this.closeDialogs();
        this.loadApplicationDetails();
      },
      error: (error) => {
        console.error('Error rejecting application:', error);
        this.snackBar.open(error.error?.message || 'Failed to reject application', 'Close', {
          duration: 5000,
        });
      },
    });
  }

  getTimelineIcon(status: string): string {
    const statusLower = status.toLowerCase();
    if (statusLower.includes('submitted')) return 'send';
    if (statusLower.includes('verified')) return 'fact_check';
    if (statusLower.includes('review')) return 'hourglass_empty';
    if (statusLower.includes('approved')) return 'check_circle';
    if (statusLower.includes('rejected')) return 'cancel';
    return 'info';
  }

  goBack() {
    if (this.isAdmin) {
      this.router.navigate(['/admin/applications']);
    } else {
      this.router.navigate(['/applications']);
    }
  }
}
