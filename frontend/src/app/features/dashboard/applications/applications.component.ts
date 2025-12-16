import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

// Material imports
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';

// Shared Components
import { ModernStatusBadgeComponent } from '../../../shared/modern-status-badge/modern-status-badge.component';

@Component({
  selector: 'app-applications',
  templateUrl: './applications.component.html',
  styleUrls: ['./applications.component.css'],
  standalone: true,
  imports: [CommonModule, RouterModule, MatToolbarModule, MatButtonModule, MatIconModule, MatCardModule, MatTableModule, MatChipsModule, ModernStatusBadgeComponent]
})
export class ApplicationsComponent implements OnInit {
  currentUser: any;
  applications = [
    {
      id: '1',
      year: '2024',
      status: 'Pending',
      submittedDate: '2024-01-15',
      type: 'Single Room',
      duration: '1 Year'
    },
    {
      id: '2',
      year: '2023',
      status: 'Approved',
      submittedDate: '2023-01-10',
      type: 'Studio Apartment',
      duration: '2 Semesters'
    }
  ];

  // Material table
  displayedColumns: string[] = ['applicationId', 'year', 'status', 'submittedDate', 'type', 'duration', 'actions'];
  dataSource = new MatTableDataSource<any>(this.applications);

  constructor(private authService: AuthService) {}

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    this.dataSource.data = this.applications;
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

  cancelApplication(applicationId: string) {
    // simple client-side cancellation; replace with API call as needed
    this.applications = this.applications.filter(a => a.id !== applicationId);
    this.dataSource.data = this.applications;
  }
}