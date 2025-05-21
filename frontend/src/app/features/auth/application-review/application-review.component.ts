import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-application-review',
  templateUrl: './application-review.component.html',
  styleUrls: ['./application-review.component.css'],
  standalone: true,
  imports: [CommonModule]
})
export class ApplicationReviewComponent implements OnInit {
  isSubmitting = false;
  currentUser: any;
  applicationData: any;

  constructor(
      private router: Router,
      private authService: AuthService
  ) {}

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/login']);
      return;
    }

    // In a real application, you would fetch the complete application data here
    this.applicationData = {
      personalInfo: {
        name: `${this.currentUser.firstName} ${this.currentUser.lastName}`,
        email: this.currentUser.email
      }
    };
  }

  onSubmit() {
    this.isSubmitting = true;
    this.isSubmitting = false;
    this.router.navigate(['/application-confirmation']);
    // this.authService.submitApplication(this.applicationData).subscribe({
    //   next: () => {
    //     this.isSubmitting = false;
    //     this.router.navigate(['/application-confirmation']);
    //   },
    //   error: (error) => {
    //     this.isSubmitting = false;
    //     console.error('Error submitting application:', error);
    //   }
    // });
  }
}