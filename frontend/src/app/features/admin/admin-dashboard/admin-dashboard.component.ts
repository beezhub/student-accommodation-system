import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminService, DashboardStats } from '../../../core/services/admin.service';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatGridListModule } from '@angular/material/grid-list';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatGridListModule,
  ],
})
export class AdminDashboardComponent implements OnInit {
  stats: DashboardStats = {
    totalApplications: 0,
    pendingApplications: 0,
    approvedApplications: 0,
    rejectedApplications: 0,
    totalDocuments: 0,
    pendingDocuments: 0,
    approvedDocuments: 0,
    rejectedDocuments: 0,
    totalStudents: 0,
    totalAdmins: 0,
  };
  isLoading = false;

  quickLinks = [
    {
      title: 'Application Review',
      description: 'Review and manage student applications',
      icon: 'assignment',
      route: '/admin/applications',
      color: 'primary',
    },
    {
      title: 'Document Verification',
      description: 'Verify student documents',
      icon: 'verified_user',
      route: '/admin/documents',
      color: 'accent',
    },
    {
      title: 'User Management',
      description: 'Create and manage admin and landlord users',
      icon: 'people',
      route: '/admin/users',
      color: 'primary',
    },
    {
      title: 'System Settings',
      description: 'Configure system-wide settings',
      icon: 'settings',
      route: '/admin/settings',
      color: 'accent',
      disabled: true,
    },
  ];

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.isLoading = true;

    this.adminService.getDashboardStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load stats:', error);
        this.isLoading = false;
      },
    });
  }
}