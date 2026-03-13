import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ApplicationService } from '../../../core/services/application.service';
import { Application } from '../../../core/models/application.model';
import { ModernStatusBadgeComponent } from '../../../shared/modern-status-badge/modern-status-badge.component';

// Material imports
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-application-confirmation',
  templateUrl: './application-confirmation.component.html',
  styleUrls: ['./application-confirmation.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    ModernStatusBadgeComponent,
  ],
})
export class ApplicationConfirmationComponent implements OnInit {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);
  private applicationService = inject(ApplicationService);

  currentUser: any;
  application: Application | null = null;
  applicationId: number | null = null;

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/login']);
    }
    const idParam = this.route.snapshot.paramMap.get('applicationId');
    this.applicationId = idParam !== null && !isNaN(Number(idParam)) ? Number(idParam) : null;

    if (this.applicationId && this.applicationId > 0) {
      this.applicationService.getApplicationById(this.applicationId).subscribe({
        next: (application: Application) => {
          this.application = application;
        },
        error: (err) => {
          console.error('Failed to load application', err);
          this.router.navigate(['/dashboard']);
        },
      });
    } else {
      console.error('No valid application ID found in route');
      this.router.navigate(['/dashboard']);
    }
  }

  goToDashboard() {
    this.router.navigate(['/dashboard']);
  }
}
