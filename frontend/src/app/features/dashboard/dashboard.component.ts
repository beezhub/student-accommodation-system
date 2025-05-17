import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  standalone: true,
  imports: [CommonModule, RouterModule]
})
export class DashboardComponent implements OnInit {
  currentUser: any;
  currentApplication = {
    status: 'Pending',
    year: '2024',
    roomInfo: null
  };

  previousApplications = [
    {
      year: '2023',
      status: 'Approved',
      room: 'Room 302, Block A',
      id: '1'
    }
  ];

  uploadedDocuments = [
    { name: 'Student ID', date: '2024-01-15' },
    { name: 'Proof of Enrollment', date: '2024-01-15' },
    { name: 'Passport', date: '2024-01-15' }
  ];

  constructor(
      private router: Router,
      private authService: AuthService
  ) {}

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/login']);
    }
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  getStatusClass(status: string): string {
    switch (status.toLowerCase()) {
      case 'approved':
        return 'bg-success';
      case 'pending':
        return 'bg-warning';
      case 'waitlisted':
        return 'bg-info';
      default:
        return 'bg-secondary';
    }
  }
}