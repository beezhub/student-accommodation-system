import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminService, DashboardStats, OccupancyStats } from '../../../core/services/admin.service';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatGridListModule,
    MatProgressBarModule,
    MatTableModule,
    MatTooltipModule,
  ],
})
export class AdminDashboardComponent implements OnInit {
  today = new Date();

  stats: DashboardStats = {
    totalApplications: 0,
    pendingApplications: 0,
    approvedApplications: 0,
    rejectedApplications: 0,
    inReviewApplications: 0,
    allocatedApplications: 0,
    totalDocuments: 0,
    pendingDocuments: 0,
    approvedDocuments: 0,
    rejectedDocuments: 0,
    totalStudents: 0,
    totalAdmins: 0,
    totalRooms: 0,
    availableRooms: 0,
    occupiedRooms: 0,
    fullRooms: 0,
    maintenanceRooms: 0,
    totalBuildings: 0,
    totalCapacity: 0,
    totalOccupancy: 0,
    overallOccupancyRate: 0,
  };
  
  occupancyStats: OccupancyStats | null = null;
  isLoading = false;
  isLoadingOccupancy = false;

  // Application stats for donut chart
  applicationChartSegments: { label: string; value: number; color: string; percent: number }[] = [];
  documentChartSegments: { label: string; value: number; color: string; percent: number }[] = [];
  roomChartSegments: { label: string; value: number; color: string; percent: number }[] = [];

  buildingOccupancyColumns = ['buildingName', 'roomCount', 'capacity', 'occupancy', 'rate'];

  quickLinks = [
    {
      title: 'Application Review',
      description: 'Review and manage student applications',
      icon: 'assignment',
      route: '/admin/applications',
      class: 'applications',
    },
    {
      title: 'Document Verification',
      description: 'Verify student documents',
      icon: 'verified_user',
      route: '/admin/documents',
      class: 'documents',
    },
    {
      title: 'Room Allocations',
      description: 'Manage room allocations',
      icon: 'meeting_room',
      route: '/admin/room-allocations',
      class: 'allocations',
    },
    {
      title: 'Building Management',
      description: 'Manage buildings and institutions',
      icon: 'apartment',
      route: '/admin/buildings',
      class: 'buildings',
    },
    {
      title: 'Waiting List',
      description: 'View student waiting list',
      icon: 'hourglass_empty',
      route: '/admin/waiting-list',
      class: 'waiting-list',
    },
    {
      title: 'System Settings',
      description: 'Configure system-wide settings',
      icon: 'settings',
      route: '/admin/settings',
      class: 'settings',
    },
  ];

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadStats();
    this.loadOccupancyStats();
  }

  loadStats(): void {
    this.isLoading = true;
    this.adminService.getDashboardStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.updateChartData();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load stats:', error);
        this.isLoading = false;
      },
    });
  }

  loadOccupancyStats(): void {
    this.isLoadingOccupancy = true;
    this.adminService.getOccupancyStats().subscribe({
      next: (stats) => {
        this.occupancyStats = stats;
        this.isLoadingOccupancy = false;
      },
      error: (error) => {
        console.error('Failed to load occupancy stats:', error);
        this.isLoadingOccupancy = false;
      },
    });
  }

  updateChartData(): void {
    // Application chart data
    const appTotal = this.stats.totalApplications || 1;
    this.applicationChartSegments = [
      { label: 'Pending', value: this.stats.pendingApplications, color: '#FFA726', percent: (this.stats.pendingApplications / appTotal) * 100 },
      { label: 'Approved', value: this.stats.approvedApplications, color: '#66BB6A', percent: (this.stats.approvedApplications / appTotal) * 100 },
      { label: 'Awaiting', value: this.stats.inReviewApplications, color: '#42A5F5', percent: (this.stats.inReviewApplications / appTotal) * 100 },
      { label: 'Allocated', value: this.stats.allocatedApplications, color: '#26A69A', percent: (this.stats.allocatedApplications / appTotal) * 100 },
      { label: 'Rejected', value: this.stats.rejectedApplications, color: '#EF5350', percent: (this.stats.rejectedApplications / appTotal) * 100 },
    ].filter(s => s.value > 0);

    // Document chart data
    const docTotal = this.stats.totalDocuments || 1;
    this.documentChartSegments = [
      { label: 'Pending', value: this.stats.pendingDocuments, color: '#FFA726', percent: (this.stats.pendingDocuments / docTotal) * 100 },
      { label: 'Approved', value: this.stats.approvedDocuments, color: '#66BB6A', percent: (this.stats.approvedDocuments / docTotal) * 100 },
      { label: 'Rejected', value: this.stats.rejectedDocuments, color: '#EF5350', percent: (this.stats.rejectedDocuments / docTotal) * 100 },
    ].filter(s => s.value > 0);

    // Room chart data
    const roomTotal = this.stats.totalRooms || 1;
    this.roomChartSegments = [
      { label: 'Available', value: this.stats.availableRooms, color: '#66BB6A', percent: (this.stats.availableRooms / roomTotal) * 100 },
      { label: 'Occupied', value: this.stats.occupiedRooms, color: '#42A5F5', percent: (this.stats.occupiedRooms / roomTotal) * 100 },
      { label: 'Full', value: this.stats.fullRooms, color: '#EF5350', percent: (this.stats.fullRooms / roomTotal) * 100 },
      { label: 'Maintenance', value: this.stats.maintenanceRooms, color: '#78909C', percent: (this.stats.maintenanceRooms / roomTotal) * 100 },
    ].filter(s => s.value > 0);
  }

  getConicGradient(segments: { label: string; value: number; color: string; percent: number }[]): string {
    if (segments.length === 0) return 'conic-gradient(#e0e0e0 0deg 360deg)';
    
    let gradient = 'conic-gradient(';
    let currentAngle = 0;
    
    segments.forEach((segment, index) => {
      const angle = (segment.percent / 100) * 360;
      gradient += `${segment.color} ${currentAngle}deg ${currentAngle + angle}deg`;
      if (index < segments.length - 1) gradient += ', ';
      currentAngle += angle;
    });
    
    gradient += ')';
    return gradient;
  }

  getOccupancyColor(rate: number): string {
    if (rate >= 90) return 'warn';
    if (rate >= 70) return 'accent';
    return 'primary';
  }

  getOccupancyClass(rate: number): string {
    if (rate >= 90) return 'high-occupancy';
    if (rate >= 70) return 'medium-occupancy';
    return 'low-occupancy';
  }
}
