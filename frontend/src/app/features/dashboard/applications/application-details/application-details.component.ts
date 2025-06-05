import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-application-details',
  templateUrl: './application-details.component.html',
  styleUrls: ['./application-details.component.css'],
  standalone: true,
  imports: [CommonModule, RouterModule]
})
export class ApplicationDetailsComponent implements OnInit {
  currentUser: any;
  applicationId: string = '';
  application = {
    id: '',
    year: '',
    status: '',
    submittedDate: '',
    type: '',
    duration: '',
    personalInfo: {
      name: '',
      email: '',
      phone: '',
      studentId: ''
    },
    accommodationDetails: {
      type: '',
      moveInDate: '',
      duration: '',
      specialRequirements: ''
    },
    documents: [
      { name: 'Student ID', status: 'Verified', date: '2024-01-15' },
      { name: 'Proof of Enrollment', status: 'Verified', date: '2024-01-15' },
      { name: 'Passport/ID', status: 'Verified', date: '2024-01-15' }
    ],
    timeline: [
      { date: '2024-01-15', status: 'Application Submitted', description: 'Your application has been received' },
      { date: '2024-01-16', status: 'Documents Verified', description: 'All required documents have been verified' },
      { date: '2024-01-17', status: 'Under Review', description: 'Application is being reviewed by the housing team' }
    ]
  };

  constructor(
      private route: ActivatedRoute,
      private router: Router,
      private authService: AuthService
  ) {}

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    this.route.params.subscribe(params => {
      this.applicationId = params['id'];
      // Here you would typically fetch the application details using the ID
      this.application.id = this.applicationId;
    });
  }

  getStatusClass(status: string): string {
    switch (status.toLowerCase()) {
      case 'verified':
      case 'approved':
        return 'bg-success';
      case 'pending':
      case 'under review':
        return 'bg-warning';
      case 'rejected':
        return 'bg-danger';
      default:
        return 'bg-secondary';
    }
  }

  goBack() {
    this.router.navigate(['/applications']);
  }
}