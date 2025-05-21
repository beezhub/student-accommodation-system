import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-applications',
  templateUrl: './applications.component.html',
  styleUrls: ['./applications.component.css'],
  standalone: true,
  imports: [CommonModule, RouterModule]
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

  constructor(private authService: AuthService) {}

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
  }

  getStatusClass(status: string): string {
    switch (status.toLowerCase()) {
      case 'approved':
        return 'bg-success';
      case 'pending':
        return 'bg-warning';
      case 'rejected':
        return 'bg-danger';
      default:
        return 'bg-secondary';
    }
  }
}