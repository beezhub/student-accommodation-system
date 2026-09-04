import {
  Component,
  OnInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { DocumentViewerComponent } from './documents/document-viewer/document-viewer.component';
import { DocumentService } from '../../core/services/document.service';
import { StudentService } from '../../core/services/student.service';
import {
  RoomAllocationService,
  RoomAllocationResponse,
} from '../../core/services/room-allocation.service';
import { ApplicationService } from '../../core/services/application.service';
import { Application } from '../../core/models/application.model';
import { AllocationStatus } from '../../core/models/status.model';
import { SystemSettingsService } from '../../core/services/system-settings.service';
import { ErrorHandlingService } from '../../core/services/error-handling.service';
import { WaitlistService, WaitlistPosition } from '../../core/services/waitlist.service';

// Material imports
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ModernStatusBadgeComponent } from '../../shared/modern-status-badge/modern-status-badge.component';
import { AcceptOfferDialogComponent } from './accept-offer-dialog/accept-offer-dialog.component';
import {
  DeclineOfferDialogComponent,
  DeclineOfferDialogData,
} from './decline-offer-dialog/decline-offer-dialog.component';
import { DeclineOfferRequest } from '../../core/services/room-allocation.service';
import { OfferExpiryComponent } from '../../shared/components/offer-expiry/offer-expiry.component';
import { SkeletonLoaderComponent } from '../../shared/components/skeleton-loader/skeleton-loader.component';
import { WaitlistStatusComponent } from '../../shared/components/waitlist-status/waitlist-status.component';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterModule,
    DocumentViewerComponent,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    ModernStatusBadgeComponent,
    OfferExpiryComponent,
    SkeletonLoaderComponent,
    WaitlistStatusComponent,
  ],
})
export class DashboardComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly documentService = inject(DocumentService);
  private readonly studentService = inject(StudentService);
  private readonly roomAllocationService = inject(RoomAllocationService);
  private readonly applicationService = inject(ApplicationService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly dialog = inject(MatDialog);
  private readonly systemSettingsService = inject(SystemSettingsService);
  private readonly errorService = inject(ErrorHandlingService);
  private readonly waitlistService = inject(WaitlistService);

  currentUser: any;
  selectedDocument: any = null;
  currentApplication: Application | null = null;

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  isLoadingCurrentApplication = false;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  processing = false;
  readonly AllocationStatus = AllocationStatus;

  applicationHistory: readonly Application[] = [];
  isLoadingApplications = false;

  uploadedDocuments: any[] = [
    { name: 'Student ID', date: '2024-01-15', status: 'Rejected' },
    { name: 'Proof of Enrollment', date: '2024-01-15', status: 'Pending' },
    { name: 'Passport', date: '2024-01-15', status: 'Verified' },
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  ];

  roomAllocation: RoomAllocationResponse | null = null;
  isLoadingRoom = false;

  /** Response time message for SUBMITTED / UNDER_REVIEW status */
  responseTimeMessage: string | null = null;

  /** Null when the student has no active waiting-list entry (card is hidden). */
  waitlistPosition: WaitlistPosition | null = null;

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/login']);
      return;
    }

    this.loadDocuments();
    this.loadRoomAllocation();
    this.loadApplicationHistory();
    this.loadResponseTimeSetting();
  }

  loadDocuments(): void {
    this.documentService.getUploadedDocuments().subscribe({
      next: (docs) => {
        this.uploadedDocuments = docs.map((doc) => ({
          name: doc.documentName,
          date: doc.uploadDate,
          status: doc.documentStatus,
        }));
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to fetch documents:', err);
        this.errorService.showWarning('Could not load your documents. Please refresh the page.');
      },
    });
  }

  loadRoomAllocation(): void {
    this.isLoadingRoom = true;
    // First get the student profile to get the student ID
    this.studentService.getStudentProfile().subscribe({
      next: (student) => {
        // Then fetch the active room allocation for this student
        this.roomAllocationService.getActiveAllocationByStudentId(student.id).subscribe({
          next: (allocation) => {
            this.roomAllocation = allocation;
            this.isLoadingRoom = false;
            this.cdr.markForCheck();
          },
          error: () => {
            // Student might not have a room allocation yet — expected state
            this.roomAllocation = null;
            this.isLoadingRoom = false;
            this.cdr.markForCheck();
          },
        });
      },
      error: (err) => {
        console.error('Failed to fetch student profile:', err);
        this.errorService.showWarning(
          'Could not load room allocation info. Please refresh the page.'
        );
        this.isLoadingRoom = false;
        this.cdr.markForCheck();
      },
    });
  }

  acceptOffer(): void {
    if (!this.roomAllocation) return;

    const dialogRef = this.dialog.open(AcceptOfferDialogComponent, {
      width: '400px',
      data: { allocation: this.roomAllocation },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.processing = true;
        this.roomAllocationService
          .acceptOffer(this.roomAllocation!.id, result.toISOString())
          .subscribe({
            next: (updatedAlloc) => {
              this.roomAllocation = updatedAlloc;
              this.processing = false;
              this.cdr.markForCheck();
            },
            error: (err) => {
              console.error('Failed to accept offer', err);
              this.errorService.showErrorSnackbar(this.errorService.parseError(err));
              this.processing = false;
              this.cdr.markForCheck();
            },
          });
      }
    });
  }

  declineOffer(): void {
    if (!this.roomAllocation || !this.currentApplication) return;

    // Count previous declines for this application
    this.getPreviousDeclineCount(this.currentApplication.id).subscribe({
      next: (declineCount: number) => {
        const dialogData: DeclineOfferDialogData = {
          previousDeclineCount: declineCount,
          roomNumber: this.roomAllocation!.roomNumber,
          buildingName: this.roomAllocation!.buildingName,
        };

        const dialogRef = this.dialog.open(DeclineOfferDialogComponent, {
          width: '500px',
          data: dialogData,
          disableClose: false,
        });

        dialogRef.afterClosed().subscribe((request: DeclineOfferRequest | undefined) => {
          if (request && this.roomAllocation) {
            this.processing = true;
            this.roomAllocationService.declineOffer(this.roomAllocation.id, request).subscribe({
              next: (updatedAlloc) => {
                this.roomAllocation = updatedAlloc;
                this.processing = false;
                // Reload application to get updated status
                this.loadApplicationHistory();
                this.cdr.markForCheck();
              },
              error: (err) => {
                console.error('Failed to decline offer', err);
                this.processing = false;

                // Handle max decline error (403)
                if (err.status === 403) {
                  this.errorService.showErrorSnackbar(
                    'Your application has been closed due to reaching the maximum number of declines.'
                  );
                  this.loadApplicationHistory();
                } else {
                  this.errorService.showErrorSnackbar(
                    'Failed to decline offer. Please try again or contact support.'
                  );
                }

                this.cdr.markForCheck();
              },
            });
          }
        });
      },
      error: (err: any) => {
        console.error('Failed to get decline count', err);
        this.errorService.showErrorSnackbar('Unable to process decline request. Please try again.');
      },
    });
  }

  private getPreviousDeclineCount(applicationId: number): Observable<number> {
    // Search for all DECLINED allocations for this application
    return new Observable((observer) => {
      this.roomAllocationService
        .searchAllocations({
          status: 'DECLINED',
          page: 0,
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          size: 100, // Should be enough to capture all declines
        })
        .subscribe({
          next: (response) => {
            // Filter by application ID and count
            const declineCount = response.content.filter(
              (alloc) => alloc.applicationId === applicationId
            ).length;
            observer.next(declineCount);
            observer.complete();
          },
          error: (err) => {
            observer.error(err);
          },
        });
    });
  }

  loadApplicationHistory(): void {
    this.isLoadingApplications = true;
    this.isLoadingCurrentApplication = true;
    this.applicationService.getMyApplications().subscribe({
      next: (applications) => {
        // Sort by createdAt descending (newest first)
        // Fallback to applicationDate if createdAt is not available
        this.applicationHistory = applications.sort((a, b) => {
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
          return dateB - dateA; // Descending order
        });

        // Set current application
        this.currentApplication = this.getCurrentApplication(applications);

        this.isLoadingApplications = false;
        this.isLoadingCurrentApplication = false;
        this.loadWaitlistPosition();
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to fetch application history:', err);
        this.errorService.showWarning('Could not load your applications. Please refresh the page.');
        this.applicationHistory = [];
        this.currentApplication = null;
        this.waitlistPosition = null;
        this.isLoadingApplications = false;
        this.isLoadingCurrentApplication = false;
        this.cdr.markForCheck();
      },
    });
  }

  /**
   * Fetches waitlist position only when the current application is
   * AWAITING_ALLOCATION (the only status under which a waiting-list entry can
   * exist). A 404 from the backend resolves to `null` and simply hides the
   * card — see WaitlistService.getMyPosition().
   */
  loadWaitlistPosition(): void {
    if (this.currentApplication?.status?.toUpperCase() !== 'AWAITING_ALLOCATION') {
      this.waitlistPosition = null;
      return;
    }

    this.waitlistService.getMyPosition().subscribe((position) => {
      this.waitlistPosition = position;
      this.cdr.markForCheck();
    });
  }

  loadResponseTimeSetting(): void {
    this.systemSettingsService.getPublicSetting('application_review_response_days').subscribe({
      next: (value) => {
        if (value) {
          this.responseTimeMessage = `Your application is now being reviewed. You\u2019ll receive an update within ${value} business days.`;
        } else {
          this.responseTimeMessage =
            'Your application is being reviewed. We\u2019ll notify you as soon as there\u2019s an update.';
        }
        this.cdr.markForCheck();
      },
    });
  }

  getCurrentApplication(applications: Application[]): Application | null {
    if (!applications || applications.length === 0) return null;

    // Sort by date descending (newest first)
    const sorted = [...applications].sort((a, b) => {
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

    // Priority: PENDING > APPROVED > most recent
    const pending = sorted.find((app) => app.status.toUpperCase() === 'PENDING');
    if (pending) return pending;

    const approved = sorted.find((app) => app.status.toUpperCase() === 'APPROVED');
    if (approved) return approved;

    // Return most recent
    return sorted[0];
  }

  get timeOfDay(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 17) return 'Good afternoon';
    return 'Good evening';
  }

  get showResponseTimeMessage(): boolean {
    const status = this.currentApplication?.status?.toUpperCase();
    return (status === 'SUBMITTED' || status === 'UNDER_REVIEW') && !!this.responseTimeMessage;
  }

  get journeySteps(): { label: string; done: boolean; active: boolean }[] {
    const status = this.currentApplication?.status?.toUpperCase() ?? '';
    const steps = [
      { label: 'Submitted', key: 'SUBMITTED' },
      { label: 'Reviewing', key: 'UNDER_REVIEW' },
      { label: 'Approved', key: 'APPROVED' },
      { label: 'Awaiting', key: 'AWAITING_ALLOCATION' },
      { label: 'Offered', key: 'OFFERED' },
    ];
    const order = [
      'PENDING',
      'SUBMITTED',
      'UNDER_REVIEW',
      'APPROVED',
      'AWAITING_ALLOCATION',
      'OFFERED',
      'ACCEPTED',
      'CHECKED_IN',
    ];
    const currentIdx = order.indexOf(status);
    return steps.map((step, i) => ({
      label: step.label,
      done: order.indexOf(step.key) < currentIdx,
      active: step.key === status || (status === 'PENDING' && i === 0),
    }));
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  viewDocument(document: any) {
    this.selectedDocument = document;
  }

  closeViewer() {
    this.selectedDocument = null;
  }
}
