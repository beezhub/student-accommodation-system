import { OpenWindowsBannerComponent } from './open-windows-banner/open-windows-banner.component';
import {
  Component,
  OnInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
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
import { User } from '../../core/models/user.model';
import { DocumentData, DocumentPreview } from '../../core/models/document.model';

/** One row of the Documents card. */
interface UploadedDocumentRow {
  name: string;
  date: string;
  status: string;
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    OpenWindowsBannerComponent,
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
  /**
   * The application journey, in order, keyed to statuses the backend actually has.
   *
   * <p>The old stepper keyed steps to SUBMITTED and OFFERED, neither of which is a member of the
   * backend enum, and its order array omitted ALLOCATED and CHANGES_REQUESTED. indexOf returned
   * -1 for those, so the moment a student accepted an offer the stepper reset to blank dots
   * (GH-151).
   *
   * <p>There is no Offered step: making an offer creates an allocation and never changes the
   * application status, so an application is never in that state. The Room card already shows an
   * outstanding offer with its countdown and the accept and decline actions.
   *
   * <p>This list is also the ranking used by getCurrentApplication, so the order is stated once.
   */
  private static readonly JOURNEY_STAGES: readonly {
    label: string;
    statuses: readonly string[];
  }[] = [
    { label: 'Submitted', statuses: ['PENDING'] },
    { label: 'Reviewing', statuses: ['UNDER_REVIEW', 'CHANGES_REQUESTED'] },
    { label: 'Approved', statuses: ['APPROVED'] },
    { label: 'Awaiting', statuses: ['AWAITING_ALLOCATION'] },
    { label: 'Allocated', statuses: ['ALLOCATED'] },
    { label: 'Checked In', statuses: ['CHECKED_IN'] },
  ];

  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly documentService = inject(DocumentService);
  private readonly studentService = inject(StudentService);
  private readonly roomAllocationService = inject(RoomAllocationService);
  private readonly applicationService = inject(ApplicationService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly dialog = inject(MatDialog);
  private readonly errorService = inject(ErrorHandlingService);
  private readonly waitlistService = inject(WaitlistService);

  currentUser: User | null = null;
  selectedDocument: DocumentPreview | null = null;
  currentApplication: Application | null = null;

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  isLoadingCurrentApplication = false;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  processing = false;
  readonly AllocationStatus = AllocationStatus;

  applicationHistory: readonly Application[] = [];
  isLoadingApplications = false;

  /**
   * Empty until the API answers. These were three hard-coded rows that rendered in the
   * Documents card and the header count before any request completed, and the error branch never
   * cleared them, so a failed load left fabricated documents on screen permanently (GH-151).
   */
  uploadedDocuments: UploadedDocumentRow[] = [];

  roomAllocation: RoomAllocationResponse | null = null;
  isLoadingRoom = false;

  /** Response time message for PENDING / UNDER_REVIEW status. */
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
        // Show the empty state rather than whatever was on screen before.
        this.uploadedDocuments = [];
        this.cdr.markForCheck();
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
          .acceptOffer(this.roomAllocation!.id, this.toLocalIsoDateTime(result))
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

    // The backend sends the decline count on the allocation itself. It used to be derived from
    // the allocation search endpoint, which students may not call: the 403 aborted the flow and
    // the dialog never opened.
    const dialogData: DeclineOfferDialogData = {
      previousDeclineCount: this.roomAllocation.previousDeclineCount ?? 0,
      roomNumber: this.roomAllocation.roomNumber,
      buildingName: this.roomAllocation.buildingName,
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
  }

  loadApplicationHistory(): void {
    this.isLoadingApplications = true;
    this.isLoadingCurrentApplication = true;
    this.applicationService.getMyApplications().subscribe({
      next: (applications) => {
        // Sort by createdAt descending (newest first)
        // Fallback to applicationDate if createdAt is not available
        this.applicationHistory = applications.sort((a, b) => this.compareByDateDesc(a, b));

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

  /**
   * GH-152: this used to fetch `application_review_response_days` and promise students an update
   * "within N business days". Nothing in the backend read that setting — no job, no escalation, no
   * reminder — so it was a service level the system had no mechanism to keep. The setting is gone
   * and so is the promise; what remains is what is actually true.
   */
  loadResponseTimeSetting(): void {
    this.responseTimeMessage =
      'Your application is being reviewed. We\u2019ll notify you as soon as there\u2019s an update.';
    this.cdr.markForCheck();
  }

  /** Position in {@link JOURNEY_STAGES}, or -1 for a status that is off the happy path. */
  private stageIndexOf(status: string | undefined): number {
    const s = status?.toUpperCase() ?? '';
    return DashboardComponent.JOURNEY_STAGES.findIndex((stage) => stage.statuses.includes(s));
  }

  private compareByDateDesc(a: Application, b: Application): number {
    const at = a.createdAt
      ? new Date(a.createdAt).getTime()
      : a.applicationDate
      ? new Date(a.applicationDate).getTime()
      : 0;
    const bt = b.createdAt
      ? new Date(b.createdAt).getTime()
      : b.applicationDate
      ? new Date(b.applicationDate).getTime()
      : 0;
    return bt - at;
  }

  /**
   * The furthest-progressed application wins, most recent breaking a tie.
   *
   * <p>Students sit at the platform tenant, so the tenant filter does not apply and this list
   * spans every company the student has applied to. The old rule put PENDING first, so a stale
   * application with one company outranked a live allocation with another and the Application
   * card contradicted the Room card beside it (GH-151).
   *
   * <p>Ranking comes from JOURNEY_STAGES rather than a second list. Note this deliberately
   * includes CHECKED_IN, unlike the backend duplicate-application check: that omits CHECKED_IN so
   * a checked-in student can apply for the next year, which is a write-side rule and has no
   * bearing on which application to show.
   */
  getCurrentApplication(applications: Application[]): Application | null {
    if (!applications || applications.length === 0) return null;

    const sorted = [...applications].sort((a, b) => this.compareByDateDesc(a, b));
    const onTrack = sorted.filter((app) => this.stageIndexOf(app.status) >= 0);
    if (onTrack.length === 0) {
      return sorted[0];
    }
    return onTrack.sort(
      (a, b) =>
        this.stageIndexOf(b.status) - this.stageIndexOf(a.status) || this.compareByDateDesc(a, b)
    )[0];
  }

  /**
   * The picker builds a Date at local midnight, and toISOString converts to UTC, so east of
   * Greenwich the backend received the previous day and rejected the student's own "today"
   * (GH-151). The backend field is a LocalDateTime, so it wants a wall-clock string with no
   * offset.
   *
   * <p>End of day rather than noon: the backend accepts (now - 1 hour) .. (now + 7 days), both
   * server-local. A noon stamp on today is rejected every afternoon once now - 1 hour passes
   * 12:00. 23:59 on the first selectable day is always after the lower bound, and 23:59 on the
   * last one is always before the upper bound.
   *
   * <p>Residual: the client compares its own wall clock against the server's. A server in a
   * different zone still has a late-evening edge, which only an integration test could settle.
   */
  private toLocalIsoDateTime(d: Date): string {
    const p = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T23:59:00`;
  }

  get timeOfDay(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 17) return 'Good afternoon';
    return 'Good evening';
  }

  get showResponseTimeMessage(): boolean {
    const status = this.currentApplication?.status?.toUpperCase();
    // SUBMITTED is not a backend status, so this hint never appeared on a just-submitted
    // application, which is the case it was built for (GH-151).
    return (status === 'PENDING' || status === 'UNDER_REVIEW') && !!this.responseTimeMessage;
  }

  get journeySteps(): { label: string; done: boolean; active: boolean }[] {
    const idx = this.stageIndexOf(this.currentApplication?.status);
    return DashboardComponent.JOURNEY_STAGES.map((stage, i) => ({
      label: stage.label,
      done: idx >= 0 && i < idx,
      active: i === idx,
    }));
  }

  /** The track describes the happy path; REJECTED, EXPIRED and CANCELLED are off it. */
  get showJourney(): boolean {
    return this.stageIndexOf(this.currentApplication?.status) >= 0;
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  viewDocument(document: DocumentData) {
    // The DTO field is documentName; the viewer reads `name`. Passing the DTO straight through
    // left the viewer with no name at all (GH-158).
    this.selectedDocument = {
      id: document.id,
      name: document.documentName,
      type: document.documentTypeName,
      uploadDate: document.uploadDate,
      status: document.documentStatus,
    };
  }

  closeViewer() {
    this.selectedDocument = null;
  }
}
