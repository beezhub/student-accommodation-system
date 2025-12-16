import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { DocumentViewerComponent } from './documents/document-viewer/document-viewer.component';
import { HttpClient } from '@angular/common/http';
import { DocumentService } from '../../core/services/document.service';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatBadgeModule } from '@angular/material/badge';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatDividerModule } from '@angular/material/divider';
import { ModernStatusBadgeComponent } from '../../shared/modern-status-badge/modern-status-badge.component';


@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  standalone: true,
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
    MatGridListModule,
    MatDividerModule,
    ModernStatusBadgeComponent,
  ],
})
export class DashboardComponent implements OnInit {
  currentUser: any;
  selectedDocument: any = null;
  currentApplication = {
    status: 'Pending',
    year: '2024',
    roomInfo: null,
  };

  previousApplications = [
    {
      year: '2023',
      status: 'Approved',
      room: 'Room 302, Block A',
      id: '1',
    },
  ];

  uploadedDocuments = [
    { name: 'Student ID', date: '2024-01-15', status: 'Rejected' },
    { name: 'Proof of Enrollment', date: '2024-01-15', status: 'Pending' },
    { name: 'Passport', date: '2024-01-15', status: 'Verified' },
  ];

  constructor(
    private router: Router,
    private authService: AuthService,
    private documentService: DocumentService
  ) { }

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/login']);
      return;
    }

    this.documentService.getUploadedDocuments().subscribe({
      next: (docs) => {
        this.uploadedDocuments = docs.map(doc => ({
          name: doc.documentName,
          date: doc.UploadedDate,
          status: doc.documentStatus
        }));
      },
      error: (err) => {
        console.error('Failed to fetch documents:', err);
      }
    });
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
