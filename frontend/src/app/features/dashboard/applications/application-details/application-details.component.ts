import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  OnInit,
  inject,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { ApplicationService } from '../../../../core/services/application.service';
import { AdminService, ApplicationResponse } from '../../../../core/services/admin.service';
import { RoleService } from '../../../../core/services/role.service';
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
  styleUrls: ['./application-details.component.scss'],
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
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly roleService = inject(RoleService);
  private readonly applicationService = inject(ApplicationService);
  private readonly adminService = inject(AdminService);
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);

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
    this.isAdmin = this.roleService.isPlatformAdmin();

    this.route.params.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      this.applicationId = +params['id'];
      if (this.applicationId) {
        this.loadApplicationDetails();
      }
    });

    this.breakpointObserver
      .observe([Breakpoints.Handset, Breakpoints.Tablet])
      .subscribe((result) => {
        this.isSmallScreen = result.matches;
        this.cdr.markForCheck();
      });
  }

  loadApplicationDetails(): void {
    this.isLoading = true;
    this.cdr.markForCheck(); // Mark explicitly for loading state

    this.applicationService.getApplicationFullDetails(this.applicationId).subscribe({
      next: (fullDetails) => {
        // Map ApplicationFullDetails to ApplicationResponse structure for compatibility
        this.application = {
          id: fullDetails.id,
          applicationCode: fullDetails.applicationCode,
          applicationDate: fullDetails.applicationDate,
          status: fullDetails.status,
          remarks: fullDetails.remarks,
          studentId: fullDetails.studentProfile.id,
          studentFirstName: fullDetails.studentProfile.firstName,
          studentLastName: fullDetails.studentProfile.lastName,
          studentEmail: fullDetails.studentProfile.email,
          // Optional fields
          preferredBuildingName: fullDetails.preferredBuildingName,
          preferredBuildings: fullDetails.preferredBuildings,
          requiresAccessibility: fullDetails.requiresAccessibility,
          specialRequirements: fullDetails.specialRequirements,
          courseName: fullDetails.courseName,
          allocationScore: 0,
          createdAt: fullDetails.createdAt,
          updatedAt: fullDetails.updatedAt,
        } as ApplicationResponse;

        this.buildTimeline();
        this.isLoading = false;
        this.cdr.markForCheck(); // Mark for check after data update
      },
      error: (error) => {
        console.error('Error loading application:', error);
        this.snackBar.open('Failed to load application details', 'Close', { duration: 3000 });
        this.isLoading = false;
        this.cdr.markForCheck(); // Mark for check after error
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
    } else if (this.application.status === 'ALLOCATED') {
      this.timeline.push({
        date: this.application.updatedAt || this.application.applicationDate,
        status: 'Room Allocated',
        description: `Room allocation complete. Building: ${
          this.application.preferredBuildingName || 'Assigned Building'
        }`,
        icon: 'home',
      });
    }
  }

  getStatusClass(status: string): string {
    switch (status.toUpperCase()) {
      case 'VERIFIED':
      case 'APPROVED':
      case 'ALLOCATED':
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
    this.cdr.markForCheck();
  }

  openRejectDialog(): void {
    this.showRejectDialog = true;
    this.remarksForm.reset();
    this.remarksForm.get('remarks')?.setValidators(Validators.required);
    this.remarksForm.get('remarks')?.updateValueAndValidity();
    this.cdr.markForCheck();
  }

  closeDialogs(): void {
    this.showApproveDialog = false;
    this.showRejectDialog = false;
    this.remarksForm.reset();
    this.cdr.markForCheck();
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
    if (statusLower.includes('allocated')) return 'home';
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
