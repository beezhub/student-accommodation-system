import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core';
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

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatBadgeModule } from '@angular/material/badge';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ModernStatusBadgeComponent } from '../../shared/modern-status-badge/modern-status-badge.component';
import { AcceptOfferDialogComponent } from './accept-offer-dialog/accept-offer-dialog.component';
import { DeclineOfferDialogComponent, DeclineOfferDialogData } from './decline-offer-dialog/decline-offer-dialog.component';
import { DeclineOfferRequest } from '../../core/services/room-allocation.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterModule,
    DocumentViewerComponent,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatBadgeModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatGridListModule,
    MatDividerModule,
    MatDialogModule,
    ModernStatusBadgeComponent,
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

  currentUser: any;
  selectedDocument: any = null;
  currentApplication: Application | null = null;

  isLoadingCurrentApplication = false;
  processing = false;
  readonly AllocationStatus = AllocationStatus;

  applicationHistory: readonly Application[] = [];
  isLoadingApplications = false;

  uploadedDocuments: any[] = [
    { name: 'Student ID', date: '2024-01-15', status: 'Rejected' },
    { name: 'Proof of Enrollment', date: '2024-01-15', status: 'Pending' },
    { name: 'Passport', date: '2024-01-15', status: 'Verified' },
  ];

  roomAllocation: RoomAllocationResponse | null = null;
  isLoadingRoom = false;

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/login']);
      return;
    }

    this.loadDocuments();
    this.loadRoomAllocation();
    this.loadApplicationHistory();
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
          error: (err) => {
            // Student might not have a room allocation yet
            console.log('No active room allocation found:', err);
            this.roomAllocation = null;
            this.isLoadingRoom = false;
            this.cdr.markForCheck();
          },
        });
      },
      error: (err) => {
        console.error('Failed to fetch student profile:', err);
        this.isLoadingRoom = false;
      },
    });

  }

  acceptOffer(): void {
    if (!this.roomAllocation) return;

    const dialogRef = this.dialog.open(AcceptOfferDialogComponent, {
      width: '400px',
      data: { allocation: this.roomAllocation }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.processing = true;
        this.roomAllocationService.acceptOffer(this.roomAllocation!.id, result.toISOString()).subscribe({
          next: (updatedAlloc) => {
            this.roomAllocation = updatedAlloc;
            this.processing = false;
            this.cdr.markForCheck();
          },
          error: (err) => {
            console.error('Failed to accept offer', err);
            this.processing = false;
            this.cdr.markForCheck();
          }
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
          buildingName: this.roomAllocation!.buildingName
        };

        const dialogRef = this.dialog.open(DeclineOfferDialogComponent, {
          width: '500px',
          data: dialogData,
          disableClose: false
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
                  alert('Your application has been closed due to reaching the maximum number of declines.');
                  this.loadApplicationHistory();
                } else {
                  alert('Failed to decline offer. Please try again or contact support.');
                }

                this.cdr.markForCheck();
              }
            });
          }
        });
      },
      error: (err: any) => {
        console.error('Failed to get decline count', err);
        alert('Unable to process decline request. Please try again.');
      }
    });
  }

  private getPreviousDeclineCount(applicationId: number): Observable<number> {
    // Search for all DECLINED allocations for this application
    return new Observable(observer => {
      this.roomAllocationService.searchAllocations({
        status: 'DECLINED',
        page: 0,
        size: 100 // Should be enough to capture all declines
      }).subscribe({
        next: (response) => {
          // Filter by application ID and count
          const declineCount = response.content.filter(
            alloc => alloc.applicationId === applicationId
          ).length;
          observer.next(declineCount);
          observer.complete();
        },
        error: (err) => {
          observer.error(err);
        }
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
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to fetch application history:', err);
        this.applicationHistory = [];
        this.currentApplication = null;
        this.isLoadingApplications = false;
        this.isLoadingCurrentApplication = false;
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

  getStatusClass(status: string): string {
    switch (status.toLowerCase()) {
      case 'approved':
        return 'status-approved';
      case 'pending':
        return 'status-pending';
      case 'rejected':
        return 'status-rejected';
      case 'verified':
        return 'status-verified';
      default:
        return '';
    }
  }
}
