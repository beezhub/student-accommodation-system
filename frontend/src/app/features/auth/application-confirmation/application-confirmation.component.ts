import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ApplicationService } from '../../../core/services/application.service';
import { Application } from '../../../core/models/application.model';

@Component({
  selector: 'app-application-confirmation',
  templateUrl: './application-confirmation.component.html',
  styleUrls: ['./application-confirmation.component.css'],
  standalone: true,
  imports: [CommonModule, RouterModule]
})
export class ApplicationConfirmationComponent implements OnInit {
  currentUser: any;
  currentDate: Date = new Date();
  application: Application | null = null;

  constructor(
      private router: Router,
      private authService: AuthService,
      private applicationService: ApplicationService
  ) {}

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/login']);
    }
    this.applicationService.getApplicationByStatus('Pending').subscribe({
      next: (applications) => {
        if (applications.length > 0) {
          this.application = applications[0];
        }
      },
      error: (err) => {
        console.error('Failed to load applications', err);
        this.router.navigate(['/dashboard']);
      }
    });
  }

  goToDashboard() {
    this.router.navigate(['/dashboard']);
  }
  
}