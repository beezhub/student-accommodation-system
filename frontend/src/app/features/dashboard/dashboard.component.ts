import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core';
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
import { ModernStatusBadgeComponent } from '../../shared/modern-status-badge/modern-status-badge.component';

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

  currentUser: any;
  selectedDocument: any = null;
  currentApplication: Application | null = null;
  isLoadingCurrentApplication = false;

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
